package rapture.pipeline2.gcp;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import rapture.common.QueueSubscriber;
import rapture.common.model.RapturePipeline2;
import rapture.pipeline2.gcp.PubsubPipeline2Handler;

public class PubsubPipeline2HandlerTest {

    int count1 = 0;
    int count2 = 0;
    int count3 = 0;

    // In fan-out mode every subscriber gets all messages

    @Test
    public void testPubsubPipeline2HandlerFanOut() {

        count1 = 0;
        count2 = 0;
        count3 = 0;

        RapturePipeline2 rp2 = new RapturePipeline2();
        rp2.setName("hazelnut" + System.currentTimeMillis());

        PubsubPipeline2Handler pipe2Handler = new PubsubPipeline2Handler();
        pipe2Handler.setConfig(ImmutableMap.of("projectid", "todo3-incap"));

        QueueSubscriber qs1 = new QueueSubscriber(rp2.getName(), rp2.getName() + "subscriber1") {
            @Override
            public boolean handleEvent(String queue, byte[] message) {
                System.out.println("Handler 1 called");
                Assert.assertEquals("A hazelnut in every byte", new String(message));
                count1++;
                return false;
            }
        };
        pipe2Handler.subscribeToPipeline(rp2.getName(), qs1);

        QueueSubscriber qs2 = new QueueSubscriber(rp2.getName(), rp2.getName() + "subscriber2") {
            @Override
            public boolean handleEvent(String queue, byte[] message) {
                System.out.println("Handler 2 called");
                Assert.assertEquals("A hazelnut in every byte", new String(message));
                count2++;
                return false;
            }
        };
        pipe2Handler.subscribeToPipeline(rp2.getName(), qs2);

        QueueSubscriber qs3 = new QueueSubscriber(rp2.getName(), rp2.getName() + "subscriber3") {
            @Override
            public boolean handleEvent(String queue, byte[] message) {
                System.out.println("Handler 3 called");
                Assert.assertEquals("A hazelnut in every byte", new String(message));
                count3++;
                return false;
            }
        };
        pipe2Handler.subscribeToPipeline(rp2.getName(), qs3);

        pipe2Handler.publishTask(rp2.getName(), "A hazelnut in every byte");
        pipe2Handler.publishTask(rp2.getName(), "A hazelnut in every byte");
        pipe2Handler.publishTask(rp2.getName(), "A hazelnut in every byte");
        // pipe2Handler.publishTopicMessage(null, "hazelnut", "A hazelnut in every byte");
        //
        // PubsubPipeline2Handler pipe2Handler2 = new PubsubPipeline2Handler();
        // pipe2Handler2.setConfig(ImmutableMap.of("projectid", "todo3-incap"));
        //
        // pipe2Handler2.publishTopicMessage(null, "hazelnut", "A hazelnut in every byte");

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
        }

        Assert.assertEquals(9, count1 + count2 + count3);
        Assert.assertEquals(3, count1);
        Assert.assertEquals(3, count2);
        Assert.assertEquals(3, count3);

        pipe2Handler.unsubscribe(qs1);
        pipe2Handler.unsubscribe(qs2);
        pipe2Handler.unsubscribe(qs3);
        pipe2Handler.deleteTopic(rp2.getName());
    }

    // In fan-in mode only one person should get each message

    // @Test
    // public void testPubsubPipeline2HandlerFanIn() {
    //
    // count1 = 0;
    // count2 = 0;
    // count3 = 0;
    //
    // PubsubPipeline2Handler pipe2Handler1 = new PubsubPipeline2Handler();
    // pipe2Handler1.setConfig(ImmutableMap.of("projectid", "todo3-incap"));
    // long h1 = pipe2Handler1.subscribeTopic("snack", "MilkyWay", new TopicMessageHandler() {
    // @Override
    // public void deliverMessage(String exchange, String topic, String sentTopic, String message) {
    // System.out.println("Handler 1 got " + message);
    // count1++;
    // }
    // });
    //
    // PubsubPipeline2Handler pipe2Handler2 = new PubsubPipeline2Handler();
    // pipe2Handler2.setConfig(ImmutableMap.of("projectid", "todo3-incap"));
    // long h2 = pipe2Handler2.subscribeTopic("snack", "MilkyWay", new TopicMessageHandler() {
    // @Override
    // public void deliverMessage(String exchange, String topic, String sentTopic, String message) {
    // System.out.println("Handler 2 got " + message);
    // count2++;
    // }
    // });
    //
    // PubsubPipeline2Handler pipe2Handler3 = new PubsubPipeline2Handler();
    // pipe2Handler3.setConfig(ImmutableMap.of("projectid", "todo3-incap"));
    // long h3 = pipe2Handler3.subscribeTopic("snack", "MilkyWay", new TopicMessageHandler() {
    // @Override
    // public void deliverMessage(String exchange, String topic, String sentTopic, String message) {
    // System.out.println("Handler 3 got " + message);
    // count3++;
    // }
    // });
    //
    // PubsubPipeline2Handler pipe2Handler = new PubsubPipeline2Handler();
    // pipe2Handler.setConfig(ImmutableMap.of("projectid", "todo3-incap"));
    //
    // pipe2Handler.publishTopicMessage(null, "MilkyWay", "Marathon");
    // pipe2Handler.publishTopicMessage(null, "MilkyWay", "Snickers");
    // pipe2Handler.publishTopicMessage(null, "MilkyWay", "Mars Bar");
    // try {
    // Thread.sleep(5000);
    // } catch (InterruptedException e) {
    // }
    //
    // Assert.assertEquals(3, count1 + count2 + count3);
    //
    // pipe2Handler.deleteTopic("MilkyWay");
    // }
}
