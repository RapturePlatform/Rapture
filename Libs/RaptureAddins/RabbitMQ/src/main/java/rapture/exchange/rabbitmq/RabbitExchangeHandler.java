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
package rapture.exchange.rabbitmq;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import rapture.common.RapturePipelineTask;
import rapture.common.exception.ExceptionToString;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.model.RaptureExchange;
import rapture.common.model.RaptureExchangeQueue;
import rapture.common.pipeline.PipelineConstants;
import rapture.config.MultiValueConfigLoader;
import rapture.exchange.ExchangeHandler;
import rapture.exchange.QueueHandler;
import rapture.exchange.RPCMessage;
import rapture.exchange.TopicMessageHandler;
import rapture.util.IDGenerator;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.FlowListener;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ReturnListener;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;

public class RabbitExchangeHandler implements ExchangeHandler {
    private Connection connection;
    private Channel channel;
    private ExecutorService service = Executors.newCachedThreadPool();
    private static Logger logger = Logger.getLogger(RabbitExchangeHandler.class);
    private String instanceName = "default"; //$NON-NLS-1$
    private int messageCounter = 1;
    private String replyQueueName;
    private Map<String, String> queueNameRegistry = new ConcurrentHashMap<String, String>();
    private QueueingConsumer consumer;

    @Override
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    @Override
    public synchronized void setConfig(Map<String, String> config) {
        // Attempt to bind to RabbitMQ
        ConnectionFactory factory = new ConnectionFactory();
        logger.info(Messages.getString("RabbitExchangeHandler.config")); //$NON-NLS-1$
        try {
            String uri = MultiValueConfigLoader.getConfig("RABBITMQ-" //$NON-NLS-1$
                    + instanceName);
            if (uri == null || uri.isEmpty()) {
                uri = "amqp://guest:guest@localhost:5672/%2f"; //$NON-NLS-1$
            }
            factory.setUri(uri);
            factory.setAutomaticRecoveryEnabled(true);
            logger.debug(Messages
                    .getString("RabbitExchangeHandler.creatingChannel")); //$NON-NLS-1$
            connection = factory.newConnection();
            connection.addShutdownListener(new ShutdownListener() {
                @Override
                public void shutdownCompleted(ShutdownSignalException cause) {
                    // This should theoretically be called when we disconnect
                    // from RabbitMQ, but due to a bug in the client library it
                    // instead gets invoked
                    // when reconnecting. This may change in future versions of
                    // amqp-client so may need to revisit
                    logger.info("Reconnected to RabbitMQ");

                }
            });
            logger.debug(Messages
                    .getString("RabbitExchangeHandler.connectionMade")); //$NON-NLS-1$
            channel = connection.createChannel();
            channel.addShutdownListener(new ShutdownListener() {
                @Override
                public void shutdownCompleted(ShutdownSignalException cause) {
                    logger.info("Disconnected from RabbitMQ. Cause :"
                            + cause.getMessage());
                    logger.debug(ExceptionToString.format(cause));
                }
            });
            logger.debug(Messages
                    .getString("RabbitExchangeHandler.channelCreated")); //$NON-NLS-1$
            channel.basicQos(100);
            channel.addReturnListener(new ReturnListener() {

                @Override
                public void handleReturn(int replyCode, String replyText,
                        String exchange, String routingKey,
                        AMQP.BasicProperties properties, byte[] body)
                        throws IOException {
                    logger.debug(String.format(
                            Messages.getString("RabbitExchangeHandler.returnListener"), replyCode, //$NON-NLS-1$
                            replyText));
                }

            });
            channel.addFlowListener(new FlowListener() {

                @Override
                public void handleFlow(boolean active) throws IOException {
                    logger.debug(String.format(
                            Messages.getString("RabbitExchangeHandler.Flow"), active)); //$NON-NLS-1$
                }

            });

            replyQueueName = channel.queueDeclare().getQueue();
            logger.info("RPC reply queue is " + replyQueueName);
            consumer = new QueueingConsumer(channel);
            channel.basicConsume(replyQueueName, true, consumer);

        } catch (Exception e) {
            String message = Messages
                    .getString("RabbitExchangeHandler.noConnect");
            throw RaptureExceptionFactory.create(
                    HttpURLConnection.HTTP_INTERNAL_ERROR, message, e);
        }
    }

    private Set<String> exchangesTested = Collections
            .synchronizedSet(new HashSet<String>());

