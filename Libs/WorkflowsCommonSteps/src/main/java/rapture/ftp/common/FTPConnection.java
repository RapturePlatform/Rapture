/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2011-2016 Incapture Technologies LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package rapture.ftp.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.log4j.Logger;

import rapture.common.CallingContext;
import rapture.common.RaptureURI;
import rapture.common.RaptureURIInputStream;
import rapture.common.RaptureURIOutputStream;
import rapture.common.Scheme;
import rapture.common.exception.ExceptionToString;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.ftp.common.FTPRequest.Action;
import rapture.ftp.common.FTPRequest.Status;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

public class FTPConnection implements Connection {

    private static final Logger log = Logger.getLogger(FTPConnection.class);

    private final FTPClient ftpClient;
    FTPConnectionConfig config = null;
    boolean isLoggedIn = false;
    CallingContext context = ContextFactory.getAnonymousUser();

    public FTPConnection(FTPConnectionConfig config) {
        ftpClient = new FTPClient();
        this.config = config;
    }

    public FTPConnection(String configUri) {
        String configDoc = Kernel.getDoc().getDoc(ContextFactory.getKernelUser(), configUri);
        this.config = (configDoc == null) ? new FTPConnectionConfig() : JacksonUtil.objectFromJson(configDoc, FTPConnectionConfig.class);
        ftpClient = new FTPClient();
    }

    public FTPConnection setContext(CallingContext ctxt) {
        context = ctxt;
        return this;
    }

    @Override
    public boolean doActions(List<FTPRequest> requests) {
        boolean rv = true;
        for (FTPRequest request : requests) {
            rv &= doAction(request);
        }
        return rv;
    }

    @Override
    public boolean doAction(FTPRequest request) {
        if (!isConnected()) {
            if (!connectAndLogin()) return false;
        }
        switch (request.getAction()) {
        case READ:
            download(request);
            break;
        case WRITE:
            upload(request);
            break;
        case EXISTS:
            return fileExists(request);
        default:
            throw new UnsupportedOperationException("Don't support " + request.getAction() + " yet");
        }
        return request.getStatus().equals(Status.SUCCESS);
    }

    public Boolean fileExists(final FTPRequest request) {
        try {
            if (this.isLocal() || request.isLocal()) {
                boolean exists = false;
                File f = null;
                String name = request.getRemoteName();
                if (name.startsWith("file://")) {
                    name = name.substring(6);
                    f = new File(name);
                    exists = f.exists();
                } else if (name.startsWith("//")) {
                    RaptureURI uri = new RaptureURI(name, Scheme.DOCUMENT);
                    exists = Kernel.getDoc().docExists(ContextFactory.getKernelUser(), uri.toString());
                } else if (!name.startsWith("/")) {
                    RaptureURI uri = new RaptureURI(name);
                    if (uri.getScheme() == Scheme.DOCUMENT) {
                        exists = Kernel.getDoc().docExists(ContextFactory.getKernelUser(), uri.toString());
                    } else if (uri.getScheme() == Scheme.BLOB) {
                        exists = Kernel.getBlob().blobExists(ContextFactory.getKernelUser(), uri.toString());
                    }
                } else {
                    f = new File(name);
                    exists = f.exists();
                }
                if (!exists && (f != null)) {
                    List<String> matches = new PathMatcher(name).getResults();
                    request.setResult(matches);
                    exists = !matches.isEmpty();
                }
                request.setLocal(true);
                return exists;
            } else {
                return FTPService.runWithRetry("Error checking whether file exists: " + request.getRemoteName(), this, false, new FTPAction<Boolean>() {
                    @Override
                    public Boolean run(int attemptNum) throws IOException {
                        List<String> result = listFiles(request.getRemoteName());
                        request.setStatus((result.size() > 0) ? Status.SUCCESS : Status.ERROR);
                        return (result.size() > 0);
                    }
                });
            }
        } catch (Exception e) {
            log.error(ExceptionToString.format(ExceptionToString.getRootCause(e)));
            return false;
        }
    }

