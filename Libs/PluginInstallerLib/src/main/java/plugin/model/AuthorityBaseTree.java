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
package plugin.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuthorityBaseTree<T extends AuthorityBaseReference<T>> extends BaseTree<T> {
    private Map<String, List<T>> referencesByAuthority;

    public AuthorityBaseTree() {
        this.referencesByAuthority = new HashMap<String, List<T>>();
    }

    public void addReference(T reference) {
        String authority = reference.getAuthority();
        ensureAuthorityPresent(authority);
        referencesByAuthority.get(authority).add(reference);
    }

    private void ensureAuthorityPresent(String authority) {
        if (!referencesByAuthority.containsKey(authority)) {
            referencesByAuthority.put(authority, getNewReferenceList());
        }
    }

    private List<T> getNewReferenceList() {
        return new ArrayList<T>();
    }

    public void workThrough(int skipPercent, ReferenceWorker<T> worker) {
        for (Map.Entry<String, List<T>> entry : referencesByAuthority.entrySet()) {
            worker.startAuthority(entry.getKey());
            Collections.sort(entry.getValue());
            int total = entry.getValue().size();
            System.out.println("Size is " + total);
            int count = 0;
            int skipCount = 0;
            if (skipPercent != -1) {
                System.out.println("Skipping by " + skipPercent);
                skipCount = skipPercent * total / 100;
            }
            for (T reference : entry.getValue()) {
                count++;
                if (skipCount > 0) {
                    skipCount--;
                }
                if (skipCount <= 0) {
                    worker.workWithReference(reference);
                }
                if (count % 1000 == 0) {
                    System.out.println("Processed " + count + " out of " + total + "(" + count * 100 / total + "%)");
                }
            }
        }
        worker.finished();
    }
}
