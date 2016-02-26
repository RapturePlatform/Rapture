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
package rapture.common.dp;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Contains constants for reserved execution context variables.
 *
 * @author bardhi
 */
public class ContextVariables {
    public static final String LOCK_KEY = "$__reserved_lock_key";
    public static final String DP_WORKER_URI = "$DP_WORKER_URI";
    public static final String DP_WORK_ORDER_URI = "$DP_WORK_ORDER_URI";
    public static final String DP_WORKER_ID = "$DP_WORKER_ID";
    public static final String DP_STEP_NAME = "$DP_STEP_NAME";
    public static final String DP_STEP_START_TIME = "$DP_STEP_START_TIME";
    public static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("yyyyMMdd");

    /*
     * The variables below are set when a workorder is executed by a job
     */
    // execution timestamp
    public static final String TIMESTAMP = "$__timestamp";
    // date string, e.g. 20121301
    public static final String LOCAL_DATE = "$__date_string";
    // URI of job that spawned this workorder
    public static final String PARENT_JOB_URI = "$__parent_job_uri";

}
