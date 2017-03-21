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
package rapture.exchange.gcp;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.google.api.gax.core.RpcFuture;
import com.google.api.gax.core.RpcFutureCallback;
import com.google.cloud.pubsub.spi.v1.Publisher;
import com.google.cloud.pubsub.spi.v1.PublisherClient;
import com.google.cloud.pubsub.spi.v1.PublisherSettings;
import com.google.cloud.pubsub.spi.v1.SubscriberClient;
import com.google.cloud.pubsub.spi.v1.SubscriberSettings;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.PullResponse;
import com.google.pubsub.v1.PushConfig;
import com.google.pubsub.v1.ReceivedMessage;
import com.google.pubsub.v1.Subscription;
import com.google.pubsub.v1.SubscriptionName;
import com.google.pubsub.v1.Topic;
import com.google.pubsub.v1.TopicName;

import rapture.common.RapturePipelineTask;
import rapture.common.exception.ExceptionToString;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.model.RaptureExchange;
import rapture.config.MultiValueConfigLoader;
import rapture.exchange.ExchangeHandler;
import rapture.exchange.QueueHandler;
import rapture.exchange.TopicMessageHandler;

public class PubsubExchangeHandler implements ExchangeHandler {

    private static Logger logger = Logger.getLogger(PubsubExchangeHandler.class);
    private String instanceName = "default"; //$NON-NLS-1$

    PublisherClient publisherClient = null;
    SubscriberClient subscriberClient = null;
    private String projectId;
    Map<String, String> config = null;

    Map<Long, Thread> activeSubscriptions = new HashMap<>();

    @Override
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    // Overrideable for testing
    PublisherSettings getPublisherSettingsFromConfig(Map<String, String> config) throws IOException {
        return PublisherSettings.defaultBuilder().build();
    }

    // Overrideable for testing
    SubscriberSettings getSubscriberSettingsFromConfig(Map<String, String> config) throws IOException {
        return SubscriberSettings.defaultBuilder().build();
    }