    @Override
    public void setupExchange(RaptureExchange exchange) {
        if (exchangesTested.contains(exchange.getName())) {
            if (exchange.getQueueBindings() != null) {
                for (RaptureExchangeQueue queue : exchange.getQueueBindings()) {
                    try {
                        bindQueue(exchange, queue);
                    } catch (IOException e) {
                        logger.error("Unable to bind "+queue.getName()+" to exchange "+exchange.getName());
                        logger.info(ExceptionToString.format(e));
                    }
                }
            }
            return;
        }

        logger.debug(String.format(
                Messages.getString("RabbitExchangeHandler.ensureAvail"), exchange.getName())); //$NON-NLS-1$
        try {
            logger.debug(String.format(
                    Messages.getString("RabbitExchangeHandler.ExchangeDeclare"), exchange.getName())); //$NON-NLS-1$
            logger.debug(String.format(
                    Messages.getString("RabbitExchangeHandler.exchangeType"), exchange.getExchangeType())); //$NON-NLS-1$
            channel.exchangeDeclare(exchange.getName(), exchange
                    .getExchangeType().name().toLowerCase(), true);
            // logger.info(ok.protocolClassId());
            // Now ensure that any declared queues are bound
            logger.debug(Messages
                    .getString("RabbitExchangeHandler.bindingQueues")); //$NON-NLS-1$
            if (exchange.getQueueBindings() != null) {
                for (RaptureExchangeQueue queue : exchange.getQueueBindings()) {
                    bindQueue(exchange, queue);
                }
            }
            exchangesTested.add(exchange.getName());
        } catch (IOException e) {
            String message = Messages
                    .getString("RabbitExchangeHandler.noExchange");
            try {
                if (channel.isOpen()) {
                    channel.close();
                }
            } catch (IOException e1) {
                logger.error(ExceptionToString.format(e1));
            }
            throw RaptureExceptionFactory.create(
                    HttpURLConnection.HTTP_INTERNAL_ERROR, message, e);
        } catch (Throwable t) {
            try {
                if (channel.isOpen()) {
                    channel.close();
                }
            } catch (IOException e1) {
                logger.error(ExceptionToString.format(e1));
            }
            String message = String
                    .format("Caught throwable during exchangeDeclare for exchange %s:\n%s",
                            exchange.getName(), ExceptionToString.format(t));
            logger.info(message);
            throw RaptureExceptionFactory.create(
                    HttpURLConnection.HTTP_INTERNAL_ERROR, message, t);
        }
    }

    private void bindQueue(RaptureExchange exchange, RaptureExchangeQueue queue)
            throws IOException {
        String underlying;
        if (isAnonymousQueue(queue.getName())) {
            if (queueNameRegistry.containsKey(queue.getName())) {
                underlying = queueNameRegistry.get(queue.getName());
            } else {
                underlying = channel.queueDeclare().getQueue();
                queueNameRegistry.put(queue.getName(), underlying);
            }
        } else {
            underlying = getUnderlyingQueueName(exchange.getName(),
                    queue.getName());
            logger.debug(String.format(
                    Messages.getString("RabbitExchangeHandler.underlyingQueue"), underlying)); //$NON-NLS-1$
            channel.queueDeclare(underlying, true, false, false, null);
        }

        if (queue.getRouteBindings().isEmpty()) {
            logger.debug(String.format(
                    Messages.getString("RabbitExchangeHandler.noRoute"), exchange.getName())); //$NON-NLS-1$
            channel.queueBind(underlying, exchange.getName(), ""); //$NON-NLS-1$
        } else {
            for (String bindingKey : queue.getRouteBindings()) {
                logger.debug(String.format(
                        Messages.getString("RabbitExchangeHandler.binding"), bindingKey, exchange.getName())); //$NON-NLS-1$
                channel.queueBind(underlying, exchange.getName(), bindingKey);
            }
        }
    }

    private boolean isAnonymousQueue(String queue) {
        return queue != null
                && queue.startsWith(PipelineConstants.ANONYMOUS_PREFIX);
    }