    @Override
    public boolean connectAndLogin() {
        if (isLocal()) {
            log.info("In local mode - not connecting");
            return true;
        }
        return FTPService.runWithRetry("Could not login to " + config.getAddress() + " as " + config.getLoginId(), this, false, new FTPAction<Boolean>() {
            @Override
            public Boolean run(int attemptNum) throws IOException {
                FTPClient ftpClient = getFtpClient();
                log.debug(String.format("Connecting to %s. Attempt %s of %s", config.getAddress(), 1 + attemptNum, config.getRetryCount()));
                try {
                    ftpClient.connect(config.getAddress());
                } catch (UnknownHostException e) {
                    log.info(ExceptionToString.summary(e));
                    throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Unknown host " + config.getAddress());
                }
                int reply = ftpClient.getReplyCode();
                if (!FTPReply.isPositiveCompletion(reply)) {
                    log.debug("Got non-positive reply of " + reply);
                    logoffAndDisconnect();
                    throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Could not connect to: " + config.getAddress());
                }
                log.debug("Logging in user: " + config.getLoginId());
                if (!ftpClient.login(config.getLoginId(), config.getPassword())) {
                    log.info("Could not login to " + config.getAddress() + " as " + config.getLoginId());
                    ftpClient.logout();
                    throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR,
                            "Could not login to " + config.getAddress() + " as " + config.getLoginId());
                }
                isLoggedIn = true;
                log.debug("Entering local passive mode");
                ftpClient.enterLocalPassiveMode();
                log.info("Connected and logged in to: " + config.getAddress());
                ftpClient.setSoTimeout(1000 * config.getTimeout());
                return true;
            }
        });
    }

    @Override
    public void logoffAndDisconnect() {
        try {
            FTPService.runWithRetry("Error closing connection", this, false, new FTPAction<Boolean>() {
                @Override
                public Boolean run(int attemptNum) throws IOException {
                    log.debug(String.format("Disconnecting from %s. Attempt %s of %s", config.getAddress(), attemptNum + 1, config.getRetryCount()));
                    FTPClient ftpClient = getFtpClient();
                    try {
                        if (isLoggedIn) {
                            ftpClient.logout();
                        }
                        return true;
                    } catch (IOException ignore) {
                        return false;
                    } finally {
                        ftpClient.disconnect();
                    }
                }
            });
        } catch (RaptureException e) {
            log.warn("Unable to log off and disconnect, but will not die: " + ExceptionToString.format(e));
        }
    }

    @Override
    public List<String> listFiles(String path) {
        List<String> files = new LinkedList<>();

        try {
            if (path == null) path = ".";
            FTPFile[] ftpfiles = getFtpClient().listFiles(path);
            for (FTPFile f : ftpfiles) {
                files.add(f.getName());
            }
        } catch (IOException e) {
            log.error(String.format("Error listing files with path [%s] on FTP config.getAddress() [%s]. Error: %s", path, config.getAddress(),
                    ExceptionToString.format(e)));
        }
        return files;
    }

    public FTPClient getFtpClient() {
        return ftpClient;
    }

    @Override
    public boolean isConnected() {
        return ftpClient.isConnected();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        logoffAndDisconnect();
    }

    /* SFTP is different */
    public boolean sendFile(InputStream stream, String fileName) throws IOException {
        int slash = fileName.lastIndexOf('/');
        FTPClient client = getFtpClient();
        if (slash > 0) {
            client.changeWorkingDirectory(fileName.substring(0, slash));
        }
        return client.storeFile(fileName.substring(slash + 1), stream);
    }

    /* SFTP is different */
    public boolean retrieveFile(String fileName, OutputStream stream) throws IOException {
        return getFtpClient().retrieveFile(fileName, stream);
    }

    /* SFTP is different */
    public InputStream read(String fileName) throws IOException {
        return getFtpClient().retrieveFileStream(fileName);
    }

    /**
     * Download a single file to the OutputStream or File defined in the FRPRequest.
     * 
     * @param requestId
     * @param remoteFileName
     * @param isLocal
     * @param dryRunDirPath
     * @return
     */
    public FTPRequest download(final FTPRequest request) {
        if (request.getAction() != Action.READ) throw new IllegalArgumentException("Expected a Read Action");
        String errorMessage = String.format("Error downloading %s", request.getRemoteName());
        return FTPService.runWithRetry(errorMessage, this, true, new FTPAction<FTPRequest>() {
            @Override
            public FTPRequest run(int attemptNum) throws IOException {
                OutputStream outStream = request.getDestination();
                if (outStream == null) {
                    RaptureURI uri = null;
                    String localName = request.getLocalName();
                    if (localName.startsWith("file://")) {
                        localName = localName.substring(6);
                    }

                    if (localName.startsWith("//")) {
                        outStream = new RaptureURIOutputStream(new RaptureURI(localName, Scheme.DOCUMENT)).setContext(context);
                    } else if (!localName.startsWith("/")) {
                        outStream = new RaptureURIOutputStream(new RaptureURI(localName)).setContext(context);
                    } else {
                        Path target = Paths.get(localName);
                        if (!target.getParent().toFile().exists()) Files.createDirectories(target.getParent());
                        outStream = new FileOutputStream(target.toFile());
                    }
                }

                boolean isRetrieved;
                if (isLocal() || request.isLocal()) {
                    File file = new File(request.getRemoteName());
                    log.debug("Local copy from " + file.getAbsolutePath());
                    if (IOUtils.copy(new FileInputStream(file), outStream) > 0) outStream.flush();
                } else {
                    isRetrieved = retrieveFile(request.getRemoteName(), outStream);
                    if (isRetrieved) {
                        log.debug("File retrieved");
                        request.setStatus(Status.SUCCESS);
                        outStream.flush();
                    } else {
                        log.warn(String.format("Missing response from %s", request.getRemoteName()));
                        request.setStatus(Status.ERROR);
                    }
                }
                outStream.close();
                return request;
            }
        });
    }

    public FTPRequest upload(final FTPRequest request) {
        if (request.getAction() != Action.WRITE) throw new IllegalArgumentException("Expected a Write Action");
        String errorMessage = String.format("Error uploading %s", request.getRemoteName());
        return FTPService.runWithRetry(errorMessage, this, true, new FTPAction<FTPRequest>() {
            @Override
            public FTPRequest run(int attemptNum) throws IOException {
                InputStream inStream = request.getSource();
                String localName = request.getLocalName();
                if (inStream == null) {
                    if (localName.startsWith("file://")) {
                        localName = localName.substring(6);
                    }
                    if (localName.startsWith("//")) {
                        inStream = new RaptureURIInputStream(new RaptureURI(localName, Scheme.DOCUMENT));
                    } else if (!localName.startsWith("/")) {
                        inStream = new RaptureURIInputStream(new RaptureURI(localName));
                    } else {
                        inStream = new FileInputStream(new File(localName));
                    }
                }

                if (request.isLocal()) {
                    File file = new File(request.getRemoteName());
                    log.debug("Copy to " + file.getAbsolutePath());
                    IOUtils.copy(inStream, new FileOutputStream(file));
                } else {
                    boolean isSent = sendFile(inStream, request.getRemoteName());
                    if (isSent) {
                        log.debug("File retrieved");
                        request.setStatus(Status.SUCCESS);
                    } else {
                        log.warn(String.format("Missing response from %s", request.getRemoteName()));
                        request.setStatus(Status.ERROR);
                    }
                }
                return request;
            }
        });

    }

    public Integer getRetryCount() {
        return config.getRetryCount();
    }

    public Integer getRetryWait() {
        return config.getRetryWait();
    }

    public boolean isLocal() {
        return (StringUtils.isEmpty(config.getAddress()));
    }

    public FTPConnectionConfig getConfig() {
        return config;
    }

    public FTPConnection setConfig(FTPConnectionConfig config) {
        this.config = config;
        return this;
    }
}
