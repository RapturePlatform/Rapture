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

import java.util.Map;

import org.apache.log4j.Logger;

import rapture.dsl.idgen.IdGenStore;

/**
 * This class implements a idgen on an underlying idgen store.
 * 
 * The implementation of the idgen store is provided by the factory that
 * creates this idgen
 */

public class RaptureIdGen {
    @Override
    public String toString() {
        return "RaptureIdGen [prefix=" + prefix + ", base=" + base + ", length=" + length + ", initial=" + initial + ", idgenStore=" + idgenStore + "]";
    }

    private static Logger log = Logger.getLogger(RaptureIdGen.class);
    
    private static final int DEFAULTBASE = 10;
    private static final String LENGTH = "length";
    private static final String BASE = "base";
    private static final String PREFIX = "prefix";
    private static final String INITIAL = "initial";
    /**
     * What prefix (if any) we should put before any ids returned by this
     * idgen
     */
    private String prefix = "";
    /**
     * What base to use to format the string returned back to the caller
     */
    private int base;
    /**
     * How many prefix chars? (if -1, means none, otherwise make sure the string
     * before the prefix is at least this many chars by inserting zeros
     */
    private int length;

    /**
     * Initial value if not zero. 
     * Useful if you have multiple limited-range idgens that cannot overlap. (eg Reuters)
     */
    private Long initial = 0L;
    
    /**
     * What to use to actually interact with the persistent idgen
     */

    private IdGenStore idgenStore;

    /**
     * Gets the idgenStore for this instance.
     * 
     * @return The idgenStore.
     */
    public IdGenStore getIdGenStore() {
        return this.idgenStore;
    }

    public String incrementIdGen(Long amount) {
        Long newNumber = idgenStore.getNextIdGen(amount);
        String val = Long.toString(newNumber, this.base).toUpperCase();
        // Keep to length chars?
        int _length = val.length();
        if (_length < length) {
            int lengthToAdd = length - val.length();
            String leadingZeros = new String(new char[lengthToAdd]).replace("\0", "0");
            val = leadingZeros + val;
        } else if (_length > length) {
            val = val.substring(_length - length);
        }
        if (prefix == null || prefix.isEmpty()) {
            return val;
        }
        return prefix + val;
    }

    // TODO: Take these static strings for the config and handle them better,
    // probably through some config class

    /**
     * Sets the idgenStore for this instance.
     * 
     * @param idgenStore
     *            The idgenStore.
     */
    public void setIdGenStore(IdGenStore idgenStore) {
        this.idgenStore = idgenStore;
        idgenStore.init();
    }

    public void setProcessorConfig(Map<String, String> config) {
        // Look for 4 aspects - prefix, base, length and initial value
        
        String prefix = config.get(PREFIX);
        if (prefix != null) {
            this.prefix = prefix;
        } else {
            this.prefix = "";
        }
        
        String b = config.get(BASE);
        if (b != null) {
            try {
                base = Integer.parseInt(b);
            } catch (NumberFormatException e) {
                log.error("Illegal base "+b+" - ignored");
                base = DEFAULTBASE;
            }
        } else {
            base = DEFAULTBASE;
        }
        
        String l = config.get(LENGTH);
        if (l != null) {
            try {
                length = Integer.parseInt(l);
            } catch (NumberFormatException e) {
                log.error("Illegal length "+l+" - ignored");
            }
        } else {
            length = 0;
        }
        
        String i = config.get(INITIAL);
        if (i != null) {
            try {
                initial = Long.parseLong(i);
                Long _init = idgenStore.getNextIdGen(0L);
                if ((initial > 0L) && (_init == 0L)) _init = idgenStore.getNextIdGen(initial);
            } catch (NumberFormatException e) {
                log.error("Illegal initial value "+i+" - ignored");
            }
        }
    }
    
    public void invalidate() {
        idgenStore.invalidate();
    }
    
    public void makeValid() {
        idgenStore.makeValid();
    }
}
