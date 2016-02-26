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

import rapture.common.Scheme;
import rapture.common.SeriesRepoConfig;
import rapture.common.SeriesRepoConfigStorage;
import rapture.repo.SeriesRepo;
import rapture.series.SeriesFactory;
import rapture.series.SeriesStore;

/**
 * Created by yanwang on 6/23/14.
 */
public class SeriesRepoCache extends AbstractRepoCache<SeriesRepoConfig, SeriesRepo> {

    public SeriesRepoCache() {
        super(Scheme.SERIES.toString());
    }

    @Override
    public SeriesRepoConfig reloadConfig(String authority) {
        return SeriesRepoConfigStorage.readByFields(authority);
    }

    @Override
    public SeriesRepo reloadRepository(SeriesRepoConfig seriesRepoConfig, boolean autoloadIndex) {
        SeriesStore store = SeriesFactory.createStore(seriesRepoConfig.getConfig(), seriesRepoConfig.getAuthority());
        return store == null ? null : new SeriesRepo(store);
    }
}
