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
package watchserver.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

import rapture.app.RaptureAppService;
import rapture.common.exception.ExceptionToString;
import rapture.common.exception.RaptureException;
import rapture.config.ConfigLoader;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import watchserver.util.SourceType;
import watchserver.util.FTPConfig;
import watchserver.util.LocalConfig;
import watchserver.util.ConfigException;

/**
 * WatchServer monitors directories and takes actions as described in the
 * associated Rapture system configuration (//sys.config/watchserver/config)
 * document.
 * 
 * This document details directories and maps to an action (e.g. script,
 * workflow) in the Rapture system.
 * 
 * @author jonathan-major
 */
public final class WatchServer {
    private static Logger log = Logger.getLogger(WatchServer.class);
    private static final String CONFIG_URI = "watchserver/config";
    private static List<LocalConfig> localconfigs = new ArrayList<LocalConfig>();
    private static List<FTPConfig> ftpconfigs = new ArrayList<FTPConfig>();
    private static String rawConfig;
    
    public static void main(String[] args) {
        try {
            Kernel.initBootstrap(ImmutableMap.of("STD", ConfigLoader.getConf().StandardTemplate), WatchServer.class, false);
            RaptureAppService.setupApp("WatchServer");
        } catch (RaptureException e1) {
            log.error("Failed to start WatchServer with " + ExceptionToString.format(e1));
            System.exit(-1);
        }
        
        try {
            rawConfig = Kernel.getSys().retrieveSystemConfig(ContextFactory.getKernelUser(), "CONFIG", CONFIG_URI);
        } catch (Exception e) {
            log.error("Failed to load configuration from " + CONFIG_URI, e);
            System.exit(-1);
        } 
        log.info("------------------------------------------------------");
        try{
            JSONParser jsonParser = new JSONParser();
            JSONObject jsonObject = (JSONObject) jsonParser.parse(rawConfig);
            JSONArray sources = (JSONArray) jsonObject.get("sources");
            
            Iterator<?> it = sources.iterator();
            ObjectMapper mapper = new ObjectMapper();
            while (it.hasNext()) {
                JSONObject source = (JSONObject) it.next(); 
                switch (SourceType.valueOf(source.get("type").toString().toUpperCase())) {
                    case LOCAL:
                        LocalConfig localconfig = mapper.readValue(source.get("config").toString(), LocalConfig.class);
                        localconfig.setSourceType(SourceType.LOCAL);
                        localconfigs.add(localconfig);
                        log.info("Loaded config for Local folder: " + localconfig.getFolder());
                        break;
                    case FTP:
                        FTPConfig ftpconfig = mapper.readValue(source.get("config").toString(), FTPConfig.class);
                        ftpconfig.setSourceType(SourceType.FTP);
                        ftpconfigs.add(ftpconfig);
                        log.info("Loaded config for FTP folder: " + ftpconfig.getFolder() + ftpconfig.getConnection().getPathtomonitor());
                        break;
                    case SFTP:    
                        log.info("SFTP NooP");
                        break;    
                    default:
                        throw new Exception("SourceType " + source.get("type").toString().toUpperCase() + " not supported!");
                }
            }       
        } catch (ConfigException e) {
            log.error("ConfigException handling configuration from " + CONFIG_URI, e);
            System.exit(-1);
        } catch (JsonParseException e) {
            log.error("Json Parse Exception handling configuration from " + CONFIG_URI, e);
            System.exit(-1);
        } catch (JsonMappingException e) {
            log.error("Json Mapping Exception handling configuration from " + CONFIG_URI, e);
            System.exit(-1);
        } catch (IOException e) {
            log.error("IO Exception handling configuration from " + CONFIG_URI, e);
            System.exit(-1);
        } catch (Exception e) {
            log.error("Exception handling configuration from " + CONFIG_URI, e);
            System.exit(-1);
        } 
        log.info("------------------------------------------------------");
        for (LocalConfig config:localconfigs) {
            new WatchLocalRunner(config).startThread();
            log.info("Local Directory Monitor setup for " + config.getFolder());
        }
        
        for (FTPConfig config:ftpconfigs) {
            new WatchFTPRunner(config).startThread();
            log.info("FTP Monitor setup for " + config.getFolder());
        }
        log.info("------------------------------------------------------");
        log.info("WatchServer started and ready to process events.");
    }
}
