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
package rapture.sheet;

import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.apache.log4j.Logger;

import rapture.common.exception.RaptureExceptionFactory;
import rapture.generated.SheetGenLexer;
import rapture.generated.SheetGenParser;

/**
 * This class is used to create sheet interfaces from config.
 * 
 */

public final class SheetStoreFactory {
    @SuppressWarnings("unused")
    private static Logger log = Logger.getLogger(SheetStoreFactory.class);
    private static final Map<Integer, String> implementationMap;
    static {
        Map<Integer, String> setupMap = new HashMap<Integer, String>();
        setupMap.put(SheetGenLexer.MEMORY, "rapture.sheet.memory.MemorySheet"); //$NON-NLS-1$
        setupMap.put(SheetGenLexer.MONGODB, "rapture.sheet.mongodb.MongoDBSheet"); //$NON-NLS-1$
        setupMap.put(SheetGenLexer.CSV, "rapture.sheet.file.CSVSheetStore");
        setupMap.put(SheetGenLexer.FILE, "rapture.sheet.file.FileSheetStore");
        implementationMap = Collections.unmodifiableMap(setupMap);
    }

    public static SheetStore getSheetStore(String authority, String config) {
        try {
            SheetGenParser parser = getParsedForConfig(config);
            int implementationType = parser.getImplementationType();
            if (implementationMap.containsKey(implementationType)) {
                return getSheet(implementationMap.get(implementationType), authority, parser.getInstance(), parser.getImplementionConfig());
            } else {
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "Unsupported");
            }
        } catch (RecognitionException e) {
            //            log.error(Messages.getString("ErrorParsingConfig") + e.getMessage()); //$NON-NLS-1$
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error getting sheet store " + e.getMessage(), e);
        }
    }

    private static SheetStore getSheet(String className, String authority, String instance, Map<String, String> config) {
        try {
            Class<?> storeClass = Class.forName(className);
            Object fStore;
            fStore = storeClass.newInstance();
            if (fStore instanceof SheetStore) {
                SheetStore ret = (SheetStore) fStore;
                ret.setConfig(authority, config);
                // ret.setInstanceName(instance);
                // ret.setConfig(id, config);
                return ret;
            } else {
                String message = "Could not create sheet store of class " + className;
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, message);
            }
        } catch (InstantiationException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Could not create sheet store", e);
        } catch (IllegalAccessException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Could not create sheet store", e);
        } catch (ClassNotFoundException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Could not create sheet store", e);
        }
    }

    private static SheetGenParser getParsedForConfig(String config) throws RecognitionException {
        SheetGenLexer lexer = new SheetGenLexer();
        lexer.setCharStream(new ANTLRStringStream(config));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        SheetGenParser parser = new SheetGenParser(tokens);
        parser.loginfo();
        return parser;
    }

    private SheetStoreFactory() {
    }
}
