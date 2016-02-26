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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import rapture.common.RapturePipelineTask;
import rapture.common.exception.ExceptionToString;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.exchange.QueueHandler;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import rapture.config.MultiValueConfigLoader;

public class MessageConsumer extends DefaultConsumer {
    private static Logger log = Logger.getLogger(MessageConsumer.class);
    private QueueHandler handler;
    private String tag;
    private ExecutorService service;

    public MessageConsumer(Channel channel, String tag, QueueHandler handler, String queueName) {
        super(channel);
        this.tag = tag;
        this.handler = handler;
        String threadPoolSizeString = MultiValueConfigLoader.getConfig("RUNNER-rabbitMQConsumerPoolSize", "50");
        int threadPoolSize = Integer.parseInt(threadPoolSizeString);
        service = Executors.newFixedThreadPool(threadPoolSize, new ThreadFactoryBuilder().setNameFormat("MessageConsumer-" + queueName + "-%d").build());
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
        final String routingKey = envelope.getRoutingKey();
        final String contentType = properties.getContentType();
        final long deliveryTag = envelope.getDeliveryTag();
        log.debug(String.format(Messages.getString("RabbitExchangeHandler.receivedMessage"), deliveryTag)); //$NON-NLS-1$							
        final RapturePipelineTask task = JacksonUtil.objectFromJson(new String(body), RapturePipelineTask.class);
        log.debug(Messages.getString("RabbitExchangeHandler.PlacingTask")); //$NON-NLS-1$
        try {
            service.execute(new Runnable() {

                @Override
                public void run() {
                    boolean ret;
                    try {
                        ret = handler.handleMessage(tag, routingKey, contentType, task);
                        log.debug(String.format(Messages.getString("RabbitExchangeHandler.acknowledgeMessage"), ret)); //$NON-NLS-1$
                    } catch (Exception e) {
                        log.error(String.format(Messages.getString("RabbitExchangeHandler.noAcknowledge"), ExceptionToString.format(e)));
                    } finally {
                        try {
                            getChannel().basicAck(deliveryTag, false);
                        } catch (IOException e) {
                            log.error(String.format("Error while acknowledging message with tag '%s':\n%s", deliveryTag, ExceptionToString.format(e)));
                        }
                    }
                    log.debug(Messages.getString("RabbitExchangeHandler.deliveryHandled")); //$NON-NLS-1$
                }

            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
