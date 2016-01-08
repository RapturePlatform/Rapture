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
package reflex.app;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import jline.console.ConsoleReader;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;

import rapture.common.client.HttpLoginApi;
import rapture.common.client.ScriptClient;
import rapture.common.client.SimpleCredentialsProvider;
import rapture.common.exception.RaptureException;
import rapture.module.AddinLoader;
import rapture.util.StringUtil;
import reflex.DefaultReflexIOHandler;
import reflex.IReflexHandler;
import reflex.ReflexExecutor;
import reflex.debug.IReflexDebugger;
import reflex.debug.NullDebugger;
import reflex.util.InstrumentDebugger;

public class Runner {
    private static Logger log = Logger.getLogger(Runner.class.getName());

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        ConsoleReader reader = new ConsoleReader();
        PrintWriter out = new PrintWriter(reader.getOutput());
        out.println("ReflexRunner");
        out.flush();
        Options options = new Options();
        options.addOption("r", true, "Rapture URL");
        options.addOption("u", true, "User");
        options.addOption("p", true, "Password");
        options.addOption("f", true, "Script file");
        options.addOption("d", false, "Debug");
        options.addOption("v", false, "Show in window");
        options.addOption("i", false, "Instrument");
        options.addOption("param", true, "Parameters to pass script");

        CommandLineParser parser = new PosixParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            // Log onto Rapture, pass the script context to
            // Reflex and Run the reflex script
            // Now lets go

            String password = "";
            String user = "";
            if (!cmd.hasOption('u')) {
                user = reader.readLine("User: ");
            } else {
                user = cmd.getOptionValue('u');
                out.println("User: " + user);
            }
            if (!cmd.hasOption('p')) {
                password = reader.readLine("Password: ", '*');
            } else {
                password = cmd.getOptionValue('p');
                out.println("Password: ******");
            }

            // Load Modules
            AddinLoader.loadAddins();

            HttpLoginApi loginApi = new HttpLoginApi(cmd.getOptionValue('r'), new SimpleCredentialsProvider(user, password));
            out.println("Logging in...");
            out.flush();
            loginApi.login();
            out.println("Done...");
            ScriptClient sc = new ScriptClient(loginApi);

            Map<String, Object> params = null;
            if (cmd.hasOption("param")) {
                String optionString = cmd.getOptionValue("param");
                params = StringUtil.getMapFromString(optionString);
            }
            IReflexHandler handler = new ReflexHandler(sc, false);
            // Get program from -f
            String program;
            boolean instrument = cmd.hasOption('i');
            boolean debug = cmd.hasOption('d');
            IReflexDebugger debugger = null;
            InstrumentDebugger idebugger = null;
            if (debug) {
                debugger = new ReflexRunnerDebugger(handler, reader, out);
            }
            if (instrument && !debug) {
                idebugger = new InstrumentDebugger();
                debugger = idebugger;
            }
            if (debugger == null) {
                debugger = new NullDebugger();
            }
            try {
                out.println("Loading program...");
                program = DefaultReflexIOHandler.getStreamAsString(new FileInputStream(cmd.getOptionValue('f')));
                if (instrument) {
                    idebugger.setProgram(program);
                }
                out.println("Done...");
                out.println("Running program...");
                out.println("-------------------");
                out.flush();
                Object ret = ReflexExecutor.runReflexProgram(program, handler, params, debugger);
                out.println("-------------------");
                out.println("Return from program is " + ret.toString());
                if (instrument) {
                    idebugger.getInstrumenter().log();
                }
            } catch (FileNotFoundException e) {
                log.error(e.getMessage());
            }

        } catch (ParseException e) {
            out.println("Parse error on command line options - " + e.getMessage());
            out.flush();
            System.exit(0);
        } catch (RaptureException e) {
            out.println("Error during setup activity - " + e.getMessage());
        }
        out.flush();
    }

}
