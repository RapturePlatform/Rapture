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
package rapture.pipeline2.gcp;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.google.api.gax.core.RpcFuture;
import com.google.api.gax.core.RpcFutureCallback;
import com.google.cloud.pubsub.spi.v1.Publisher;
import com.google.cloud.pubsub.spi.v1.PublisherClient;
import com.google.cloud.pubsub.spi.v1.PublisherSettings;
import com.google.cloud.pubsub.spi.v1.SubscriberClient;
import com.google.cloud.pubsub.spi.v1.SubscriberSettings;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.PullResponse;
import com.google.pubsub.v1.PushConfig;
import com.google.pubsub.v1.ReceivedMessage;
import com.google.pubsub.v1.Subscription;
import com.google.pubsub.v1.SubscriptionName;
import com.google.pubsub.v1.Topic;
import com.google.pubsub.v1.TopicName;

import rapture.common.QueueSubscriber;
import rapture.common.TaskStatus;
import rapture.common.exception.ExceptionToString;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.config.MultiValueConfigLoader;
import rapture.pipeline.Pipeline2Handler;

public class PubsubPipeline2Handler implements Pipeline2Handler {

    static Logger logger = Logger.getLogger(PubsubPipeline2Handler.class);
    private String instanceName = "default"; //$NON-NLS-1$

    PublisherClient publisherClient = null;
    SubscriberClient subscriberClient = null;
    private String projectId;
    Map<String, String> config = null;

    Map<QueueSubscriber, Thread> activeSubscriptions = new ConcurrentHashMap<>();

    // Clean up for unit testing
    static List<PubsubPipeline2Handler> handlers = new CopyOnWriteArrayList<>();

    public PubsubPipeline2Handler() {
        handlers.add(this);
    }

    // Run between unit tests
    public static void cleanUp() {
        for (PubsubPipeline2Handler handler : handlers) {
            handler.close();
        }
    }

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

    private Topic getTopic(String topicId) {
        TopicName topicName = TopicName.create(projectId, topicId);
        try {
            return publisherClient.getTopic(topicName);
        } catch (Exception e) {
            return publisherClient.createTopic(topicName);
        }
    }

    public void unsubscribe(QueueSubscriber qsubscriber) {
        if (subscriberClient == null) return; // WTF?
        SubscriptionName subscriptionName = SubscriptionName.create(projectId, qsubscriber.getSubscriberId());
        subscriberClient.deleteSubscription(subscriptionName);
    }

