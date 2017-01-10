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
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import rapture.common.exception.ExceptionToString;

public class SFTPConnection extends FTPConnection {

    // too many loggers
    private static final Logger log4jLogger = Logger.getLogger(SFTPConnection.class);

    private final JSch jsch;
    private ChannelSftp channel;
    private Session session;
    private static int logLevel = com.jcraft.jsch.Logger.INFO;

    // Unfortunately JSCH only allows one logger for all instances
    // This means that if there are multiple simultaneous SFTP connections
    // we can't necessarily associate log messages with connections.
    // We can only track messages that were logged while this SFTPConnection was instantiated

    static class JschLogger implements com.jcraft.jsch.Logger {
        static Map<SFTPConnection, List<String>> errors = new ConcurrentHashMap<>();

        @Override
        public boolean isEnabled(int level) {
            return level >= logLevel;
        }

        @Override
        public void log(int level, String message) {
            switch (level) {

            // DEBUG and INFO level are somewhat noisy, but we'll keep the messages for debugging in case something fails
            case DEBUG:
                // logger.debug(message);
                break;
            case INFO:
                // logger.info(message);
                break;
            case WARN:
                log4jLogger.warn(message);
                break;
            case ERROR:
                log4jLogger.error(message);
                break;
            case FATAL:
                log4jLogger.fatal(message);
                break;
            }
            for (List<String> list : errors.values()) {
                list.add(message);
            }
        }

        public static void register(SFTPConnection conn) {
            errors.put(conn, new ArrayList<>());
        }

        public static List<String> getErrors(SFTPConnection conn) {
            return errors.get(conn);
        }

        public static List<String> deregister(SFTPConnection conn) {
            return errors.remove(conn);
        }
    }

    static {
        JSch.setLogger(new JschLogger());
    }

    public JSch setup(FTPConnectionConfig config) {
        JschLogger.register(this);
        if (config.isUseSFTP()) {
            log4jLogger.info("Connecting via SFTP");
            return new JSch();
        } else {
            log4jLogger.info("useSFTP is false - using FTP");
            return null;
        }
    }

    public SFTPConnection(FTPConnectionConfig config) {
        super(config);
        jsch = setup(config);
    }

    public SFTPConnection(String configUri) {
        super(configUri);
        jsch = setup(this.getConfig());
    }

    @Override
    public boolean connectAndLogin(FTPRequest request) {
        if (isLocal()) {
            log4jLogger.info("In test mode - not connecting");
            return true;
        }
        if (!config.isUseSFTP()) return super.connectAndLogin(request);
        if (isConnected()) return true;
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
            if (e.getCause() instanceof ConnectException) {
                request.addError("Unable to connect to " + config.getAddress() + " as " + config.getLoginId());
            }
            for (String err : JschLogger.getErrors(this)) {
                request.addError(err);
            }
            request.addError(ExceptionToString.summary(e));

            log4jLogger.info(JschLogger.getErrors(this));
            log4jLogger.info(ExceptionToString.summary(e));
            return false;
        }
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
        JschLogger.deregister(this);
        logoffAndDisconnect();
        super.finalize();
    }

    /**
     * Returns a list of filenames found here. This accepts glob(3) patterns in path.
     *
     * @param path
     * @return
     */
    @Override
    public List<String> listFiles(String path, FTPRequest request) {
        if (!config.isUseSFTP()) return super.listFiles(path, request);

        List<String> filenames = new LinkedList<>();
        if (channel == null) return filenames;
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
            for (String error : JschLogger.getErrors(this)) {
                request.addError(error);
            }
            log4jLogger.info(JschLogger.getErrors(this));
            String message = String.format("Error listing files with path [%s] on server [%s]. Error: %s", path, config.getAddress(),
                    ExceptionToString.format(e));
            log4jLogger.info(message);
            request.addError(message);
        }
        return filenames;
    }

    @Override
    public InputStream read(String fileName) throws IOException {

        if (isLocal()) {
            File f = new File(fileName);
            if (f.canRead()) return new FileInputStream(f);
            return new ByteArrayInputStream("".getBytes());
        } else {
            if (!config.isUseSFTP()) return super.read(fileName);

            try {
                return channel.get(fileName);
            } catch (SftpException e) {
                String err = String.format("Failed to download %s: %s", fileName, e.getMessage());
                log4jLogger.warn(err);
                log4jLogger.info(JschLogger.getErrors(this));
                log4jLogger.trace(ExceptionToString.format(e));
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
    public boolean sendFile(InputStream stream, FTPRequest request) throws IOException {
        if (!config.isUseSFTP()) return super.sendFile(stream, request);
        String fileName = request.getRemoteName();
        if (fileName.endsWith("/")) {
            log4jLogger.error("fileName must be the name of a file, not a directory");
            request.addError("fileName must be the name of a file, not a directory");
        } else {
            try {
                int lio = fileName.lastIndexOf('/');
                if (lio > 0) {
                    channel.cd(fileName.substring(0, lio));
                }
                channel.put(stream, (lio > 0) ? fileName.substring(lio + 1) : fileName);
                return true;
            } catch (SftpException e) {
                List<String> errors = JschLogger.getErrors(this);
                log4jLogger.info(errors);
                request.addError(errors.get(errors.size() - 1));
                throw new IOException("Cannot copy data to " + fileName, e);
            }
        }
        return false;
    }
}
