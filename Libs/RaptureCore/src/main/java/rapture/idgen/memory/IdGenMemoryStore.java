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
package rapture.idgen.memory;

import java.util.Map;

import rapture.common.exception.RaptureExceptionFactory;
import rapture.dsl.idgen.IdGenStore;

public class IdGenMemoryStore implements IdGenStore {

    private Long idgen = 0L;
    private boolean valid = true;
    
    @Override
    public Long getNextIdGen(Long interval) {
        if (!valid) throw RaptureExceptionFactory.create("idgen has been deleted");
        idgen = idgen + interval;
        return idgen;
    }

    @Override
    public void resetIdGen(Long number) {
        idgen = number;
    }

    @Override
    public void setConfig(Map<String, String> config) {
    }

    @Override
    public void setInstanceName(String instanceName) {
        // unused, ignore
    }

    @Override
    public void invalidate() {
        valid = false;        
    }
    
    public void makeValid() {
        valid = true;
    }

    @Override
    public void init() {
        //nothing to do        
    }
}
