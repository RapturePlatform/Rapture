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
package rapture.stat;

import java.util.List;
import java.util.Map;

/**
 * This interface is the main entry point for an implementation that can.
 * 
 * a. record stats of various types (message, value, presence) b. compute stats
 * on those records (to generate a history) c. retrieve the history for a stat
 * d. retrieve a top level "current" status e. define the capabilities of stats
 * 
 * @author amkimian
 * 
 */
public interface IRaptureStatApi {
    /**
     * Compute this record
     * 
     * @param key
     */

    void computeRecord(String key);

    /**
     * Given a unique key and a config, define a statistic. The config really
     * specifies how the data should be summarized.
     * 
     * @param key
     * @param config
     */
    void defineStat(String key, String config);

    /**
     * Get a document describing the "current status". Each stat recorded
     * returns the last computed stat, except for the string stat type which
     * returns the latest value in the ring.
     * 
     * @return
     */
    Map<String, BaseStat> getCurrentStat();

    /**
     * Return a timestamped history for this stat
     * 
     * @param key
     * @return
     */
    List<? extends BaseStat> getHistory(String key);

    /**
     * Return a list of all of the stat keys recorded (actually defined)
     * 
     * @return
     */
    List<String> getStatKeys();

    void recordCounter(String key, long delta);

    /**
     * Record a presence, given a key and a unique instance name. The presence
     * is updated if it already exists (i.e. its timestamp is refreshed).
     * Key+instance also expires after a set configurable timeout
     * 
     * Computing this stat returns (records) the number of unique instances
     * 
     * @param key
     */
    void recordPresence(String key, String instanceName);

    /**
     * Record a string value. A string value in a stat is usually a circular
     * buffer of recent events The number of entries in the buffer is controlled
     * by the config.
     * 
     * Computing this stat does nothing.
     * 
     * @param key
     * @param value
     */
    void recordStringStat(String key, String value);

    /**
     * Record a value at this point in time for this key. Values eventually
     * expire (and are cleaned out)
     * 
     * Computing this value converts old values to a rate (by summation or
     * average usually).
     * 
     * @param key
     * @param value
     */
    void recordValue(String key, long value);
}
