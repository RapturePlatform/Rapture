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

    static Integer count1 = 0;
    static Integer count2 = 0;
    static Integer count3 = 0;
    static Integer count4 = 0;

    // In fan-out mode every subscriber gets all messages

    @Test
    public void testPubsubPipeline2HandlerFanOut() {

        try {
            count1 = 0;
            count2 = 0;
            count3 = 0;
            String toby = "A hazelnut in every byte";

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
                    synchronized (count1) {
                        System.out.println("Handler 1 called - count1 is now " + ++count1);
                        Assert.assertEquals(toby, new String(message));
                        if (count1 + count2 + count3 == 9) {
                            synchronized (waitForMe) {
                                System.out.println("Sum is now 9");
                                waitForMe.notify();
                            }
                        }
                    }
                    return true;
                }
            };
            pipe2Handler.subscribe(rp2.getName(), qs1);

            QueueSubscriber qs2 = new QueueSubscriber(rp2.getName(), rp2.getName() + "subscriber2") {
                @Override
                public boolean handleEvent(byte[] message) {
                    synchronized (count2) {
                        System.out.println("Handler 2 called - count2 is now " + ++count2);
                        Assert.assertEquals(toby, new String(message));
                        if (count1 + count2 + count3 == 9) {
                            synchronized (waitForMe) {
                                System.out.println("Sum is now 9");
                                waitForMe.notify();
                            }
                        }
                    }
                    return true;
                }
            };
            pipe2Handler.subscribe(rp2.getName(), qs2);

            QueueSubscriber qs3 = new QueueSubscriber(rp2.getName(), rp2.getName() + "subscriber3") {
                @Override
                public boolean handleEvent(byte[] message) {
                    synchronized (count3) {
                        System.out.println("Handler 3 called - count3 is now " + ++count3);
                        Assert.assertEquals(toby, new String(message));
                        if (count1 + count2 + count3 == 9) {
                            synchronized (waitForMe) {
                                System.out.println("Sum is now 9");
                                waitForMe.notify();
                            }
                        }
                    }
                    return true;
                }
            };
            pipe2Handler.subscribe(rp2.getName(), qs3);

            pipe2Handler.publishTask(rp2.getName(), toby);
            pipe2Handler.publishTask(rp2.getName(), toby);
            pipe2Handler.publishTask(rp2.getName(), toby);

            synchronized (waitForMe) {
                try {
                    waitForMe.wait(60000);
                } catch (InterruptedException e) {
                    Assert.fail(ExceptionToString.summary(e));
                }
            }

            Assert.assertEquals("Handler 1", 3, count1.intValue());
            Assert.assertEquals("Handler 2", 3, count2.intValue());
            Assert.assertEquals("Handler 3", 3, count3.intValue());

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

    // Handlers should co-exist with different Namespace values.
    // In this test we create two pairs of handlers. Each pair of handlers shares a namespace
    // We add a subscriber to each handler. For one pair the subscription names are the same,
    // so each message should only get handled once.
    // The other pair have different subscription IDs, so every message sent to that topic will be handled by both.

    // Messages sent to handler A or B should be received by subscribers to handler A or B
    // Messages sent to handler C or D should be received by subscribers to both handlers C and D

    @Test
    public void testPubsubPipeline2MultiTenancy() {

        try {
            count1 = 0;
            count2 = 0;
            count3 = 0;
            count4 = 0;
            String named = "duplicate";

            PubsubPipeline2Handler pipe2HandlerA = new PubsubPipeline2Handler();
            PubsubPipeline2Handler pipe2HandlerB = new PubsubPipeline2Handler();
            PubsubPipeline2Handler pipe2HandlerC = new PubsubPipeline2Handler();
            PubsubPipeline2Handler pipe2HandlerD = new PubsubPipeline2Handler();
            try {
                pipe2HandlerA.setConfig(ImmutableMap.of("namespace", "rapturetest1", "threads", "10"));
                pipe2HandlerB.setConfig(ImmutableMap.of("namespace", "rapturetest1", "threads", "10"));
                pipe2HandlerC.setConfig(ImmutableMap.of("namespace", "rapturetest2", "threads", "10"));
                pipe2HandlerD.setConfig(ImmutableMap.of("namespace", "rapturetest2", "threads", "10"));
            } catch (Exception e1) {
                // This can't run if the credentials aren't available.
                // rapture.common.exception.RaptureException: Cannot configure:
                // java.io.IOException: The Application Default Credentials are not available.
                // They are available if running in Google Compute Engine.
                // Otherwise, the environment variable GOOGLE_APPLICATION_CREDENTIALS must be defined pointing to a file defining the credentials.
                // See https://developers.google.com/accounts/docs/application-default-credentials for more information.
                Assume.assumeNoException(e1);
            }

            String flake = "Only the crumbliest, flakiest chocolate";
            String toblerone = "Triangular almonds from triangular trees";

            QueueSubscriber qs1 = new QueueSubscriber(named, named + "same") {
                @Override
                public boolean handleEvent(byte[] message) {
                    synchronized (count1) {
                        System.out.println("Handler 1 called - count1 is now " + ++count1);
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        Assert.assertEquals(flake, new String(message));
                        if (count1 + count2 + count3 + count4 == 6) {
                            synchronized (waitForMe) {
                                System.out.println("Sum is now 6");
                                waitForMe.notify();
                            }
                        }
                    }
                    return true;
                }
            };
            pipe2HandlerA.subscribe(named, qs1);

            QueueSubscriber qs2 = new QueueSubscriber(named, named + "same") {
                @Override
                public boolean handleEvent(byte[] message) {
                    synchronized (count2) {
                        System.out.println("Handler 2 called - count2 is now " + ++count2);
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        Assert.assertEquals(flake, new String(message));
                        if (count1 + count2 + count3 + count4 == 6) {
                            synchronized (waitForMe) {
                                System.out.println("Sum is now 6");
                                waitForMe.notify();
                            }
                        }
                    }
                    return true;
                }
            };
            pipe2HandlerB.subscribe(named, qs2);

            QueueSubscriber qs3 = new QueueSubscriber(named, named + "same") {
                @Override
                public boolean handleEvent(byte[] message) {
                    synchronized (count3) {
                        System.out.println("Handler 3 called - count3 is now " + ++count3);
                        Assert.assertEquals(toblerone, new String(message));
                        if (count1 + count2 + count3 + count4 == 6) {
                            synchronized (waitForMe) {
                                System.out.println("Sum is now 6");
                                waitForMe.notify();
                            }
                        }
                    }
                    return true;
                }
            };
            pipe2HandlerC.subscribe(named, qs3);

            QueueSubscriber qs4 = new QueueSubscriber(named, named + "different") {
                @Override
                public boolean handleEvent(byte[] message) {
                    synchronized (count4) {
                        System.out.println("Handler 4 called - count4 is now " + ++count4);
                        Assert.assertEquals(toblerone, new String(message));
                        if (count1 + count2 + count3 + count4 == 6) {
                            synchronized (waitForMe) {
                                System.out.println("Sum is now 6");
                                waitForMe.notify();
                            }
                        }
                    }
                    return true;
                }
            };
            pipe2HandlerD.subscribe(named, qs4);

            pipe2HandlerA.publishTask(named, flake);
            pipe2HandlerB.publishTask(named, flake);
            pipe2HandlerC.publishTask(named, toblerone);
            pipe2HandlerD.publishTask(named, toblerone);

            synchronized (waitForMe) {
                try {
                    waitForMe.wait(60000);
                } catch (InterruptedException e) {
                    Assert.fail(ExceptionToString.summary(e));
                }
            }

            // So we should expect to see the messages published to HandlerB
            Assert.assertEquals("Handlers 1+2", 2, count1.intValue() + count2.intValue());
            Assert.assertEquals("Handler 3", 2, count3.intValue());
            Assert.assertEquals("Handler 4", 2, count4.intValue());

            pipe2HandlerA.unsubscribe(qs1);
            pipe2HandlerB.unsubscribe(qs2);
            pipe2HandlerC.unsubscribe(qs3);
            pipe2HandlerD.unsubscribe(qs4);
            pipe2HandlerA.deleteTopic(named);
            pipe2HandlerC.deleteTopic(named);
            pipe2HandlerA.forceDeleteSubscription(qs1);
            // qs2 is same as qs1 so doesn't need to be deleted
            pipe2HandlerC.forceDeleteSubscription(qs3);
            pipe2HandlerD.forceDeleteSubscription(qs4);

        } catch (RaptureException e) {
            Throwable cause = e.getCause();
            if (cause.getMessage().contains("RESOURCE_EXHAUSTED")) {
                System.err.println(ExceptionToString.format(cause));
                Assume.assumeNoException(cause);
            }
        }
    }

}
