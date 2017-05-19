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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.google.api.core.ApiFuture;
import com.google.api.gax.grpc.ApiException;
import com.google.api.gax.grpc.ExecutorProvider;
import com.google.api.gax.grpc.InstantiatingExecutorProvider;
import com.google.cloud.pubsub.spi.v1.AckReplyConsumer;
import com.google.cloud.pubsub.spi.v1.MessageReceiver;
import com.google.cloud.pubsub.spi.v1.Publisher;
import com.google.cloud.pubsub.spi.v1.Publisher.Builder;
import com.google.cloud.pubsub.spi.v1.Subscriber;
import com.google.cloud.pubsub.spi.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.spi.v1.TopicAdminClient;
import com.google.cloud.pubsub.spi.v1.TopicAdminSettings;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.PushConfig;
import com.google.pubsub.v1.Subscription;
import com.google.pubsub.v1.SubscriptionName;
import com.google.pubsub.v1.Topic;
import com.google.pubsub.v1.TopicName;

import rapture.common.QueueSubscriber;
import rapture.common.exception.ExceptionToString;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.config.MultiValueConfigLoader;
import rapture.pipeline.Pipeline2Handler;

public class PubsubPipeline2Handler implements Pipeline2Handler {

    static Logger logger = Logger.getLogger(PubsubPipeline2Handler.class);
    private String projectId;
    private String namespace = "rapture";
    Map<String, String> config = null;

    static Map<QueueSubscriber, Subscriber> subscribers = new ConcurrentHashMap<>();
    static Map<SubscriptionName, Subscription> subscriptions = new ConcurrentHashMap<>();

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

    ExecutorProvider executor = null;

    @Override
    public synchronized void setConfig(Map<String, String> config) {
        projectId = StringUtils.trimToNull(config.get("projectid"));
        if (projectId == null) {
            projectId = MultiValueConfigLoader.getConfig("GOOGLE-projectid");
            if (projectId == null) {
                throw new RuntimeException("Project ID not set in RaptureGOOGLE.cfg or in config " + config);
            }
        }

        String ns = StringUtils.stripToNull(config.get("namespace"));
        if (ns == null) {
            ns = MultiValueConfigLoader.getConfig("GOOGLE-namespace");
        }
        if (ns != null) namespace = ns.toLowerCase();

        String threads = config.get("threads");
        if (threads != null) {
            executor = InstantiatingExecutorProvider.newBuilder().setExecutorThreadCount(Integer.parseInt(threads)).build();
        }

        // for debugging - it should not be used again
        this.config = new HashMap<>(config);
        this.config.put("projectid", projectId);
        this.config.put("namespace", namespace);

    }

    static Map<String, Topic> topics = new ConcurrentHashMap<>();

    private TopicAdminClient topicAdminClientCreate() throws IOException {
        TopicAdminSettings.Builder builder = TopicAdminSettings.defaultBuilder();
        // The default executor provider creates an insane number of threads.
        if (executor != null) builder.setExecutorProvider(executor);
        return TopicAdminClient.create(builder.build());
    }
    
