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
package rapture.pipeline;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Assert;

import rapture.common.CallingContext;
import rapture.common.RapturePipelineTask;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.mime.MimeJarCacheUpdate;
import rapture.exchange.TopicMessageHandler;
import rapture.kernel.Kernel;
import rapture.kernel.PipelineApiImpl;
import rapture.kernel.pipeline.JarCacheUpdateHandler;

public abstract class BasePipelineIntTest {

    static PipelineApiImpl papi;
    static CallingContext context;

    private static final String category = "alpha";
    private static final String domain = "main";
    private static final String exchange = "raptureTopic";
    private static final String topic = "hazelnut";
    private static final String message = "Message";

    Boolean success = false;

    public void testSimpleTopicMessage() {
        success = false;
        final BasePipelineIntTest monitor = this;

        TopicMessageHandler tmh = new TopicMessageHandler() {
            @Override
            public void deliverMessage(String exchange, String topic, String sentTopic, String message) {
                System.out.println("Got message " + message);
                success = true;
                synchronized (this) {
                    this.notify();
                }
            }
        };

        papi.subscribeTopic(domain, exchange, topic, tmh);
        try {
            Thread.sleep(500);
        } catch (Exception e) {
        }

        synchronized (tmh) {
            papi.publishTopicMessage(context, domain, exchange, topic, message);
            try {
                tmh.wait(5000);
            } catch (Exception e) {
            }
        }
        assertTrue("Message handler was not called after 15s", success);
    }

    private static final String TEST_JAR_PATH = StringUtils.join(new String[] { "build", "classes", "test" }, File.separator);

    StringBuilder sb;

    // Test broadcast messages using the Jar MimeJarCacheUpdate
    public void testBroadcastMessage() throws IOException {
        success = false;
        Logger jcl = Logger.getLogger(JarCacheUpdateHandler.class);
        String uri = "jar://mytest/testjar1.jar";
        sb = new StringBuilder();
        final BasePipelineIntTest monitor = this;

        Appender app = new AppenderSkeleton() {
            @Override
            public void close() {
            }

            @Override
            public boolean requiresLayout() {
                return false;
            }

            @Override
            protected void append(LoggingEvent event) {
                sb.append(event.getMessage());
                success = true;
                synchronized (this) {
                    this.notify();
                }
            }
        };
        jcl.addAppender(app);

        RapturePipelineTask task = new RapturePipelineTask();
        MimeJarCacheUpdate mime = new MimeJarCacheUpdate();
        mime.setJarUri(new RaptureURI(uri, Scheme.JAR));
        mime.setDeletion(true);
        task.setContentType(MimeJarCacheUpdate.getMimeType());
        task.addMimeObject(mime);

        synchronized (app) {
            Kernel.getPipeline().broadcastMessageToAll(context, task);
            try {
                app.wait(5000);
            } catch (Exception e) {
            }
        }
        assertTrue("log appender was not called after 5s", success);
        Assert.assertEquals("Updating jar cache for jar uri [jar://mytest/testjar1.jar]", sb.toString());
    }
}
