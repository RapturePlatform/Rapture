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
package reflex.app;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import jline.console.ConsoleReader;

import org.apache.log4j.Logger;

import rapture.common.client.HttpLoginApi;
import rapture.common.client.ScriptClient;
import rapture.common.client.SimpleCredentialsProvider;
import rapture.common.exception.RaptureException;
import rapture.config.ParameterValueReader;
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
        
        if(args.length == 0){
            showProgramUsage();
            System.exit(0);
        }
        
        ConsoleReader reader = new ConsoleReader();
        PrintWriter out = new PrintWriter(reader.getOutput());
        log.info("-------------------------------");
        log.info("ReflexRunner started");
        ParameterValueReader pvr = new ParameterValueReader(args);
        pvr.addMapping("r", true, "Rapture URL", "RAPTURE_HOST", "Rapture-Home");
        pvr.addMapping("u", true, "User", "RAPTURE_USER", "Rapture-User");
        pvr.addMapping("p", true, "Password", "RAPTURE_PASSWORD", "Rapture-Password");
        pvr.addMapping("f", true, "Script file", null, null);
        pvr.addMapping("d", false, "Debug", null, null);
        pvr.addMapping("i", false, "Instrument", null, null);
        pvr.addMapping("params", true, "Parameters to pass script", null, null);
  
        try {
            // Log onto Rapture, pass the script context to
            // Reflex and Run the reflex script
            // Now lets go

            String rapture = pvr.getValue("r");
            if (rapture == null) {
                log.error("Rapture URL is undefined. Specify a URL using -r or set environment variable RAPTURE_HOST.");
                System.exit(1);
            }
            
            String user = pvr.getValue("u");
            String password = pvr.getValue("p");
            if (user == null) {
                user = reader.readLine("User: ");
            } else {
                log.info("User: " + user);
            }
            if (password == null) {
                password = reader.readLine("Password: ", '*');
            } else {
                log.info("Password: ******");
            }

            // Load Modules
            AddinLoader.loadAddins();

            HttpLoginApi loginApi = new HttpLoginApi(rapture, new SimpleCredentialsProvider(user, password));
            loginApi.login();
            log.info("Logged into " + rapture);
            ScriptClient sc = new ScriptClient(loginApi);

            Map<String, Object> params = null;
            String optionString = pvr.getValue("params");
            if (optionString != null) {
                params = StringUtil.getMapFromString(optionString);
                log.debug("Loaded params: " + params.toString());
            }
            IReflexHandler handler = new ReflexHandler(sc, false);
            // Get program from -f
            String program;
            boolean instrument = (pvr.getValue("i") != null);
            boolean debug = (pvr.getValue("d") != null);
            IReflexDebugger debugger = null;
            InstrumentDebugger idebugger = null;
            if(instrument && debug) {
                log.error("-d and -i cannot be used at same time.");
                System.exit(2);
            }
            if (debug) {
                debugger = new ReflexRunnerDebugger(handler, reader, out);
                log.info("Debugger switched on.");
            }
            if (instrument) {
                idebugger = new InstrumentDebugger();
                debugger = idebugger;
            }
            if (debugger == null) {
                debugger = new NullDebugger();
            }
            try {
                String filename = pvr.getValue("f");
                if (filename == null) {
                    log.error("Read from stdin currently not supported. Use:  -f filename");
                } else {
                    program = DefaultReflexIOHandler.getStreamAsString(new FileInputStream(filename));
                    if (instrument) {
                        idebugger.setProgram(program);
                        log.info("Code Instrumention switched on.");
                    }
                    log.info("Running script: " + filename);
                    log.info("Output from script: ");
                    Object ret = ReflexExecutor.runReflexProgram(program, handler, params, debugger);
                    log.info("Return from script: " + ret.toString());
                    log.info("Script execution finished.");
                    if (instrument) {
                        log.info("Instrumentation output:");
                        System.out.println(idebugger.getInstrumenter().getInstrumentLogs());
                    }
                }
            } catch (FileNotFoundException e) {
                log.error(e.getMessage());
            }
        } catch (RaptureException e) {
            log.error("Error during setup activity - " + e.getMessage());
        } finally {
            log.info("-------------------------------");
        }
    }
    
    private static void showProgramUsage(){
        System.out.println("Overview:");
        System.out.println("    ReflexRunner is a Java command line tool that will read and execute a local reflex script against a remote Rapture instance.");
        System.out.println("CLI Usage:");
        System.out.println("    ReflexRunner [-r Rapture API URL] [-u Rapture username] [-p Rapture password] [-f Script file] [-params Script parameters] [-d Debug] [-i Instrument]");
        System.out.println("Examples:");
        System.out.println("    * Execute a script with a parameter     : ReflexRunner -r http://localhost:8665/rapture -f myScript.rfx -param x=1");
        System.out.println("    * Execute a script with 2 parameters    : ReflexRunner -r http://localhost:8665/rapture -f myScript.rfx -param x=1,y=abc");
        System.out.println("    * Execute a script with instrumentation : ReflexRunner -r http://localhost:8665/rapture -f myScript.rfx -i");
        System.out.println("Usage notes:");
        System.out.println("    * -r and -f are mandatory parameters");
        System.out.println("    * User name and password will be prompted for if not set using switches or environment variables");
        System.out.println("    * Cannot use -i and -d switches at the same time");
        System.out.println("    * Parameter input format is a comma seperated list of: <variable name>=<variable value>");
        System.out.println("Supported environment variables:");
        System.out.println("    * RAPTURE_HOST sets -r");
        System.out.println("    * RAPTURE_USER sets -u");
        System.out.println("    * RAPTURE_PASSWORD sets -p");
        System.out.println("Exit codes:");
        System.out.println("    * 0: No parameters passed.");
        System.out.println("    * 1: Rapture URL (-r) parameter or environment variable is missing");
        System.out.println("    * 2: Switches -d and -i cannot be used at same time");
    }
    
}