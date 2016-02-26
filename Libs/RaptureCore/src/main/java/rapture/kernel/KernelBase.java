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
package rapture.kernel;

import java.net.HttpURLConnection;

import org.apache.log4j.Logger;

import rapture.common.Messages;
import rapture.common.RaptureURI;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.repo.Repository;
import rapture.util.StringComplianceCheck;

public abstract class KernelBase {
    static Logger log = Logger.getLogger(KernelBase.class);

    private Kernel kernel;

    private Repository bootstrapRepo;

    private Repository configRepo;

    private Repository settingsRepo;

    private Repository ephemeralRepo;

    Messages apiMessageCatalog;

    public KernelBase(Kernel raptureKernel) {
        this.kernel = raptureKernel;
        apiMessageCatalog = new Messages("Api");
        resetRepo();
    }

    protected void checkParameter(String name, Object parameter) {
        if (parameter == null || ((parameter instanceof String) && (((String)parameter).isEmpty()))) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NullEmpty", name)); //$NON-NLS-1$
        }
    }

    protected void checkRaptureURI(String name, RaptureURI uri){
        checkParameter(name, uri.getFullPath());
    }

    protected void checkValidAuthority(String name) {
        StringComplianceCheck.checkNoInvalidChars(name);
    }

    protected void checkValidTypeName(String name) {
        StringComplianceCheck.checkNoInvalidChars(name);
    }

    protected Repository getBootstrapRepo() {
        return bootstrapRepo;
    }

    public Repository getConfigRepo() {
        return configRepo;
    }

    protected Repository getEphemeralRepo() {
        return ephemeralRepo;
    }

    protected Kernel getKernel() {
        return kernel;
    }

    public Repository getSettingsRepo() {
        return settingsRepo;
    }

    public void resetRepo() {
        bootstrapRepo = kernel.getRepo("sys.bootstrap");
        configRepo = kernel.getRepo("sys.RaptureConfig");
        settingsRepo = kernel.getRepo("sys.RaptureSettings");
        ephemeralRepo = kernel.getRepo("sys.RaptureEphemeral");
    }

    void start() {
    };

    void end() {
    };

}
