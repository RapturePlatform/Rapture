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
package rapture.repo.cassandra.key;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.google.common.base.Optional;

/**
 * @author bardhi
 * @since 3/10/15.
 */
public class KeyNormalizer {
    private static final String NORMALIZED_FORMAT = "%s/%s";
    private static final Logger log = Logger.getLogger(KeyNormalizer.class);
    private final Optional<String> pKeyPrefix;

    public KeyNormalizer(Optional<String> pKeyPrefix) {
        this.pKeyPrefix = pKeyPrefix;
    }

    public List<NormalizedKey> normalizeKeys(List<String> original) {
        List<NormalizedKey> normalized = new ArrayList<>(original.size());
        for (String key : original) {
            normalized.add(normalizeKey(key));
        }
        return normalized;
    }

    /**
     * Normalize a key by prepending any necessary pKeyPrefix to it
     *
     * @param key
     * @return
     */
    public NormalizedKey normalizeKey(String key) {
        if (pKeyPrefix.isPresent()) {
            return new NormalizedKey(String.format(NORMALIZED_FORMAT, pKeyPrefix.get(), key));
        } else {
            return new NormalizedKey(key);
        }
    }

    /**
     * strip end slashes from prefix since in cassandra we store each folder name as a key
     *
     * @param prefix
     * @return
     */
    public NormalizedKey normalizePrefix(String prefix) {
        prefix = PathStripper.stripPrefix(prefix);
        return normalizeKey(prefix);
    }
}