    // Public for testing
    public void deleteTopic(String topicId) {
        if (subscriberClient == null) return; // WTF?
        TopicName topicName = TopicName.create(projectId, topicId);
        try {
            publisherClient.deleteTopic(topicName);
        } catch (Exception e) {
            String error = String.format("Cannot delete topic %s:\n%s", topicId, ExceptionToString.format(e));
            logger.error(error);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    public void close() {
        Set<QueueSubscriber> subs = activeSubscriptions.keySet();
        for (QueueSubscriber subscriber : subs) {
            unsubscribe(subscriber);
            activeSubscriptions.remove(subscriber);
        }
        if (publisherClient != null) {
            try {
                publisherClient.close();
            } catch (Exception e) {
            }
            publisherClient = null;
        }
        if (subscriberClient != null) {
            try {
                subscriberClient.close();
            } catch (Exception e) {
            }
            subscriberClient = null;
        }
    }

    @Override
    public void createPipeline(String queueIdentifier) {
        Topic topic = getTopic(queueIdentifier);
    }

    static Map<String, String> subMap = new ConcurrentHashMap<>();

    @Override
    public void subscribeToPipeline(String queueIdentifier, final QueueSubscriber qsubscriber) {
        logger.debug("Subscribing to " + queueIdentifier + " as " + qsubscriber.getSubscriberId());
        if (StringUtils.stripToNull(queueIdentifier) == null) {
            throw new RuntimeException("Null topic");
        }

        if (subMap.containsKey(queueIdentifier)) {
            logger.info("Queue already has a subscriber");
        }
        subMap.put(queueIdentifier, qsubscriber.getSubscriberId());

        Topic topic = getTopic(queueIdentifier);
        try {
            TopicName topicName = topic.getNameAsTopicName();
            if (subscriberClient == null) {
                SubscriberSettings subscriberSettings = getSubscriberSettingsFromConfig(config);
                subscriberClient = SubscriberClient.create(subscriberSettings);
            }
            SubscriptionName subscriptionName = SubscriptionName.create(projectId, qsubscriber.getSubscriberId());
            try {
                @SuppressWarnings("unused")
                Subscription subscription = subscriberClient.createSubscription(subscriptionName, topicName, PushConfig.getDefaultInstance(), 0);
            } catch (Exception e) {
            }

            Thread subscriber = new Thread() {
                @Override
                public void run() {
                    logger.info("Running subscription thread for " + topic.getName());

                    while (!interrupted()) {
                        // get one message only
                        PullResponse response = subscriberClient.pull(subscriptionName, false, 1);
                        List<ReceivedMessage> messasges = response.getReceivedMessagesList();
                        if (!messasges.isEmpty()) {
                            ReceivedMessage rcvmsg = messasges.get(0);
                            PubsubMessage mess = rcvmsg.getMessage();
                            logger.info(subscriptionName.getSubscription() + " reading Message " + mess.getMessageId());
                            if (!this.isInterrupted()) {
                                // Should we ack before we deliver or after?
                                subscriberClient.acknowledge(subscriptionName, ImmutableList.of(rcvmsg.getAckId()));
                                byte[] info = mess.getData().toByteArray();
                                try {
                                    TaskStatus taskStatus = JacksonUtil.objectFromJson(info, TaskStatus.class);
                                    qsubscriber.handleTask(queueIdentifier, taskStatus);
                                } catch (Exception e) {
                                    qsubscriber.handleEvent(queueIdentifier, info);
                                }
                            }
                        }
                    }
                    activeSubscriptions.remove(qsubscriber);
                }
            };
            subscriber.start();

            activeSubscriptions.put(qsubscriber, subscriber);
        } catch (Exception e) {
            String error = String.format("Cannot subscribe to topic %s:\n%s", topic.getName(), ExceptionToString.format(e));
            logger.error(error);
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, error, e);
        }
    }

    @Override
    public void unsubscribePipeline(String queueIdentifier, QueueSubscriber qsubscriber) {
        logger.debug("Unsubscribing from " + queueIdentifier + " as " + qsubscriber.getSubscriberId());
        Thread thread = activeSubscriptions.remove(qsubscriber);
        if (thread != null) thread.interrupt();
        SubscriptionName subscriptionName = SubscriptionName.create(projectId, qsubscriber.getSubscriberId());
        try {
            subscriberClient.deleteSubscription(subscriptionName);
        } catch (Exception e) {
            // If there are multiple subscribers sharing the same ID it may have already have been deleted.
        }
        if (subMap.get(queueIdentifier).equals(qsubscriber.getSubscriberId())) {
            subMap.remove(queueIdentifier);
        }
    }

    @Override
    public void publishTask(final String queue, final String task) {
        Topic topic = getTopic(queue);
        ByteString data = ByteString.copyFromUtf8(task);
        TopicName topicName = topic.getNameAsTopicName();


        try {
            PubsubMessage psmessage = PubsubMessage.newBuilder().setData(data).build();
            Publisher publisher = Publisher.newBuilder(topicName).build();

            // RpcFuture is being renamed to ApiFuture in a later version of GAX
            RpcFuture<String> messageIdFuture = publisher.publish(psmessage);
            messageIdFuture.addCallback(new RpcFutureCallback<String>() {
                @Override
                public void onSuccess(String messageId) {
                    logger.trace("published " + task + "\n to queue " + queue + " with message id: " + messageId);
                }

                @Override
                public void onFailure(Throwable t) {
                    logger.warn("failed to publish: " + t);
                }
            });
        } catch (IOException e) {
            String error = String.format("Cannot send message to topic %s:\n%s", topic.getName(), ExceptionToString.format(e));
            logger.error(error);
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, error, e);
        }

    }

    @Override
    public boolean queueExists(String queueIdentifier) {
        TopicName topicName = TopicName.create(projectId, queueIdentifier);
        try {
            publisherClient.getTopic(topicName);
            return true;
        } catch (Exception e) {
            return false;
        }

    }

    @Override
    public void removePipeline(String queueIdentifier) {
        TopicName topicName = TopicName.create(projectId, queueIdentifier);
        publisherClient.deleteTopic(topicName);
    }
}
