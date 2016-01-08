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
package rapture.common;

import java.util.ArrayList;
import java.util.List;

/**
 * Note that this class is referenced in types.api - any changes to this file should be reflected there.
**/

public class NotificationResult {

    @Override
    public String toString() {
        return "NotificationResult [currentEpoch=" + currentEpoch + ", references=" + references + "]";
    }

    public Long getCurrentEpoch() {
        return currentEpoch;
    }

    public void setCurrentEpoch(Long currentEpoch) {
        this.currentEpoch = currentEpoch;
    }

    public List<String> getReferences() {
        return references;
    }

    public void setReferences(List<String> references) {
        this.references = references;
    }

    private Long currentEpoch;
    private List<String> references = new ArrayList<String>();

    public NotificationResult() {

    }

    public NotificationResult(Long currentEpoch) {
        this.currentEpoch = currentEpoch;
    }

    public void addId(String id) {
        references.add(id);
    }

}
