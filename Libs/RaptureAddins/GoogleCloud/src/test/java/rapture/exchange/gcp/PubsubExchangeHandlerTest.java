package rapture.exchange.gcp;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import rapture.exchange.TopicMessageHandler;

public class PubsubExchangeHandlerTest {

    int count = 0;

    @Test
    public void testPubsubExchangeHandler() {
        PubsubExchangeHandler peh = new PubsubExchangeHandler();
        peh.setConfig(ImmutableMap.of("projectid", "high-plating-157918"));
        peh.subscribeTopic("", "hazelnut", new TopicMessageHandler() {
            @Override
            public void deliverMessage(String exchange, String topic, String sentTopic, String message) {
                Assert.assertEquals("A hazelnut in every byte", message);
                count++;
            }
        });

        peh.publishTopicMessage(null, "hazelnut", "A hazelnut in every byte");
        peh.publishTopicMessage(null, "hazelnut", "A hazelnut in every byte");
        peh.publishTopicMessage(null, "hazelnut", "A hazelnut in every byte");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
        }
        Assert.assertEquals(3, count);

    }
}
