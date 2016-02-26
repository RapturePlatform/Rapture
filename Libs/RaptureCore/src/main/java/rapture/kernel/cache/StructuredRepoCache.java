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
package rapture.kernel.cache;

import org.apache.log4j.Logger;

import rapture.common.Scheme;
import rapture.common.StructuredRepoConfig;
import rapture.common.StructuredRepoConfigStorage;
import rapture.repo.StructuredRepo;
import rapture.structured.StructuredFactory;
import rapture.structured.StructuredStore;

public class StructuredRepoCache extends AbstractRepoCache<StructuredRepoConfig, StructuredRepo> {
    @SuppressWarnings("unused")
    private static Logger log = Logger.getLogger(StructuredRepoCache.class);

    public StructuredRepoCache() {
        super(Scheme.STRUCTURED.toString());
    }

    @Override
    public StructuredRepoConfig reloadConfig(String authority) {
        return StructuredRepoConfigStorage.readByFields(authority);
    }

    @Override
    public StructuredRepo reloadRepository(StructuredRepoConfig config, boolean autoloadIndex) {
        StructuredStore store = StructuredFactory.getRepo(config.getConfig(), config.getAuthority());
        return store == null ? null : new StructuredRepo(store);
    }

}