    public Topic getTopic(String topicId) {
        if (topicId == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Illegal Argument: topic Id is null");
        }
        Exception cause = null;
        // prepend namespace
        String topId = namespace + topicId.toLowerCase();
        Topic topic = topics.get(topId);
        while (topic == null) {
            try (TopicAdminClient topicAdminClient = topicAdminClientCreate()) {
                TopicName topicName = TopicName.create(projectId, topId);
                try {
                    topic = topicAdminClient.getTopic(topicName);
                } catch (Exception e) {
                    // We could not get the topic. Presumably it doesn't exist. Can we create it?
                    cause = e;
                    if (topic == null) {
                        topic = topicAdminClient.createTopic(topicName);
                        topics.put(topId, topic);
                    }
                }
            } catch (Exception ioe) {
                if (ioe instanceof ApiException) {
                    if (ioe.getMessage().contains("ALREADY_EXISTS")) {
                        // We could not get it, so we tried to create it and got ALREADY_EXISTS.
                        // But if it already exists, why couldn't we get it? The CAUSE exception is of more interest to us here.
                        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR,
                                "Cannot get topic " + topicId + " using configuration " + JacksonUtil.jsonFromObject(config), cause);
                    }
                    if (ioe.getMessage().contains("RESOURCE_EXHAUSTED")) {
                        // This happens sometimes in unit testing because the cache doesn't persist between test cases.
                        // Should never happen in regular use.
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                        }
                        continue;
                    }
                }
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR,
                        "Cannot create or get topic " + topicId + " using configuration " + JacksonUtil.jsonFromObject(config), ioe);
            }
        }
        return topic;
    }

    public void deleteTopic(String topicId) {
        if (topicId == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Illegal Argument: topic Id is null");
        }
        // prepend namespace
        String topId = namespace + topicId.toLowerCase();
        Topic topic = topics.get(topId);
        if (topic != null) {
            try (TopicAdminClient topicAdminClient = topicAdminClientCreate()) {
                TopicName topicName = TopicName.create(projectId, topId);
                topicAdminClient.deleteTopic(topicName);
            } catch (Exception ioe) {
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Cannot delete topic " + topicId, ioe);
            }
        }
        topics.remove(topicId);
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    public void close() {
        Set<QueueSubscriber> subs = subscribers.keySet();
        for (QueueSubscriber subscriber : subs) {
            unsubscribe(subscriber);
        }
    }

    @Override
    public void createQueue(String queueIdentifier) {
        Topic topic = getTopic(queueIdentifier);
    }

    @Override
    public void subscribe(String queueIdentifier, final QueueSubscriber qsubscriber) {
        if (StringUtils.stripToNull(queueIdentifier) == null) {
            throw new RuntimeException("Null topic");
        }

        SubscriptionName subscriptionName = SubscriptionName.create(projectId, namespace + qsubscriber.getSubscriberId());
        Topic topic = getTopic(queueIdentifier);
        Subscription subscription = subscriptions.get(subscriptionName);
        if (subscription == null) {
            try (SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create()) {
                try {
                    subscription = subscriptionAdminClient.getSubscription(subscriptionName);
                } catch (Exception e) {
                    if (subscription == null) {
                        subscription = subscriptionAdminClient.createSubscription(subscriptionName, topic.getNameAsTopicName(), PushConfig.getDefaultInstance(),
                                0);
                        subscriptions.put(subscriptionName, subscription);
                    }
                }
            } catch (Exception ioe) {
                System.err.println(ExceptionToString.format(ioe));
            }
        }
        try {
            MessageReceiver receiver = new MessageReceiver() {
                @Override
                public void receiveMessage(PubsubMessage message, AckReplyConsumer consumer) {
                    System.out.println("Received " + message.getData().toStringUtf8());
                    if (qsubscriber.handleEvent(message.getData().toByteArray())) consumer.ack();
                }
            };

            Subscriber.Builder builder = Subscriber.defaultBuilder(subscriptionName, receiver);
            // The default executor provider creates an insane number of threads.
            if (executor != null) builder.setExecutorProvider(executor);
            Subscriber subscriber = builder.build();

            subscriber.addListener(new Subscriber.Listener() {
                @Override
                public void failed(Subscriber.State from, Throwable failure) {
                    // Subscriber encountered a fatal error and is shutting down.
                    System.err.println(failure);
                }
            }, MoreExecutors.directExecutor());
            subscriber.startAsync().awaitRunning();
            subscribers.put(qsubscriber, subscriber);
        } catch (Exception e) {
            String error = String.format("Cannot subscribe to topic %s:\n%s", topic.getName(), ExceptionToString.format(e));
            logger.error(error);
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, error, e);
        }
        logger.debug("Subscribed to " + queueIdentifier + " as " + qsubscriber.getSubscriberId());

    }

    @Override
    public void unsubscribe(QueueSubscriber qsubscriber) {
        logger.debug("Unsubscribing " + qsubscriber.getSubscriberId());
        Subscriber subscriber = subscribers.remove(qsubscriber);
        if (subscriber != null) subscriber.stopAsync();
    }

    public void forceDeleteSubscription(QueueSubscriber qsubscriber) {
        SubscriptionName subscriptionName = SubscriptionName.create(projectId, namespace + qsubscriber.getSubscriberId());
        try (SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create()) {
            subscriptionAdminClient.deleteSubscription(subscriptionName);
        } catch (Exception ioe) {
            System.err.println(ExceptionToString.format(ioe));
        }
    }

    private static Map<TopicName, Publisher> randomHouse = new ConcurrentHashMap<>();

    static final Runnable listener = new Runnable() {
        @Override
        public void run() {
            System.out.println("Message published");
        }
    };

    @Override
    public void publishTask(final String queue, final String task) {
        Topic topic = getTopic(queue);
        ByteString data = ByteString.copyFromUtf8(task);
        TopicName topicName = topic.getNameAsTopicName();

        try {
            PubsubMessage psmessage = PubsubMessage.newBuilder().setData(data).build();
            Publisher publisher = randomHouse.get(topicName);
            if (publisher == null) {
                logger.trace("No publisher found for " + topicName + " - creating");
                Builder builder = Publisher.defaultBuilder(topicName);
                // The default executor provider creates an insane number of threads.
                if (executor != null) builder.setExecutorProvider(executor);
                publisher = builder.build();
                randomHouse.put(topicName, publisher);
            } else {
                logger.trace("Existing publisher found for " + topicName);
            }

            ApiFuture<String> messageIdFuture = publisher.publish(psmessage);

            if (executor != null) messageIdFuture.addListener(listener, executor.getExecutor());

        } catch (IOException e) {
            String error = String.format("Cannot send message to topic %s:\n%s", topic.getName(), ExceptionToString.format(e));
            logger.error(error);
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, error, e);
        }
    }
}
