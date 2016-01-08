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
package rapture.kernel.mailbox;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.model.RaptureMailMessage;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

public class MailboxTest {
    @Test
    public void generalMailboxTest() {
        CallingContext ctx = ContextFactory.getKernelUser();
        Kernel.initBootstrap();
        Kernel.getMailbox().createMailboxRepo(ctx, "VREP {} using MEMORY {}", "FOUNTAIN { base=\"26\", length=\"8\", prefix=\"MSG\" } USING MEMORY {}");
        Kernel.getMailbox().putMessage(ctx, "//test/orderPosting", "This would be an order");
        Kernel.getMailbox().putMessage(ctx, "//test/orderPosting", "This would be an order2");
        List<RaptureMailMessage> msgs = Kernel.getMailbox().getMessages(ctx, "//test/orderPosting");
        for (RaptureMailMessage msg : msgs) {
            System.out.println("Found message " + msg.getId());
            Kernel.getMailbox().moveMessage(ctx, "//test/orderPosting/" + msg.getId(), "//test/orderDone");
        }
        assertEquals("MSG00000001",msgs.get(0).getId());
        assertEquals("MSG00000002",msgs.get(1).getId());
    }
}
