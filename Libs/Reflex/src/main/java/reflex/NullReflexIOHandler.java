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
package reflex;

import reflex.value.ReflexArchiveFileValue;
import reflex.value.ReflexFileValue;
import reflex.value.ReflexStreamValue;
import reflex.value.ReflexValue;

public class NullReflexIOHandler implements IReflexIOHandler {

 
    @Override
    public ReflexValue forEachLine(ReflexStreamValue fileValue, IReflexLineCallback iReflexLineCallback) {
    	throw new ReflexException(-1, "Not allowed");
    }

    @Override
    public ReflexValue getContent(ReflexStreamValue fileValue) {
    	throw new ReflexException(-1, "Not allowed");
    }

    /**
     * Return a boolean value indicating whether this file is a file
     * 
     * @return
     */
    @Override
    public ReflexValue isFile(ReflexFileValue fileValue) {
    	throw new ReflexException(-1, "Not allowed");
    }

    /**
     * Return a boolean value indicating whether this file is a folder
     * 
     * @return
     */
    @Override
    public ReflexValue isFolder(ReflexFileValue fileValue) {
    	throw new ReflexException(-1, "Not allowed");
    }

    @Override
    public ReflexValue readdir(ReflexFileValue fileValue) {
    	throw new ReflexException(-1, "Not allowed");
    }

    @Override
    public void writeFile(ReflexFileValue asFile, String data) {
    	throw new ReflexException(-1, "Not allowed");
    }


    @Override
    public ReflexValue getArchiveEntry(ReflexArchiveFileValue fVal) {
    	throw new ReflexException(-1, "Not allowed");
    }

    @Override
    public void writeArchiveEntry(ReflexArchiveFileValue fVal, ReflexValue value) {
    	throw new ReflexException(-1, "Not allowed");
    }

    @Override
    public void close(ReflexArchiveFileValue fVal) {
    	throw new ReflexException(-1, "Not allowed");
    }

    @Override
    public void remove(ReflexFileValue toDelete) {
    	throw new ReflexException(-1, "Not allowed");
    }

    @Override
    public boolean hasCapability() {
        return false;
    }

    @Override
    public void mkdir(ReflexFileValue asFile) {
        throw new ReflexException(-1, "Not allowed");
    }

}
