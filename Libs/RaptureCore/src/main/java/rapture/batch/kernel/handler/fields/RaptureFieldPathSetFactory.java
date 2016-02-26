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
package rapture.batch.kernel.handler.fields;

import java.util.HashSet;
import java.util.Set;

import rapture.common.RaptureFieldPath;

public final class RaptureFieldPathSetFactory {

    /**
     * Create a series of fieldpaths
     * 
     * Fields are type=path;type2=path
     * 
     * @param string
     * @return
     */
    public static Set<RaptureFieldPath> createFrom(String config) {
        Set<RaptureFieldPath> ret = new HashSet<RaptureFieldPath>();
        String[] paths = config.split(";");
        for (String pathConfig : paths) {
            String[] pathParts = pathConfig.split(",");
            if (pathParts.length > 1) {
                RaptureFieldPath fieldPath = new RaptureFieldPath();
                fieldPath.setTypeName(pathParts[0]);
                fieldPath.setPath(pathParts[1]);
                ret.add(fieldPath);
            }
        }
        return ret;
    }

    private RaptureFieldPathSetFactory() {

    }

}
