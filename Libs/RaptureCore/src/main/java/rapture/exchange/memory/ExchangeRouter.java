/**
 * The MIT License (MIT)
 *
 * Copyright (C) 2011-2016 Incapture Technologies LLC
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import rapture.common.RapturePipelineTask;
import rapture.common.model.RaptureExchange;
import rapture.common.model.RaptureExchangeQueue;
import rapture.exchange.QueueHandler;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class ExchangeRouter {
    private static Logger log = Logger.getLogger(ExchangeRouter.class);

    private String exchangeName;
    private Map<String, List<QueueHandler>> queueToHandler = new HashMap<String, List<QueueHandler>>();
    private Map<String, String> routingKeyToQueueName = new HashMap<String, String>();
    private ExecutorService service;

    public ExchangeRouter(RaptureExchange exchange) {
        log.debug(exchange.debug());
        this.exchangeName = exchange.getName();
        service = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("ExchangeRouter-" + exchange.getName() + "-%d").build());

    }

    public void bindQueues(RaptureExchange exchange) {
        if (exchange.getQueueBindings() != null) {
            for (RaptureExchangeQueue queue : exchange.getQueueBindings()) {
                bindQueue(exchange, queue);
            }
        }
    }
    
    private void bindQueue(RaptureExchange exchange, RaptureExchangeQueue queue) {
        if (queue.getRouteBindings().isEmpty()) {
            routingKeyToQueueName.put("", queue.getName());
        } else {
            for (String bindingKey : queue.getRouteBindings()) {
                log.info("Binding "+bindingKey+ " to "+queue.getName());
                routingKeyToQueueName.put(bindingKey, queue.getName());
            }
        }
    }

    public void registerQueueHandler(String queueName, QueueHandler handler) {
        log.info("Registering queue handler");
        List<QueueHandler> currHandlers = queueToHandler.get(queueName);
        if (currHandlers == null) {
            currHandlers = new LinkedList<QueueHandler>();
            queueToHandler.put(queueName, currHandlers);
        }
        currHandlers.add(handler);
    }

    public void deregisterQueueHandler(String queueName, QueueHandler handler) {
        if (queueToHandler.containsKey(queueName)) {
            queueToHandler.get(queueName).remove(handler);
        }
    }

    public void putItemOnExchange(final RapturePipelineTask task, final String routingKey) {
        // We don't do any real routing, we push the message onto one handler at
        // each queue

        service.execute(new Runnable() {

            @Override
            public void run() {
                log.info("Routing message");
                String queueName;
                if (routingKey == null || routingKey.length() == 0) {
                    queueName = routingKeyToQueueName.get("");
                } else {
                    queueName = routingKeyToQueueName.get(routingKey);
                }
                if (queueName == null) {
                    log.info(String.format("NULL queueName for routingKey=%s, exchange=%s", routingKey, exchangeName));
                } else {
                    List<QueueHandler> currHandlers = queueToHandler.get(queueName);
                    if (currHandlers == null) {
                        log.info(String.format("NULL handlers for queueName=%s routingKey=%s, exchange=%s", queueName, routingKey, exchangeName));

                    } else {
                        boolean handled = false;
                        for (QueueHandler handler : currHandlers) {
                            // Loop until someone takes it
                            handled = handleMessage(task, handler);
                            if (handled) break;
                        }
                        if (!handled)
                            log.info(String.format("Task not handled for queueName=%s routingKey=%s, exchange=%s", queueName, routingKey, exchangeName));
                    }
                }
            }
        });
    }

    private boolean handleMessage(final RapturePipelineTask task, QueueHandler handler) {
        return handler.handleMessage("test", "test", task.getContentType(), task);
    }
}
