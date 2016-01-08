/**
 * The MIT License (MIT)
 *
 * Copyright (C) 2011-2016 Incapture Technologies LLC
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
package rapture.kernel.stat;

public class StatConstants {
    public static final String USERS = "stat/users";
    public static final String CALLCOUNT = "stat/callcount";
    public static final String THROUGHPUT = "stat/throughput";
    public static final String EVENTRUN = "stat/runevent";
    public static final String TRIGGERRUN = "stat/triggerrun";
    public static final String TOTALCALLS = "count/totalcalls";
    public static final String EVENTSFIRED = "count/eventsfired";
    public static final String SCRIPTSRUN = "count/scriptsrun";
    public static final String SCRIPTRATE = "stat/scriptrate";

    // Stats around pipeline

    public static final String PIPELINERATE = "stat/pipelinerate";
    public static final String PIPELINETASKCOUNT = "count/pipelinecount";
    public static final String PIPELINEWAITCOUNT = "count/pipelinewait";
    public static final String PIPELINERUNNINGCOUNT = "count/pipelinerunning";

    public static final String APPPREFIX = "server/";
    public static final String WEBAPPS = APPPREFIX + "webapp";
    public static final String WORKERS = APPPREFIX + "worker";
}
