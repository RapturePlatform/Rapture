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
package rapture.kernel.schedule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import rapture.common.RaptureJobExec;

/**
 * @author bardhi
 * @since 1/28/15.
 */
public class JobExecSort {
    private static final Comparator<RaptureJobExec> COMPARATOR = new Comparator<RaptureJobExec>() {
        @Override
        public int compare(RaptureJobExec o1, RaptureJobExec o2) {
            if (o1 == null) {
                if (o2 == null) {
                    return 0;
                } else {
                    return -1;
                }
            }
            if (o2 == null) {
                return 1;
            }

            if (o1.getExecTime() == null) {
                if (o2.getExecTime() == null) {
                    return 0;
                } else {
                    return -1;
                }
            }

            if (o2.getExecTime() == null) {
                return 1;
            }

            return o1.getExecTime().compareTo(o2.getExecTime());
        }
    };

    public static List<RaptureJobExec> sortByExecTime(List<RaptureJobExec> original) {
        List<RaptureJobExec> ret = new ArrayList<>(original);
        Collections.sort(ret, COMPARATOR);
        return ret;
    }
}
