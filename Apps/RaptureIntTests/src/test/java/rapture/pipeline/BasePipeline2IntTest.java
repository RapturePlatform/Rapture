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
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.PipelineTaskState;
import rapture.common.QueueSubscriber;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.TaskStatus;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.mime.MimeJarCacheUpdate;
import rapture.config.RaptureConfig;
import rapture.kernel.ContextFactory;
import rapture.kernel.Pipeline2ApiImpl;
import rapture.kernel.pipeline.JarCacheUpdateHandler;
import rapture.kernel.pipeline.TaskSubmitter;

public abstract class BasePipeline2IntTest {

    static Pipeline2ApiImpl papi;
    static CallingContext context = ContextFactory.getKernelUser();
    static RaptureConfig config;

    static long now = System.currentTimeMillis();

    private static final String queue = "Queue" + now;
    private static final String subscriber = "Subscriber" + now;
    private static final String message = "Message" + now;

    Boolean success = false;

    public void testSimpleTopicMessage() {
        success = false;
        String queueConfig = config.DefaultExchange;

        papi.createBroadcastQueue(context, queue, queueConfig);

        QueueSubscriber qs1 = new QueueSubscriber(queue, queue + "_" + subscriber + "_1") {
            @Override
            public boolean handleTask(String pipeline, TaskStatus task) {
                Assert.fail("Did not expect Task " + JacksonUtil.formattedJsonFromObject(task));
                return false;
            }

            @Override
            public boolean handleEvent(String pipeline, byte[] message) {
                if (success == true) Assert.fail("Handler called twice for a topic message");
                success = true;
                synchronized (this) {
                    this.notify();
                }
                return true;
            }
        };
        papi.subscribeToQueue(context, qs1);
        try {
            Thread.sleep(50);
        } catch (Exception e) {
        }

        synchronized (qs1) {
            papi.broadcastMessage(context, queue, message);
            try {
                qs1.wait(5000);
            } catch (Exception e) {
            }
        }
        assertTrue("Message handler was not called after 5s", success);
        papi.unsubscribeQueue(context, qs1);
    }

    @Test
    public void testTaskWithResponse() {
        success = false;
        String queueConfig = config.DefaultExchange;
        papi.createTaskQueue(context, queue, queueConfig);

        // Simple task consumer
        QueueSubscriber qs2 = new QueueSubscriber(queue, queue + "_" + subscriber + "_2") {
            @Override
            public boolean handleTask(String pipeline, TaskStatus status) {
                System.out.println("Got Task " + JacksonUtil.formattedJsonFromObject(status));
                status.addToOutput("Response");
                status.setCurrentState(PipelineTaskState.COMPLETED);
                papi.publishTaskResponse(context, queue, status);
                return true;
            }
        };
        papi.subscribeToQueue(context, qs2);
        TaskStatus status = papi.publishTask(context, queue, "Hello World", 15000L, null);
        Assert.assertNotNull(status);
        Assert.assertEquals(1, status.getOutput().size());
        Assert.assertEquals("Response", status.getOutput().get(0));
        Assert.assertEquals(PipelineTaskState.COMPLETED, status.getCurrentState());
        System.out.println(status);
        papi.unsubscribeQueue(context, qs2);
    }