    @Override
    public synchronized void setConfig(Map<String, String> config) {
        this.config = config;
        projectId = StringUtils.trimToNull(config.get("projectid"));
        if (projectId == null) {
            projectId = MultiValueConfigLoader.getConfig("GOOGLE-projectid");
            if (projectId == null) {
                throw new RuntimeException("Project ID not set in RaptureGOOGLE.cfg or in config " + config);
            }
        }

        try {
            PublisherSettings publisherSettings = getPublisherSettingsFromConfig(config);
            publisherClient = PublisherClient.create(publisherSettings);
        } catch (IOException e) {
            String message = String.format("Cannot configure: %s", ExceptionToString.format(e));
            logger.error(message);
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, message, e);
        }
    }

    private Set<String> exchangesTested = Collections.synchronizedSet(new HashSet<String>());
    private Map<String, Topic> topicMap = new HashMap<>();

    private Topic getTopic(String topicId) {
        Topic topic = topicMap.get(topicId);
        if (topic != null) return topic;

        TopicName topicName = TopicName.create(projectId, topicId);

        try {
            topic = publisherClient.getTopic(topicName);
        } catch (Exception e) {
            topic = publisherClient.createTopic(topicName);
        }
        topicMap.put(topicId, topic);
        return topic;
    }

    @Override
    public void setupExchange(RaptureExchange exchange) {
        logger.warn("setupExchange not implemented: " + exchange.toString());
    }

    /**
     * This method doens't really "bind" the queue to an exchange. The queue must be already bound beforehand. What happens here is we register a
     * {@link QueueHandler} for messages received by this queue
     */
    @Override
    public String startConsuming(String exchange, String queue, final QueueHandler handler) {
        logger.error("startConsuming not implemented: Exchange " + exchange + " Queue " + queue);
        return null;
    }

    @Override
    public void tearDownExchange(RaptureExchange exchange) {
        logger.error("tearDownExchange not implemented: " + exchange.toString());
    }

    @Override
    public void putTaskOnExchange(final String exchange, final RapturePipelineTask task, final String routingKey) {
        logger.error("putTaskOnExchange not implemented: Exchange " + exchange + "Routing Key " + routingKey + " task " + task.toString());
    }

    @Override
    public String subscribeToExchange(String exchange, List<String> routingKeys, QueueHandler handler) {
        logger.error("subscribeToExchange Not yet implemented : Exchange " + exchange + " Routing Keys " + JacksonUtil.formattedJsonFromObject(routingKeys));
        return null;
    }

    @Override
    public void ensureExchangeUnAvailable(RaptureExchange exchangeConfig) {
        throw new RuntimeException("ensureExchangeUnAvailable not implemented: " + exchangeConfig.toString());
    }

    @Override
    public Map<String, Object> makeRPC(String queueName, String fnName, Map<String, Object> params, long timeoutInSeconds) {
        throw new RuntimeException("makeRPC Not yet implemented");
    }

    @Override
    public void publishTopicMessage(String exchange, String topicId, String message) {
        Topic topic = getTopic(topicId);
        ByteString data = ByteString.copyFromUtf8(message);
        TopicName topicName = topic.getNameAsTopicName();

        try {
            PubsubMessage psmessage = PubsubMessage.newBuilder().setData(data).build();
            Publisher publisher = Publisher.newBuilder(topicName).build();

            // RpcFuture is being renamed to ApiFuture in a later version of GAX
            RpcFuture<String> messageIdFuture = publisher.publish(psmessage);
            messageIdFuture.addCallback(new RpcFutureCallback<String>() {
                @Override
                public void onSuccess(String messageId) {
                    System.out.println("published with message id: " + messageId);
                }

                @Override
                public void onFailure(Throwable t) {
                    System.out.println("failed to publish: " + t);
                }
            });
        } catch (IOException e) {
            String error = String.format("Cannot send message to topic %s:\n%s", topicId, ExceptionToString.format(e));
            logger.error(error);
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, error, e);
        }
    }

    @Override
    public long subscribeTopic(String exchange, String topicId, TopicMessageHandler messageHandler) {
        Long handle = System.currentTimeMillis();
        try {
            Topic topic = getTopic(topicId);
            TopicName topicName = topic.getNameAsTopicName();
            if (subscriberClient == null) {
                SubscriberSettings subscriberSettings = getSubscriberSettingsFromConfig(config);
                subscriberClient = SubscriberClient.create(subscriberSettings);
            }
            String subscriptionId = "sub_" + handle;
            SubscriptionName subscriptionName = SubscriptionName.create(projectId, subscriptionId);
            Subscription subscription = subscriberClient.createSubscription(subscriptionName, topicName, PushConfig.getDefaultInstance(), 0);
            
            Thread subscriber = new Thread() {
                @Override
                public void run() {
                    logger.info("Running subscription thread for " + topicId);
                    
                    while (!interrupted()) {
                        // get one message only
                        PullResponse response = subscriberClient.pull(subscriptionName, false, 1);
                        List<ReceivedMessage> messasges = response.getReceivedMessagesList();
                        if (!messasges.isEmpty()) {
                            ReceivedMessage mess = messasges.get(0);
                            String data = mess.getMessage().getData().toStringUtf8();
                            messageHandler.deliverMessage(null, topicId, topicId, data);
                        }
                    }
                }
            };
            subscriber.start();

            activeSubscriptions.put(handle, subscriber);
        } catch (Exception e) {
            String error = String.format("Cannot subscribe to topic %s:\n%s", topicId, ExceptionToString.format(e));
            logger.error(error);
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, error, e);
        }
        return handle;
    }

    @Override
    public void unsubscribeTopic(long handle) {
        if (subscriberClient == null) return; // WTF?
        String subscriptionId = "sub_" + handle;
        SubscriptionName subscriptionName = SubscriptionName.create(projectId, subscriptionId);
        subscriberClient.deleteSubscription(subscriptionName);
        Thread bare = activeSubscriptions.remove(handle);
        bare.interrupt();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        Set<Long> handles = activeSubscriptions.keySet();
        for (Long handle : handles)
            unsubscribeTopic(handle);
        if (publisherClient != null) publisherClient.close();
        if (subscriberClient != null) subscriberClient.close();
    }
}