    /**
     * This method doens't really "bind" the queue to an exchange. The queue must be already bound beforehand. What happens here is we register a
     * {@link QueueHandler} for messages received by this queue
     */
    @Override
    public String startConsuming(String exchange, String queue,
            final QueueHandler handler) {
        boolean autoAck = Boolean.valueOf(MultiValueConfigLoader.getConfig(
                "RABBITMQ-autoAck", "false"));
        final String tag = IDGenerator.getUUID();
        try {
            String underlyingQueue;
            if (isAnonymousQueue(queue)) {
                // If anonymous queue, don't use the silly autogenerated
                // anonymous name. Instead, ask RabbitMQ to create one
                if (queueNameRegistry.containsKey(queue)) {
                    underlyingQueue = queueNameRegistry.get(queue);
                } else {
                    throw RaptureExceptionFactory
                            .create(String
                                    .format("Error! Cannot start consuming on undefined anonymous queue %s, on exchange %s",
                                            queue, exchange));
                }
            } else {
                underlyingQueue = getUnderlyingQueueName(exchange, queue);
            }
            logger.debug(String.format(
                    Messages.getString("RabbitExchangeHandler.startConsuming"), underlyingQueue)); //$NON-NLS-1$
            logger.debug(String.format(
                    Messages.getString("RabbitExchangeHandler.underly"), underlyingQueue)); //$NON-NLS-1$
            channel.basicConsume(underlyingQueue, autoAck, tag,
                    new MessageConsumer(channel, tag, handler, underlyingQueue));
        } catch (IOException e) {
            String message = String.format(Messages
                    .getString("RabbitExchangeHandler.noStartConsuming"), //$NON-NLS-1$
                    String.format("%s (%s)", queue, exchange));
            try {
                channel.close();
            } catch (IOException e1) {
                logger.error(ExceptionToString.format(e1));
            }
            throw RaptureExceptionFactory.create(
                    HttpURLConnection.HTTP_INTERNAL_ERROR, message, e);
        }
        return tag;
    }

    private String getUnderlyingQueueName(String exchange, String queue) {
        return exchange + "-" + queue;
    }

    @Override
    public void tearDownExchange(RaptureExchange exchange) {
        try {
            for (RaptureExchangeQueue queue : exchange.getQueueBindings()) {
                unbindQueue(exchange, queue);
            }
            channel.exchangeDelete(exchange.getName());
            // This will ensure it gets recreated later
            exchangesTested.remove(exchange.getName());
        } catch (IOException e) {
            String message = String.format(Messages
                    .getString("RabbitExchangeHandler.noDelete"));
            throw RaptureExceptionFactory.create(
                    HttpURLConnection.HTTP_INTERNAL_ERROR, message, e);
        }
    }

    private void unbindQueue(RaptureExchange exchange,
            RaptureExchangeQueue queue) throws IOException {
        String underlying = getUnderlyingQueueName(exchange.getName(),
                queue.getName());
        for (String routingKey : queue.getRouteBindings()) {
            channel.queueUnbind(underlying, exchange.getName(), routingKey);
        }
        channel.queueDelete(underlying);
    }

    @Override
    public void putTaskOnExchange(final String exchange,
            final RapturePipelineTask task, final String routingKey) {
        logger.debug(Messages.getString("RabbitExchangeHandler.puttingTask")); //$NON-NLS-1$
        Submitter s = new Submitter(exchange, task, routingKey);
        service.submit(s);
        RaptureException ex = s.getError();
        if (ex != null) throw ex;
    }

    private class Submitter implements Runnable {
        final String exchange;
        final RapturePipelineTask task;
        final String routingKey;
        private RaptureException error = null;

        public Submitter(final String exchange, final RapturePipelineTask task,
                final String routingKey) {
            this.exchange = exchange;
            this.task = task;
            this.routingKey = routingKey;
        }

        @Override
        public void run() {
            try {
                byte[] messageBody;
                messageBody = JacksonUtil.bytesJsonFromObject(task);

                // For now, set this to delivery mode 1 (non-persistent),
                // but
                // have this configurable later.

                messageCounter++;
                Integer deliveryMode = Integer.parseInt(MultiValueConfigLoader
                        .getConfig("RABBITMQ-deliveryMode", "1"));
                AMQP.BasicProperties props = new AMQP.BasicProperties()
                        .builder().contentType(task.getContentType())
                        .deliveryMode(deliveryMode)
                        .priority(task.getPriority())
                        .messageId("" + messageCounter).build(); //$NON-NLS-1$

                logger.debug(String.format(Messages
                        .getString("RabbitExchangeHandler.publishMessage"), //$NON-NLS-1$
                        exchange, routingKey, task.getContentType()));
                try {
                    channel.basicPublish(exchange, routingKey, props,
                            messageBody);
                    logger.debug(String.format(Messages
                            .getString("RabbitExchangeHandler.sendMessage"),
                            props.getMessageId(), task.getContent(), exchange,
                            routingKey));
                } catch (IOException e) {
                    String message = Messages
                            .getString("RabbitExchangeHandler.noPublish");
                    throw RaptureExceptionFactory.create(
                            HttpURLConnection.HTTP_INTERNAL_ERROR, message, e);
                }
            } catch (RaptureException e) {
                error = e;
            }
        }

