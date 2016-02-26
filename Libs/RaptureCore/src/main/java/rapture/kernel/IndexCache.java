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
package rapture.kernel;

import rapture.index.IndexHandler;
import rapture.table.IndexFactory;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * The index cache, like the repo cache, holds instantiated index
 * instances, and are created on demand.
 *
 * @author alan
 *
 */
public class IndexCache {
    private static Logger log = Logger.getLogger(IndexCache.class);

    private Map<String, IndexHandler> indexCache;

    public IndexCache() {
        indexCache = new HashMap<String, IndexHandler>();
    }

    public IndexHandler getIndex(String indexURI) {

        if (indexCache.containsKey(indexURI)) {
            return indexCache.get(indexURI);
        } else {
            log.debug("Index " + indexURI + " not cached, loading config");
            try {
                IndexHandler index = IndexFactory.getIndex(indexURI);
                indexCache.put(indexURI, index);
                return index;
            } catch (Exception e) {
                log.error("Error getting index " + e.getMessage());
            }
        }
        return null;
    }

}
