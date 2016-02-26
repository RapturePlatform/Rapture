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
package rapture.home;

import java.io.File;

public enum RaptureHomeRetriever {
    INSTANCE;

    private String globalConfigHome;
    private String home;
    private String appConfigHome;
    private String appName;

    RaptureHomeRetriever() {
        home = getPropOrEnv(RAPTURE_HOME);
        globalConfigHome = getPropOrEnv(GLOBAL_CONFIG_HOME);
        appName = getPropOrEnv(APP_NAME);
        if (appName != null && globalConfigHome != null) {
            appConfigHome = globalConfigHome + File.separator + appName;
        } else {
            appConfigHome = null;
        }
    }

    private String getPropOrEnv(String key) {
        String value = System.getProperty(key);
        if (value == null) {
            value = System.getenv(key);
        }
        return value;
    }

    public static final String GLOBAL_CONFIG_HOME = "RAPTURE_CONFIG_HOME";
    public static final String APP_NAME = "RAPTURE_APP_NAME";
    public static final String RAPTURE_HOME = "RAPTURE_HOME";

    public static String getRaptureHome() {
        return INSTANCE.home;
    }

    public static String getGlobalConfigHome() {
        return INSTANCE.globalConfigHome;
    }

    public static String getAppConfigHome() {
        return INSTANCE.appConfigHome;
    }

    public static String getAppName() {
        return INSTANCE.appName;
    }
}
