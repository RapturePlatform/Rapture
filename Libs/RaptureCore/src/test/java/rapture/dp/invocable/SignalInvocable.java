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
package rapture.dp.invocable;

import java.util.Set;

import org.apache.log4j.Logger;

import com.google.common.collect.Sets;

import rapture.common.CallingContext;
import rapture.common.dp.AbstractInvocable;

/**
 * This is a test class for single-node workflow tests
 * 
 * @author mel
 */
public class SignalInvocable extends AbstractInvocable {
    private static Logger log = Logger.getLogger(SignalInvocable.class);
    private final String key;
    private final long delay;

    public SignalInvocable(String workerURI, String stepName, String key, long delay) {
        super(workerURI, stepName);
        this.key = key;
        this.delay = delay;
    }

    @Override
    public String invoke(CallingContext ctx) {
        log.info("Signal " + key);
        if (delay > 0) try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            throw new IllegalStateException("Test received unexpected user interrupt");
        }
        if (key.startsWith("~")) Singleton.clearSignal(key.substring(1));
        else Singleton.setSignal(key);
        return "ok";
    }

    public static class Singleton {
        static Set<String> on = Sets.newHashSet();

        synchronized static public void setSignal(String key) {
            on.add(key);
        }

        synchronized static public void clearSignal(String key) {
            on.remove(key);
        }

        synchronized static public boolean testSignal(String key) {
            return on.contains(key);
        }
    }
}