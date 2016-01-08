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
package rapture.kernel.cache;

import rapture.common.exception.RaptureExceptionFactory;
import rapture.kernel.Kernel;
import rapture.kernel.internalnotification.TypeChangeManager;
import rapture.notification.NotificationMessage;
import rapture.notification.RaptureMessageListener;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A repo cache that listens to events on type change from remote instances,
 * and publishes events on type change to remote instances.
 *
 * @param <CONFIG>
 */
public abstract class AbstractRepoCache<CONFIG, REPO> implements RaptureMessageListener<NotificationMessage> {

    private String type;

    private Map<String, CONFIG> configCache;
    protected Map<String, REPO> repoCache;

    public AbstractRepoCache(String type) {
        this.type = type;
        Kernel.getKernel().registerTypeListener(type, this);

        configCache = new ConcurrentHashMap<String, CONFIG>();
        repoCache = new ConcurrentHashMap<String, REPO>();
    }

    public void addRepo(String authority, CONFIG config, REPO repository) {
        if ((config == null) || (repository == null))
            throw RaptureExceptionFactory.create("Cannot add null config or repository. (Check if Mongo is running)");
        configCache.put(authority, config);
        repoCache.put(authority, repository);
    }

    public void removeRepo(String authority) {
        removeRepo(authority, true);
    }

    public boolean hasRepo(String authority) {
        boolean hasConfig = configCache.containsKey(authority);
        boolean hasRepo = repoCache.containsKey(authority);
        if (hasConfig != hasRepo) {
            // cache is corrupted, remove repo
            removeRepo(authority, false);
            return false;
        }
        return hasConfig;
    }

    public CONFIG getConfig(String authority) {
        reloadIfNotExist(authority, true);
        return configCache.get(authority);
    }

    public REPO getRepo(String authority) {
        reloadIfNotExist(authority, true);
        return repoCache.get(authority);
    }

    /**
     * This is called when a type has been changed anywhere in Rapture
     */
    @Override
    public void signalMessage(NotificationMessage message) {
        String authority = TypeChangeManager.getAuthorityFromMessage(message);
        removeRepo(authority, false);
    }

    protected void reloadIfNotExist(String authority, boolean autoloadIndex) {
        if (!hasRepo(authority)) {
            reloadRepo(authority, autoloadIndex);
        }
    }

    // reload config, store, and repo
    protected void reloadRepo(String authority, boolean autoloadIndex) {
        CONFIG config = reloadConfig(authority);
        if (config != null) {
            REPO repository = reloadRepository(config, autoloadIndex);
            if (repository != null) {
                addRepo(authority, config, repository);
            }
        }
    }

    /**
     * Remove repo from cache, and notify other Rapture instances
     *
     * @param authority
     * @param notify
     */
    protected void removeRepo(String authority, boolean notify) {
        configCache.remove(authority);
        repoCache.remove(authority);

        if (notify) {
            notifyChange(authority);
        }
    }

    private void notifyChange(String authority) {
        if (Kernel.getTypeChangeManager() != null) {
            Kernel.getTypeChangeManager().setTypeChanged(type, authority);
        }
    }

    public abstract CONFIG reloadConfig(String authority);

    public abstract REPO reloadRepository(CONFIG config, boolean autoloadIndex);

}