        public RaptureException getError() {
            return error;
        }
    }

    @Override
    public String subscribeToExchange(String exchange,
            List<String> routingKeys, QueueHandler handler) {
        throw RaptureExceptionFactory.create(
                HttpURLConnection.HTTP_INTERNAL_ERROR,
                Messages.getString("RabbitExchangeHandler.notYetSupported")); //$NON-NLS-1$
    }

    @Override
    public void ensureExchangeUnAvailable(RaptureExchange exchangeConfig) {
        // This will ensure it gets recreated later
        exchangesTested.remove(exchangeConfig.getName());
    }

    @Override
    public Map<String, Object> makeRPC(String queueName, String fnName,
            Map<String, Object> params, long timeoutInSeconds) {
        // Send a message on a defined queue and wait for a response
        try {
            String corrId = java.util.UUID.randomUUID().toString();
            BasicProperties props = new BasicProperties.Builder()
                    .correlationId(corrId).replyTo(replyQueueName).build();
            // Construct message
            // A message contains the fnName and the map, as JSON
            RPCMessage messageObj = new RPCMessage();
            messageObj.setFnName(fnName);
            messageObj.setParams(params);
            String message = JacksonUtil.jsonFromObject(messageObj);
            logger.debug("Will make call on queue name " + queueName);
            logger.debug("Message is " + message);
            channel.basicPublish("", queueName, props,
                    message.getBytes("UTF-8"));

            // Now wait for a response, max timeout
            String response = null;
            while (true) {
                QueueingConsumer.Delivery delivery = consumer
                        .nextDelivery(timeoutInSeconds * 1000);
                if (delivery == null) {
                    throw RaptureExceptionFactory
                            .create("Timed out while waiting for response");
                }
                if (delivery.getProperties().getCorrelationId().equals(corrId)) {
                    response = new String(delivery.getBody());
                    break;
                }
            }
            Map<String, Object> ret = null;
            if (response != null) {
                ret = JacksonUtil.getMapFromJson(response);
            }
            return ret;
        } catch (IOException e) {
            throw RaptureExceptionFactory.create(
                    "Could not create reply rpc queue", e);
        } catch (ShutdownSignalException e) {
            throw RaptureExceptionFactory.create("Shutdown", e);
        } catch (ConsumerCancelledException e) {
            throw RaptureExceptionFactory.create("Cancelled", e);
        } catch (InterruptedException e) {
            throw RaptureExceptionFactory.create("Interrupted", e);

        }
    }

    @Override
    public void publishTopicMessage(String exchange, String topic,
            String message) {
        Publisher p = new Publisher(exchange, topic, message);
        service.submit(p);
    }

    private class Publisher implements Runnable {
        final String exchange;
        final String topic;
        final String message;

        public Publisher(final String exchange, final String topic,
                final String message) {
            this.exchange = exchange;
            this.topic = topic;
            this.message = message;
        }

        @Override
        public void run() {
            try {
                channel.basicPublish(exchange, topic, null, message.getBytes());
            } catch (IOException e) {
                throw RaptureExceptionFactory.create(
                        "Could not publish on topic queue", e);
            }
        }
    }

    private AtomicLong subscriptionHandler = new AtomicLong(0);

    private Map<Long, SubscriptionThread> subscriberMap = new HashMap<Long, SubscriptionThread>();

    @Override
    public long subscribeTopic(String exchange, String topic,
            TopicMessageHandler messageHandler) {

        SubscriptionThread t = new SubscriptionThread(exchange, topic, channel,
                messageHandler);
        long handleId = subscriptionHandler.incrementAndGet();
        subscriberMap.put(handleId, t);
        t.start();
        return handleId;
    }

    @Override
    public void unsubscribeTopic(long handle) {
        SubscriptionThread t = subscriberMap.remove(handle);
        if (t != null) {
            t.closeSubscription();
        }
    }

}
