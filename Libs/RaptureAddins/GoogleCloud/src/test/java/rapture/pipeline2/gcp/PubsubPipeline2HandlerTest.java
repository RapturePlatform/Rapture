package rapture.pipeline2.gcp;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import rapture.common.QueueSubscriber;
import rapture.common.exception.ExceptionToString;
import rapture.common.exception.RaptureException;
import rapture.common.model.RapturePipeline2;

public class PubsubPipeline2HandlerTest {

    int count1 = 0;
    int count2 = 0;
    int count3 = 0;

    // In fan-out mode every subscriber gets all messages

    @Test
    public void testPubsubPipeline2HandlerFanOut() {

        try {
            count1 = 0;
            count2 = 0;
            count3 = 0;

            RapturePipeline2 rp2 = new RapturePipeline2();
            rp2.setName("foo");

            PubsubPipeline2Handler pipe2Handler = new PubsubPipeline2Handler();
            try {
                pipe2Handler.setConfig(ImmutableMap.of("threads", "10"));
            } catch (Exception e1) {
                // This can't run if the credentials aren't available.
                // rapture.common.exception.RaptureException: Cannot configure:
                // java.io.IOException: The Application Default Credentials are not available.
                // They are available if running in Google Compute Engine.
                // Otherwise, the environment variable GOOGLE_APPLICATION_CREDENTIALS must be defined pointing to a file defining the credentials.
                // See https://developers.google.com/accounts/docs/application-default-credentials for more information.
                Assume.assumeNoException(e1);
            }

            QueueSubscriber qs1 = new QueueSubscriber(rp2.getName(), rp2.getName() + "subscriber1") {
                @Override
                public boolean handleEvent(byte[] message) {
                    System.out.println("Handler 1 called");
                    Assert.assertEquals("A hazelnut in every byte", new String(message));
                    count1++;
                    if (count1 + count2 + count3 == 9) {
                        synchronized (waitForMe) {
                            waitForMe.notify();
                        }
                    }
                    return true;
                }
            };
            pipe2Handler.subscribe(rp2.getName(), qs1);

            QueueSubscriber qs2 = new QueueSubscriber(rp2.getName(), rp2.getName() + "subscriber2") {
                @Override
                public boolean handleEvent(byte[] message) {
                    System.out.println("Handler 2 called");
                    Assert.assertEquals("A hazelnut in every byte", new String(message));
                    count2++;
                    if (count1 + count2 + count3 == 9) {
                        synchronized (waitForMe) {
                            waitForMe.notify();
                        }
                    }
                    return true;
                }
            };
            pipe2Handler.subscribe(rp2.getName(), qs2);

            QueueSubscriber qs3 = new QueueSubscriber(rp2.getName(), rp2.getName() + "subscriber3") {
                @Override
                public boolean handleEvent(byte[] message) {
                    System.out.println("Handler 3 called");
                    Assert.assertEquals("A hazelnut in every byte", new String(message));
                    count3++;
                    if (count1 + count2 + count3 == 9) {
                        synchronized (waitForMe) {
                            waitForMe.notify();
                        }
                    }
                    return true;
                }
            };
            pipe2Handler.subscribe(rp2.getName(), qs3);

            pipe2Handler.publishTask(rp2.getName(), "A hazelnut in every byte");
            pipe2Handler.publishTask(rp2.getName(), "A hazelnut in every byte");
            pipe2Handler.publishTask(rp2.getName(), "A hazelnut in every byte");

            synchronized (waitForMe) {
                try {
                    waitForMe.wait(60000);
                } catch (InterruptedException e) {
                    Assert.fail(ExceptionToString.summary(e));
                }
            }

            Assert.assertEquals("Handler 1", 3, count1);
            Assert.assertEquals("Handler 2", 3, count2);
            Assert.assertEquals("Handler 3", 3, count3);

            pipe2Handler.unsubscribe(qs1);
            pipe2Handler.unsubscribe(qs2);
            pipe2Handler.unsubscribe(qs3);
            pipe2Handler.deleteTopic(rp2.getName());
            pipe2Handler.forceDeleteSubscription(qs1);
            pipe2Handler.forceDeleteSubscription(qs2);
            pipe2Handler.forceDeleteSubscription(qs3);

        } catch (RaptureException e) {
            Throwable cause = e.getCause();
            if (cause.getMessage().contains("RESOURCE_EXHAUSTED")) {
                System.err.println(ExceptionToString.format(cause));
                Assume.assumeNoException(cause);
            }
        }
    }

