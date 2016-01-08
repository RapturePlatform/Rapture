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
package rapture.persistence.storable.mapper;

import rapture.common.model.RaptureUser;
import rapture.common.version.OldestVersion;

/**
 * @author bardhi
 * @since 7/21/15.
 */
public class RaptureUserSerDeserHelper implements StorableSerDeserHelper<RaptureUser, RaptureUserExtended> {

    @Override
    public Class<RaptureUser> getStorableClass() {
        return RaptureUser.class;
    }

    @Override
    public Class<RaptureUserExtended> getExtendedClass() {
        return RaptureUserExtended.class;
    }

    public void onSerialize(RaptureUser user) {
//        user.setUsername(user.getUserId()); not ready yet
    }

    public void onDeserialize(RaptureUserExtended user) {
        if (user.get_raptureVersion().compareTo(OldestVersion.INSTANCE) <= 0) {
            user.setUserId(user.getUsername());
        }
    }
}
