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
package rapture.persistence.storable.mapper.test;

import rapture.common.RaptureJob;
import rapture.persistence.storable.mapper.StorableSerDeserHelper;

/**
 *
 * @author bardhi
 * @since 7/21/15.
 */
public class JobSerDeserHelper implements StorableSerDeserHelper<RaptureJob, RaptureJob> {
    @Override
    public void onSerialize(RaptureJob storable) {

    }

    @Override
    public void onDeserialize(RaptureJob user) {

    }

    @Override
    public Class<RaptureJob> getStorableClass() {
        return RaptureJob.class;
    }

    @Override
    public Class<RaptureJob> getExtendedClass() {
        return null;
    }
}
