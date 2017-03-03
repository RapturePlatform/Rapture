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
package rapture.dsl.idgen;

import java.net.HttpURLConnection;
import java.util.Map;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.apache.log4j.Logger;

import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.exception.RaptureExceptionFormatter;
import rapture.generated.IdGenLexer;
import rapture.generated.IdGenParser;
import rapture.idgen.file.FileIdGenStore;
import rapture.idgen.memory.IdGenMemoryStore;

/**
 * This class is used to create idGen interfaces from config.
 * 
 */

public final class IdGenFactory {
    private static Logger log = Logger.getLogger(IdGenFactory.class);

    public static RaptureIdGen getIdGen(String config) {
        // Given a config that is formatted in the idGen command
        // string, create a RaptureIdGen
        // and assign the appropriate features and implementation to it.
        //
        try {
            IdGenParser parser = parseConfig(config);
            RaptureIdGen ret = new RaptureIdGen();
            switch (parser.getStore().getType()) {
            case IdGenLexer.FILE:
                FileIdGenStore ffs = new FileIdGenStore();
                ffs.setInstanceName(parser.getInstance());
                ffs.setConfig(parser.getConfig().getConfig());
                ret.setIdGenStore(ffs);
                break;
            case IdGenLexer.MEMORY:
                ret.setIdGenStore(new IdGenMemoryStore());
                break;
            case IdGenLexer.GDS:
                ret.setIdGenStore(getIdGenStore("rapture.idgen.google.IdGenGoogleStore", parser.getInstance(), parser.getConfig().getConfig()));
                break;
            case IdGenLexer.MONGODB:
                ret.setIdGenStore(getIdGenStore("rapture.idgen.mongodb.IdGenMongoStore", parser.getInstance(), parser.getConfig().getConfig()));
                break;
            default:
                log.error("Unsupported idGen store " + parser.getConfig().getName());
            }
            // Needs to be made valid before setting config
            ret.makeValid();
            ret.setProcessorConfig(parser.getProcessorConfig().getConfig());
            return ret;
        } catch (RecognitionException e) {
            log.error("Error parsing config - " + e.getMessage());
        }
        return null;
    }

    private static IdGenStore getIdGenStore(String className, String instance, Map<String, String> config) {
        Throwable throwable;
        try {
            Class<?> idGenClass = Class.forName(className);
            Object fStore;
            fStore = idGenClass.newInstance();
            if (fStore instanceof IdGenStore) {
                IdGenStore ret = (IdGenStore) fStore;
                ret.setInstanceName(instance);
                ret.setConfig(config);
                return ret;
            } else {
                RaptureException raptException = RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Could not create idGen");
                log.error(RaptureExceptionFormatter.getExceptionMessage(raptException, className + " is not a idGen, cannot instantiate"));
                throw raptException;
            }
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throwable = e;
        }
        RaptureException raptException = RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Could not create idGen");
        log.error(RaptureExceptionFormatter.getExceptionMessage(raptException, throwable));
        throw raptException;
    }

    public static IdGenParser parseConfig(String config) throws RecognitionException {
        IdGenLexer lexer = new IdGenLexer();
        log.info("Creating idGen from config - " + config);
        lexer.setCharStream(new ANTLRStringStream(config));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        IdGenParser parser = new IdGenParser(tokens);
        parser.iinfo();
        return parser;
    }

    private IdGenFactory() {
    }
}
