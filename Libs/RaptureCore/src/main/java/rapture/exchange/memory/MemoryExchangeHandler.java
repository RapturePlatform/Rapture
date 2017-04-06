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
package rapture.exchange.memory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.log4j.Logger;

import rapture.common.RapturePipelineTask;
import rapture.common.model.RaptureExchange;
import rapture.exchange.ExchangeHandler;
import rapture.exchange.QueueHandler;
import rapture.exchange.TopicMessageHandler;

/**
 * A trivial implementation of ExchangeHandler, primarily for single server use and for testing
 * 
 * @author amkimian
 * 
 */
public class MemoryExchangeHandler implements ExchangeHandler {
    private static Logger log = Logger.getLogger(MemoryExchangeHandler.class);
    private Map<String, ExchangeRouter> routerMap = new HashMap<>();
    private String instanceName = "default"; //$NON-NLS-1$

    Map<String, Map<String, LinkedList<String>>> exchanges = new HashMap<>();
    Map<String, Map<String, LinkedList<TopicMessageHandler>>> topicHandlers = new HashMap<>();

    @Override
    public void setConfig(Map<String, String> config) {
    }

    @Override
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    @Override
    public void setupExchange(RaptureExchange exchange) {
        log.info("Ensure exchange available: " + instanceName);
        ExchangeRouter exchangeRouter = routerMap.get(exchange.getName());
        if (exchangeRouter == null) {
            log.info("Setting up exchange " + exchange.getName());
            exchangeRouter = new ExchangeRouter(exchange);
            routerMap.put(exchange.getName(), exchangeRouter);
        }
        exchangeRouter.bindQueues(exchange);
    }

    @Override
    public void tearDownExchange(RaptureExchange exchange) {
        if (routerMap.containsKey(exchange.getName())) {
            routerMap.remove(exchange.getName());
        }
    }

    @Override
    public void putTaskOnExchange(String exchange, RapturePipelineTask task, String routingKey) {
        if (routerMap.containsKey(exchange)) {
            log.info("Putting task on exchange " + exchange);
            routerMap.get(exchange).putItemOnExchange(task, routingKey);
        } else {
            log.warn("No mapping for " + routingKey + " on " + exchange);
        }
    }

    @Override
    public String startConsuming(String exchange, String queue, QueueHandler handler) {
        log.info("Registering queue handler for " + exchange + "; " + queue);
        if (routerMap.containsKey(exchange)) {
            routerMap.get(exchange).registerQueueHandler(queue, handler);
            return "test";
        } else {
            log.error("Cannot register queue handler for " + exchange + "; " + queue);
        }
        return null;
    }

    @Override
    public void ensureExchangeUnAvailable(RaptureExchange exchangeConfig) {
        tearDownExchange(exchangeConfig);
    }

    @Override
    public void publishTopicMessage(String exchange, String topic, String message) {
        Map<String, LinkedList<String>> queues = exchanges.get(exchange);
        if (queues == null) {
            queues = new HashMap<>();
            exchanges.put(exchange, queues);
        }
        LinkedList<String> queue = queues.get(topic);
        if (queue == null) {
            queue = new LinkedList<>();
            queues.put(topic, queue);
        }
        queue.addLast(message);

        Map<String, LinkedList<TopicMessageHandler>> handlers = topicHandlers.get(exchange);
        if (handlers == null) return;

        while (!queue.isEmpty()) {
            try {
                String mess = queue.removeFirst();
                if (mess == null) return;
                for (TopicMessageHandler handler : handlers.get(topic))
                    handler.deliverMessage(exchange, topic, topic, mess);
            } catch (Exception e) {
                return;
            }
        }
    }

    @Override
    public long subscribeTopic(String exchange, String topic, TopicMessageHandler messageHandler) {
        Map<String, LinkedList<TopicMessageHandler>> handlers = topicHandlers.get(exchange);
        if (handlers == null) {
            handlers = new HashMap<>();
            topicHandlers.put(exchange, handlers);
        }
        LinkedList<TopicMessageHandler> handleList = handlers.get(topic);
        if (handleList == null) {
            handleList = new LinkedList<>();
            handlers.put(topic, handleList);
        }
        handleList.addLast(messageHandler);
        return 0L;
    }

    @Override
    public void unsubscribeTopic(long handle) {
        log.error("unsubscribe Topic not implemented");
    }

}
