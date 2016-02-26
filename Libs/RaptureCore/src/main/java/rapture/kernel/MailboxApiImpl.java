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
package rapture.kernel;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import rapture.common.CallingContext;
import rapture.common.RaptureConstants;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.api.MailboxApi;
import rapture.common.exception.RaptureException;
import rapture.common.impl.jackson.JsonContent;
import rapture.common.model.RaptureMailMessage;
import rapture.common.model.RaptureMailMessageStorage;
import rapture.common.model.RepoConfig;
import rapture.common.model.RepoConfigStorage;
import rapture.repo.RepoVisitor;
import rapture.repo.Repository;

public class MailboxApiImpl extends KernelBase implements MailboxApi {
    private static final String IDGEN_ID = "//sys/mailbox";

    private static Logger log = Logger.getLogger(MailboxApiImpl.class);

    public MailboxApiImpl(Kernel raptureKernel) {
        super(raptureKernel);
    }

    @Override
    public List<RaptureMailMessage> getMessages(CallingContext context, String mailboxUri) {
        final RaptureURI internalUri = new RaptureURI(mailboxUri, Scheme.MAILBOX);
        String prefix = RaptureMailMessageStorage.addressToStorageLocation(internalUri).getDocPath();

        Repository repository = Kernel.getKernel().getRepo(RaptureConstants.MAIL_REPO);
        final List<RaptureMailMessage> ret = new ArrayList<RaptureMailMessage>();
        repository.visitAll(prefix, null, new RepoVisitor() {

            @Override
            public boolean visit(String name, JsonContent content, boolean isFolder) {
                if (!isFolder) {
                    log.info("Visiting " + name);
                    RaptureMailMessage msg;
                    try {
                        msg = RaptureMailMessageStorage.readFromJson(content);
                        ret.add(msg);
                    } catch (RaptureException e) {
                        log.error("Error getting message " + e.getMessage());
                    }
                }
                return true;
            }

        });
        return ret;
    }

    @Override
    public void moveMessage(CallingContext context, String messageUri, String newMessageUri) {

        // Moving a mailbox message involves loading it, deleting the original,
        // then changing the category,
        // then saving a new one.

        RaptureURI internalUri = new RaptureURI(messageUri, Scheme.MAILBOX);
        RaptureURI newInternalUri = new RaptureURI(newMessageUri, Scheme.MAILBOX);
        RaptureMailMessage msg = RaptureMailMessageStorage.readByAddress(internalUri);
        if (msg != null) {
            RaptureMailMessageStorage.deleteByAddress(internalUri, context.getUser(), "Move document");
            msg.setDocumentPath(newInternalUri.getDocPath());
            RaptureMailMessageStorage.add(msg, context.getUser(), "Move document");
        }
    }

    @Override
    public String putMessage(CallingContext context, String messageUri, String content) {
        // To post a mailbox message we need to get the mailbox repo and
        // idgen, and
        // use the latter to create a new id, create a new document with that
        // id, and move the document
        // into the mailbox repo. We also fire the event associated with
        // that category and authority

        RaptureURI internalUri = new RaptureURI(messageUri, Scheme.MAILBOX);
        String id = Kernel.getIdGen().nextIds(context, IDGEN_ID, 1L);
        RaptureMailMessage msg = new RaptureMailMessage();
        msg.setDocumentPath(internalUri.getDocPath());
        msg.setAuthority(internalUri.getAuthority());
        msg.setId(id);
        msg.setWhen(new Date());
        msg.setUser(context.getUser());
        RaptureMailMessageStorage.add(msg, context.getUser(), "New mail message");

        Kernel.getEvent().runEvent(context, "//" + internalUri.getAuthority() + "/mail/" + internalUri.getAuthority() + "/" + internalUri.getDocPath(),
                msg.getStoragePath(), "");
        return id;
    }

    @Override
    public void createMailboxRepo(CallingContext context, String mailboxConfig, String idGenConfig) {
        RepoConfig configRepo = new RepoConfig();
        configRepo.setName("RaptureMailbox");
        configRepo.setConfig(mailboxConfig);
        RepoConfigStorage.add(configRepo, context.getUser(), "Set mailbox repo");
        if (Kernel.getIdGen().idGenExists(context, IDGEN_ID)) {
            log.warn("Mailbox ID Generator "+IDGEN_ID+" already exists");
        } else
            Kernel.getIdGen().createIdGen(context, IDGEN_ID, idGenConfig);
    }
}