    // In fan-in mode only one person should get each message

    Object waitForMe = new Object();

    @Test
    public void testPubsubPipeline2HandlerFanIn() {

        try {
            count1 = 0;
            count2 = 0;
            count3 = 0;

            RapturePipeline2 rp2 = new RapturePipeline2();
            rp2.setName("MilkyWay");

            PubsubPipeline2Handler pipe2Handler1 = new PubsubPipeline2Handler();
            try {
                pipe2Handler1.setConfig(ImmutableMap.of("threads", "10"));
            } catch (Exception e1) {
                // This can't run if the credentials aren't available.
                // rapture.common.exception.RaptureException: Cannot configure:
                // java.io.IOException: The Application Default Credentials are not available.
                // They are available if running in Google Compute Engine.
                // Otherwise, the environment variable GOOGLE_APPLICATION_CREDENTIALS must be defined pointing to a file defining the credentials.
                // See https://developers.google.com/accounts/docs/application-default-credentials for more information.
                Assume.assumeNoException(e1);
            }

            QueueSubscriber qs1 = new QueueSubscriber(rp2.getName(), rp2.getName() + "subscriber") {
                @Override
                public boolean handleEvent(byte[] message) {
                    System.out.println("Handler 1 got " + new String(message));
                    count1++;
                    if (count1 + count2 + count3 == 3) {
                        synchronized (waitForMe) {
                            waitForMe.notify();
                        }
                    }
                    return true;
                }
            };
            pipe2Handler1.subscribe(rp2.getName(), qs1);

            QueueSubscriber qs2 = new QueueSubscriber(rp2.getName(), rp2.getName() + "subscriber") {
                @Override
                public boolean handleEvent(byte[] message) {
                    System.out.println("Handler 2 got " + new String(message));
                    count2++;
                    if (count1 + count2 + count3 == 3) {
                        synchronized (waitForMe) {
                            waitForMe.notify();
                        }
                    }
                    return true;
                }
            };
            pipe2Handler1.subscribe(rp2.getName(), qs2);

            QueueSubscriber qs3 = new QueueSubscriber(rp2.getName(), rp2.getName() + "subscriber") {
                @Override
                public boolean handleEvent(byte[] message) {
                    System.out.println("Handler 3 got " + new String(message));
                    count3++;
                    if (count1 + count2 + count3 == 3) {
                        synchronized (waitForMe) {
                            waitForMe.notify();
                        }
                    }
                    return true;
                }
            };
            pipe2Handler1.subscribe(rp2.getName(), qs3);

            PubsubPipeline2Handler pipe2Handler = new PubsubPipeline2Handler();
            pipe2Handler.setConfig(ImmutableMap.of("threads", "10"));

            pipe2Handler.publishTask(rp2.getName(), "Marathon");
            pipe2Handler.publishTask(rp2.getName(), "Snickers");
            pipe2Handler.publishTask(rp2.getName(), "Mars Bar");

            synchronized (waitForMe) {
                try {
                    waitForMe.wait(60000);
                } catch (InterruptedException e) {
                    Assert.fail(ExceptionToString.summary(e));
                }
            }

            pipe2Handler.unsubscribe(qs1);
            pipe2Handler.unsubscribe(qs2);
            pipe2Handler.unsubscribe(qs3);

            pipe2Handler.deleteTopic(rp2.getName());
            pipe2Handler.forceDeleteSubscription(qs1); // They all have the same name
            Assert.assertEquals(3, count1 + count2 + count3);
        } catch (RaptureException e) {
            Throwable cause = e.getCause();
            if (cause.getMessage().contains("RESOURCE_EXHAUSTED")) {
                System.err.println(ExceptionToString.format(cause));
                Assume.assumeNoException(cause);
            }
        }
    }
}
