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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import rapture.common.exception.ExceptionToString;
import rapture.common.exception.RaptureExceptionFactory;

public class SFTPConnection extends FTPConnection {

    private static final Logger log = Logger.getLogger(SFTPConnection.class);

    private final JSch jsch;
    private ChannelSftp channel;
    private Session session;

    public SFTPConnection(FTPConnectionConfig config) {
        super(config);
        if (config.isUseSFTP()) {
            jsch = new JSch();
        } else {
            log.info("useSFTP is false - using FTP");
            jsch = null;
        }
    }

    public SFTPConnection(String configUri) {
        super(configUri);
        if (getConfig().isUseSFTP()) jsch = new JSch();
        else {
            log.info("useSFTP is false - using FTP");
            jsch = null;
        }
    }

    @Override
    public boolean connectAndLogin() {
        if (isLocal()) {
            log.info("In test mode - not connecting");
            return true;
        }

        if (!config.isUseSFTP()) {
            return super.connectAndLogin();
        } else {
            if (!isConnected()) {
                return FTPService.runWithRetry("Could not login to " + config.getAddress() + " as " + config.getLoginId(), this, false,
                        new FTPAction<Boolean>() {
                            @SuppressWarnings("synthetic-access")
                            @Override
                            public Boolean run(int attemptNum) throws IOException {
                                try {
                                    java.util.Properties properties = new java.util.Properties();
                                    session = jsch.getSession(config.getLoginId(), config.getAddress(), config.getPort());
                                    properties.put("StrictHostKeyChecking", "no");
                                    properties.put("kex",
                                            "diffie-hellman-group1-sha1,diffie-hellman-group14-sha1,diffie-hellman-group-exchange-sha1,diffie-hellman-group-exchange-sha256");
                                    session.setConfig(properties);
                                    if (!StringUtils.isEmpty(config.getPassword())) {
                                        session.setPassword(config.getPassword());
                                    } else {
                                        jsch.addIdentity(config.getAddress(), config.getPrivateKey().getBytes(), null, null);
                                    }
                                    session.connect();
                                    channel = (ChannelSftp) session.openChannel("sftp");
                                    channel.setInputStream(System.in);
                                    channel.setOutputStream(System.out);
                                    channel.connect();
                                    return true;
                                } catch (Exception e) {
                                    if (e.getCause() instanceof UnknownHostException) {
                                        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Unknown host " + config.getAddress());
                                    }
                                    log.error("Unable to establish secure FTP connection " + config.getLoginId() + "@" + config.getAddress() + ":"
                                            + config.getPort());
                                    throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR,
                                            "Unable to establish secure FTP connection to " + config.getAddress(), e);
                                }
                            }
                        });
            }
        }
        return false;
    }

    @Override
    public boolean isConnected() {
        if (!getConfig().isUseSFTP()) return super.isConnected();
        return isLocal() || (session != null && channel != null && session.isConnected() && channel.isConnected());
    }

    @Override
    public void logoffAndDisconnect() {
        if (isLocal()) return;

        if (!config.isUseSFTP()) {
            super.logoffAndDisconnect();
        } else {
            if ((channel != null) && channel.isConnected()) {
                channel.disconnect();
                channel.exit();
            }
            session.disconnect();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        logoffAndDisconnect();
    }

    /**
     * Returns a list of filenames found here. This accepts glob(3) patterns in path.
     *
     * @param path
     * @return
     */
    @Override
    public List<String> listFiles(String path) {
        if (!config.isUseSFTP()) return super.listFiles(path);

        List<String> filenames = new LinkedList<>();
        try {
            @SuppressWarnings("rawtypes")
            Vector vector = channel.ls((path == null) ? "." : path);
            for (Object o : vector) {
                String file = o.toString();
                file = file.substring(file.lastIndexOf(' ') + 1);
                if (!file.startsWith(".")) {
                    filenames.add(file);
                }
            }
        } catch (SftpException e) {
            log.debug(String.format("Error listing files with path [%s] on server [%s]. Error: %s", path, config.getAddress(), ExceptionToString.format(e)));
        }
        return filenames;
    }

    @Override
    public InputStream read(String fileName) throws IOException {

        if (isLocal()) {
            File f = new File(fileName);
            if (f.canRead()) {
                return new FileInputStream(f);
            }
            return new ByteArrayInputStream("".getBytes());
        } else {
            if (!config.isUseSFTP()) return super.read(fileName);

            try {
                return channel.get(fileName);
            } catch (SftpException e) {
                String err = String.format("Failed to download %s: %s", fileName, e.getMessage());
                log.warn(err);
                log.trace(ExceptionToString.format(e));
                throw new IOException(err, e);
            }
        }
    }

    @Override
    public boolean retrieveFile(String fileName, OutputStream stream) throws IOException {
        if (!config.isUseSFTP()) return super.retrieveFile(fileName, stream);
        IOUtils.copy(read(fileName), stream);
        return true;
    }

    @Override
    // fileName must be the name of a file, not a directory
    public boolean sendFile(InputStream stream, String fileName) throws IOException {
        if (!config.isUseSFTP()) return super.sendFile(stream, fileName);
        if (fileName.endsWith("/")) {
            log.error("fileName must be the name of a file, not a directory");
        } else try {
            int lio = fileName.lastIndexOf('/');
            if (lio > 0) channel.cd(fileName.substring(0, lio));
            channel.put(stream, (lio > 0) ? fileName.substring(lio + 1) : fileName);
            return true;
        } catch (SftpException e) {
            throw new IOException("Cannot copy data to " + fileName, e);
        }
        return false;
    }
}
