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
package rapture.script;

import rapture.common.CallingContext;
import rapture.common.RaptureScript;
import rapture.common.ScriptResult;
import rapture.index.IndexHandler;

import java.util.List;
import java.util.Map;

/**
 * This is used to provide a standard interface to running scripts in Rapture
 * <p/>
 * Scripts are stored pieces of execution code, and have a purpose.
 * <p/>
 * Basically the RaptureScriptingEngine runs a script in a context, and given
 * the language associated with a Script an instance of IRaptureScript is
 * created which is then executed for the given purpose.
 *
 * @author alan
 */
public interface IRaptureScript {
    /**
     * The goal of running this script is to ascertain whether, given the
     * parameters and document context , this document should be selected for
     * inclusion in something. If true, include it. If false, done't.
     */

    boolean runFilter(CallingContext context, RaptureScript script, RaptureDataContext data, Map<String, Object> parameters);

    /**
     * The goal of running a script in this way is for that script to manipulate
     * the index to add entries as needed
     *
     * @param context
     * @param script
     * @param indexHandler
     * @param data
     */
    void runIndexEntry(CallingContext context, RaptureScript script, IndexHandler indexHandler, RaptureDataContext data);

    /**
     * The goal of running this script is to map the values in the data context
     * into a Map of (column name, value) entries that is what needs to be
     * ultimately incorporated into a view result (mainly)
     */

    List<Object> runMap(CallingContext context, RaptureScript script, RaptureDataContext data, Map<String, Object> parameters);

    /**
     * Run an operation with this context
     *
     * @param context
     * @param ctx
     * @param params
     * @return
     */
    String runOperation(CallingContext context, RaptureScript script, String ctx, Map<String, Object> params);

    /**
     * Run a general script/program
     *
     * @param context
     * @param script
     * @param extraVals
     * @return
     */
    String runProgram(CallingContext context, IActivityInfo activityInfo, RaptureScript script, Map<String, Object> extraVals);

    /**
     * Run a script with extended return value
     *
     * @param context
     * @param script
     * @param activity
     * @param params
     * @return
     */
    ScriptResult runProgramExtended(CallingContext context, IActivityInfo activity, RaptureScript script, Map<String, Object> params);

    /**
     * Check that a script is valid
     *
     * @param context
     * @param script
     * @return
     */
    String validateProgram(CallingContext context, RaptureScript script);
}