    @Test
    public void testTaskWithTimeout() {
        success = false;
        String queueConfig = config.DefaultExchange;
        String timeoutQueue = this.queue + "timeout";
        papi.createTaskQueue(context, timeoutQueue, queueConfig);

        // Simple task consumer
        QueueSubscriber qs3 = new QueueSubscriber(timeoutQueue, timeoutQueue + "_" + subscriber + "_3") {
            @Override
            public boolean handleTask(String pipeline, TaskStatus status) {
                System.out.println("Got Task " + JacksonUtil.formattedJsonFromObject(status));
                try {
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                status.addToOutput("Response");
                status.setCurrentState(PipelineTaskState.COMPLETED);
                papi.publishTaskResponse(context, timeoutQueue, status);
                System.out.println("Completed " + status);
                return true;
            }
        };
        papi.subscribeToQueue(context, qs3);
        TaskStatus status = papi.publishTask(context, timeoutQueue, "Hello World", 2000L, null);

        // Should return task status after 2s

        Assert.assertNotNull(status);
        Assert.assertEquals(JacksonUtil.jsonFromObject(status.getOutput()), 0, status.getOutput().size());
        Assert.assertNotEquals(PipelineTaskState.COMPLETED, status.getCurrentState());

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // by now task should have completed
        status = papi.getStatus(context, status.getTaskId());
        System.out.println("Expected to be complete by now" + status);
        Assert.assertNotNull(status);
        Assert.assertEquals(1, status.getOutput().size());
        Assert.assertEquals("Response", status.getOutput().get(0));
        Assert.assertEquals(PipelineTaskState.COMPLETED, status.getCurrentState());
        papi.unsubscribeQueue(context, qs3);
        papi.removeTaskQueue(context, timeoutQueue);
    }

    /**
     * Try to simulate the case where there are two task handlers running on different machines
     */
    @Test
    public void testTaskWithTwoListeners() {
        // DANGER WILL ROBINSON
        // Since Kernel is a Singleton [enum] we have to cheat here and pass in a null.
        // Pipeline2ApiImpl
        Pipeline2ApiImpl papi2 = new Pipeline2ApiImpl(null);

        success = false;
        String queueConfig = config.DefaultExchange;

        // Two subscribers with the same ID - only one should handle each task
        QueueSubscriber qsA = new QueueSubscriber(queue, queue + "_" + subscriber) {
            @Override
            public boolean handleTask(String pipeline, TaskStatus status) {
                status.addToOutput("Response from Subscriber A");
                status.setCurrentState(PipelineTaskState.COMPLETED);
                papi.publishTaskResponse(context, queue, status);
                return true;
            }
        };

        QueueSubscriber qsB = new QueueSubscriber(queue, queue + "_" + subscriber) {
            @Override
            public boolean handleTask(String pipeline, TaskStatus status) {
                System.out.println("Got Task " + JacksonUtil.formattedJsonFromObject(status));
                status.addToOutput("Response from Subscriber B");
                status.setCurrentState(PipelineTaskState.COMPLETED);
                papi2.publishTaskResponse(context, queue, status);
                return true;
            }
        };
        papi2.createTaskQueue(context, queue, queueConfig);
        papi2.subscribeToQueue(context, qsB);

        papi.createTaskQueue(context, queue, queueConfig);
        papi.subscribeToQueue(context, qsA);

        TaskStatus status = papi.publishTask(context, queue, "Hello World", 15000L, null);
        TaskStatus status2 = papi.publishTask(context, queue, "Hello World2", 15000L, null);
        Assert.assertNotNull(status);
        Assert.assertEquals(1, status.getOutput().size());
        Assert.assertEquals(PipelineTaskState.COMPLETED, status.getCurrentState());
        System.out.println(status);

        Assert.assertNotNull(status2);
        Assert.assertEquals(1, status2.getOutput().size());
        Assert.assertEquals(PipelineTaskState.COMPLETED, status2.getCurrentState());
        System.out.println(status2);

        // Should have been processed by different subscribers
        Assert.assertNotEquals(status.getOutput().get(0), status2.getOutput().get(0));
        papi.unsubscribeQueue(context, qsA);
        papi2.unsubscribeQueue(context, qsB);
    }

    private static final String TEST_JAR_PATH = StringUtils.join(new String[] { "build", "classes", "test" }, File.separator);

    StringBuilder sb;

    // Not yet tied in
    // Test broadcast messages using the Jar MimeJarCacheUpdate
    public void testBroadcastMessage() throws IOException {
        success = false;
        Logger jcl = Logger.getLogger(JarCacheUpdateHandler.class);
        String uri = "jar://mytest/testjar1.jar";
        sb = new StringBuilder();
        final BasePipeline2IntTest monitor = this;

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

        MimeJarCacheUpdate mime = new MimeJarCacheUpdate();
        mime.setJarUri(new RaptureURI(uri, Scheme.JAR));
        mime.setDeletion(true);

        synchronized (app) {
            TaskSubmitter.submitBroadcastToAll(context, mime, MimeJarCacheUpdate.getMimeType(), null);
            // Kernel.getPipeline2().getTrusted().broadcastMessageToAll(context, task);
            try {
                app.wait(20000);
            } catch (Exception e) {
            }
        }
        assertTrue("log appender was not called after 20s", success);
        Assert.assertTrue(sb.toString().startsWith("Updating jar cache for jar uri [jar://mytest/testjar1.jar]"));
    }
}
