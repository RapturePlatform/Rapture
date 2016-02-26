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

import org.apache.log4j.Logger;

import rapture.exchange.TopicMessageHandler;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;

public class SubscriptionThread extends Thread {
	private static Logger logger = Logger
			.getLogger(SubscriptionThread.class);
	private QueueingConsumer consumer;
	private String exchange;
	private String topic;
	private Channel channel;
	private TopicMessageHandler handler;
	private boolean closeMe = false;

	public SubscriptionThread(String exchange, String topic, Channel channel,
			TopicMessageHandler handler) {
		this.channel = channel;
		this.exchange = exchange;
		this.topic = topic;
		this.handler = handler;
	}

	public boolean closeSubscription() {
		closeMe = true;
		return true;
	}
	
	@Override
	public void run() {
		logger.info("Running subscription thread for " + exchange + "," + topic);
		String queueName;
		try {
			queueName = channel.queueDeclare().getQueue();
			logger.info("Queue is " + queueName);
			channel.queueBind(queueName, exchange, topic);

			QueueingConsumer consumer = new QueueingConsumer(channel);
			channel.basicConsume(queueName, true, consumer);

			while (true) {
				QueueingConsumer.Delivery delivery = consumer.nextDelivery(500);
				if (delivery != null) {
					String message = new String(delivery.getBody());
					String sentTopic = delivery.getEnvelope().getRoutingKey();

					try {
					handler.deliverMessage(exchange, topic, sentTopic, message);
					} catch(Exception e) {
						logger.info("Socket closed");
						closeMe = true;
					}
				}
				if (closeMe) {
					logger.info("Stopping subscription thread");
					break;
				}
			}
			
			channel.queueDelete(queueName);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ShutdownSignalException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ConsumerCancelledException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
