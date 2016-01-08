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

import rapture.common.Scheme;
import rapture.common.exception.ExceptionToString;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.model.DocumentRepoConfig;
import rapture.common.model.DocumentRepoConfigStorage;
import rapture.common.model.IndexConfig;
import rapture.common.model.IndexConfigStorage;
import rapture.common.model.RaptureDocConfig;
import rapture.dsl.idef.IndexDefinition;
import rapture.dsl.idef.IndexDefinitionFactory;
import rapture.index.IndexProducer;
import rapture.kernel.ContextFactory;
import rapture.kernel.cache.config.DocRepoConfigFactory;
import rapture.repo.RepoFactory;
import rapture.repo.Repository;

import java.util.Collections;

import org.antlr.runtime.RecognitionException;
import org.apache.log4j.Logger;

import rapture.config.ConfigLoader;

/**
 * @author yanwang
 * @since 6/23/14.
 */
public class DocRepoCache extends AbstractStorableRepoCache<DocumentRepoConfig> {

    private static final Logger log = Logger.getLogger(AbstractRepoCache.class);

    public DocRepoCache() {
        super(Scheme.DOCUMENT.toString());
    }

    @Override
    public DocumentRepoConfig reloadConfig(String authority) {
        DocumentRepoConfig config = DocumentRepoConfigStorage.readByFields(authority);
        if (config != null) {
            return config;
        }
        try {
            return createRepoConfigFromTemplate(authority);
        } catch (RecognitionException e) {
            log.error(ExceptionToString.format(e));
            return null;
        }
    }

    @Override
    public Repository reloadRepository(DocumentRepoConfig documentRepoConfig, boolean autoloadIndex) {
        Repository repository = RepoFactory.getRepo(documentRepoConfig.getDocumentRepo().getConfig());
        if (repository != null && autoloadIndex) {
            // Is there an index with the same name? If there is,
            // attempt to load that and set it in the repo
            IndexConfig iConfig = IndexConfigStorage.readByFields(documentRepoConfig.getAuthority());
            if (iConfig != null) {
                IndexDefinition definition = IndexDefinitionFactory.getDefinition(iConfig.getConfig());
                IndexProducer producer = new IndexProducer(Collections.singletonList(definition));
                repository.setIndexProducer(producer);
            }
        }
        return repository;
    }

    private DocumentRepoConfig createRepoConfigFromTemplate(String authority) throws RecognitionException {

        if (ConfigLoader.getConf().AllowAutoRepoCreation) {
            String config = loadConfigFromTemplate(authority);
            RaptureDocConfig docConfig = new RaptureDocConfig();
            docConfig.setConfig(config);
            docConfig.setAuthority(authority);

            log.debug("Creating new Repository for authority " + authority + " using Config " + docConfig.getConfig());
            DocumentRepoConfig repoConfig = new DocumentRepoConfig();
            repoConfig.setAuthority(authority);
            repoConfig.setDocumentRepo(docConfig);
            DocumentRepoConfigStorage.add(repoConfig, ContextFactory.getKernelUser().getUser(), "Automatically created repo");
            return repoConfig;
        } else
            throw RaptureExceptionFactory.create("Repository for authority " + authority + " not defined and automatic creation not allowed");
    }

    public static void main(String[] args) throws RecognitionException {
        DocRepoCache c = new DocRepoCache();
        String config = c.loadConfigFromTemplate("");
        log.info(String.format("config is [%s]", config));
    }

    private String loadConfigFromTemplate(String authority) throws RecognitionException {
        String standardTemplate = ConfigLoader.getConf().StandardTemplate;
        return new DocRepoConfigFactory().createConfig(standardTemplate, authority);
    }
}
