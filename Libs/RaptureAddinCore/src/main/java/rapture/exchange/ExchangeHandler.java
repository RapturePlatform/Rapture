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
package rapture.exchange;

import java.util.Map;

import rapture.common.RapturePipelineTask;
import rapture.common.model.RaptureExchange;

/**
 * Interact with the general concept of something that can handle an exchange
 * 
 * @author amkimian
 * 
 */
public interface ExchangeHandler {
    void setConfig(Map<String, String> config);

    void setInstanceName(String instanceName);

    /**
     * Sets up the exchange, and all the queue bindings associated with it,
     * based on what is configured in the argument
     * 
     * @param exchange
     *            The {@link RaptureExchange} object that contains the
     *            config we want to set up
     */
    void setupExchange(RaptureExchange exchange);

    void tearDownExchange(RaptureExchange exchange);

    void putTaskOnExchange(String exchange, RapturePipelineTask task, String routingKey);

    /**
     * Register a {@link QueueHandler} for messages received by the specified
     * queue, and start consuming messages received by that queue. This is what
     * notifies the messaging system that we are a consumer and care about
     * messages on this queue.
     * 
     * @param exchange
     * @param queue
     * @param handler
     * @return
     */
    String startConsuming(String exchange, String queue, QueueHandler handler);

    void ensureExchangeUnAvailable(RaptureExchange exchangeConfig);

	void publishTopicMessage(String exchange, String topic, String message);

    long subscribeTopic(String exchange, String topic, TopicMessageHandler messageHandler);

	void unsubscribeTopic(long handle);
}
