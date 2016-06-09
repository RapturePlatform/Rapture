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
package rapture.series;

import java.net.HttpURLConnection;
import java.util.Map;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.apache.log4j.Logger;

import rapture.common.RaptureURI;
import rapture.common.exception.ExceptionToString;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.generated.SRapGenLexer;
import rapture.generated.SRapGenParser;
import rapture.repo.RepoFactory;
import rapture.series.config.ConfigValidatorService;
import rapture.series.config.InvalidConfigException;

import com.google.common.collect.ImmutableMap;

public class SeriesFactory {

    /**
     *  @deprecated This is inconsistent with standard practice. Normally the authority/URI comes BEFORE the config string.
     */
    @Deprecated
    public static SeriesStore createStore(String config, String authority) {
        try {
            log.info("Creating repo from config - " + config);
            SRapGenParser parser = parseConfig(config);
            ConfigValidatorService.validateConfig(parser);
            return createSeriesStore(parser, authority);
        } catch (RecognitionException | InvalidConfigException e) {
            log.error("Error parsing config - " + e.getMessage());
            if (log.isDebugEnabled()) log.debug(ExceptionToString.format(e));
        } catch (RaptureException e) {
            log.error("Error when initializing repo - " + e.getMessage());
            if (log.isDebugEnabled()) log.debug(e.getFormattedMessage());
        }
        return null;
    }

    public static SeriesStore createStore(RaptureURI uri, String config) {
        try {
            log.info("Creating repo from config - " + config);
            SRapGenParser parser = parseConfig(config);
            ConfigValidatorService.validateConfig(parser);
            return createSeriesStore(parser, uri.getAuthority());
        } catch (RecognitionException | InvalidConfigException e) {
            log.error("Error parsing config - " + e.getMessage());
            if (log.isDebugEnabled()) log.debug(ExceptionToString.format(e));
        } catch (RaptureException e) {
            log.error("Error when initializing repo - " + e.getMessage());
            if (log.isDebugEnabled()) log.debug(e.getFormattedMessage());
        }
        return null;
    }

    private static Logger log = Logger.getLogger(RepoFactory.class);

    private static final Map<Integer, String> keyStoreImplementationMap = ImmutableMap.of(
        SRapGenLexer.MEMORY, "rapture.series.mem.MemorySeriesStore",
        SRapGenLexer.CASSANDRA, "rapture.series.cassandra.CassandraSeriesStore",
        SRapGenLexer.MONGODB, "rapture.series.mongo.MongoSeriesStore",
        SRapGenLexer.CSV, "rapture.series.mem.CSVSeriesStore",
	    SRapGenLexer.FILE, "rapture.series.file.FileSeriesStore");

    private static SeriesStore createSeriesStore(String className, String instanceName, Map<String, String> config) {
        try {
            Class<?> seriesClass = Class.forName(className);
            Object fStore;
            fStore = seriesClass.newInstance();
            if (fStore instanceof SeriesStore) {
                SeriesStore ret = (SeriesStore) fStore;
                ret.setInstanceName(instanceName);
                ret.setConfig(config);
                return ret;
            } else {
                String message = (className + " is not a repo, cannot instantiate");
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, message);
            }
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error creating series store of type " + className, e);
        }
    }

    private static SeriesStore createSeriesStore(SRapGenParser parser, String authority) {
        // parser should have everything we want now
        int storeType = parser.getStoreType();
        if (keyStoreImplementationMap.containsKey(storeType)) {
        	// TODO: Alan - why was this authority as the instanceName?
            return createSeriesStore(keyStoreImplementationMap.get(storeType), "default", parser.getConfig().getConfig());
        } else {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "Unsupported key store type");
        }
    }

    public static SRapGenParser parseConfig(String config) throws RecognitionException {
        SRapGenLexer lexer = new SRapGenLexer();
        lexer.setCharStream(new ANTLRStringStream(config));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        SRapGenParser parser = new SRapGenParser(tokens);
        parser.repinfo();
        return parser;
    }

    private SeriesFactory() {
    }
}
