/**
 * The MIT License (MIT)
 *
 * Copyright (C) 2011-2016 Incapture Technologies LLC
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
package rapture.repo.mem;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import rapture.repo.StoreKeyVisitor;

@Ignore
public class MemKeyStoreTest {

    MemKeyStore mks = null;

    @Before
    public void setUp() throws Exception {
        mks = new MemKeyStore();
        mks.setConfig(null);
    }

    // Ignore("triggering Out Of Heap error")
    @Test
    public void testRAP2149() {
        Thread thread1 = new Thread() {
            @Override
            public void run() {
                for (int i = 0; i < Integer.MAX_VALUE; i++) {
                    try {
                        mks.put("key" + i, "val" + i);
                    } catch (Exception e) {
                        return;
                    }
                }
            }
        };
        Thread thread2 = new Thread() {
            @Override
            public void run() {
                for (int i = 1; i < 5; i++) {
                    try {
                        mks.visitKeys("key", new StoreKeyVisitor() {
                            public boolean visit(String key, String value) {
                                @SuppressWarnings("unused")
                                String got = key + ":" + value;
                                // System.out.println(got);
                                return true;
                            }
                        });
                    } catch (Exception e) {
                        Assert.fail(e.getMessage());
                    }
                }
            }
        };

        thread1.start();
        thread2.start();
        try {
            thread2.join();
        } catch (InterruptedException e) {
        }
        thread1.interrupt();
        mks.dropKeyStore();
    }
}
