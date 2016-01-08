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
package rapture.batch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * The script parser takes an input reader and reads it line by line, assuming
 * it is of a batch script structure.
 * 
 * The batch script structure looks something like this: $$ COMMANDNAME
 * param=value param= xxx content xxx $$ ($$ separates commands) (then command
 * name) (then parameters, name=value, or name= and then a multi line value) For
 * each command thus defined, run through the executor. The parser creates the
 * commands and fires them off in stream, rather than parsing the whole thing
 * and then doing it
 * 
 * This allows for long scripts...
 * 
 * @author amkimian
 * 
 */
public class ScriptParser {
    private IBatchExecutor executor;
    private ScriptState state = ScriptState.WAITINGFORCOMMAND;
    private static Logger log = Logger.getLogger(ScriptParser.class);

    public ScriptParser(IBatchExecutor executor) {
        this.executor = executor;
    }

    private void executeCommand(String currentCommand, Map<String, String> params, OutputStream output) {
        executor.runGeneralCommand(currentCommand, params, output);
    }

    public void parseScript(Reader reader, OutputStream output) throws IOException {
        BufferedReader bufferedInput = new BufferedReader(reader);
        String line;
        String currentCommand = "";
        Map<String, String> params = null;
        String currentLongParamName = "";
        StringBuilder currentLongParam = null;
        String currentLongCommandTerminator = "";

        while ((line = bufferedInput.readLine()) != null) {
            switch (state) {
            case WAITINGFORCOMMAND:
                // Look for $$, ignore comments (#)
                if (line.startsWith("#") || line.isEmpty()) {
                    continue;
                }
                if (line.equals("$$")) {
                    state = ScriptState.INCOMMAND;
                } else {
                    log.error("Did not understand " + line + " when in state WAITINGFORCOMMAND");
                }
                break;
            case INCOMMAND:
                // Look for a string that represents a command
                if (line.isEmpty()) {
                    continue;
                } else {
                    currentCommand = line.trim();
                    params = new HashMap<String, String>();
                    state = ScriptState.LOOKFORPARAMS;
                }
                break;
            case LOOKFORPARAMS:
                if (line.isEmpty()) {
                    continue;
                }
                if (line.equals("$$")) {
                    executeCommand(currentCommand, params, output);
                    state = ScriptState.INCOMMAND;
                } else {
                    if (line.trim().endsWith("=")) {
                        // Multiline param
                        currentLongParamName = line.substring(0, line.indexOf('='));
                        state = ScriptState.MULTILINESTART;
                    } else {
                        int pos = line.indexOf('=');
                        if (pos != -1) {
                            String paramName = line.substring(0, pos);
                            String paramValue = line.substring(pos + 1);
                            params.put(paramName, paramValue);
                        }
                    }
                }
                break;
            case MULTILINESTART:
                if (line.isEmpty()) {
                    continue;
                }
                currentLongCommandTerminator = line;
                state = ScriptState.MULTILINEGRAB;
                currentLongParam = new StringBuilder();
                break;
            case MULTILINEGRAB:
                if (line.equals(currentLongCommandTerminator)) {
                    params.put(currentLongParamName, currentLongParam.toString());
                    state = ScriptState.LOOKFORPARAMS;
                } else {
                    currentLongParam.append(line);
                    currentLongParam.append("\n");
                }
                break;
            }
        }
    }
}
