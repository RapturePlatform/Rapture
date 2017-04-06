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
package rapture.pipeline2.memory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;

import rapture.common.QueueSubscriber;
import rapture.exchange.memory.MemoryTopic;
import rapture.pipeline.Pipeline2Handler;

public class MemoryPipeline2Handler implements Pipeline2Handler {

    String instanceName;
    private static Logger logger = Logger.getLogger(MemoryPipeline2Handler.class);
    Map<QueueSubscriber, Thread> activeSubscriptions = new HashMap<>();
    Map<String, ConcurrentLinkedQueue<String>> queueMap = new ConcurrentHashMap<>();

    @Override
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    @Override
    public synchronized void setConfig(Map<String, String> config) {
    }

    static Map<String, MemoryTopic> topicMap = new ConcurrentHashMap<>();

    MemoryTopic createTopic(String name) {
        MemoryTopic topic = topicMap.get(name);
        if (topic != null) return topic;
        topic = new MemoryTopic();
        topic.setTopicName(name);
        topicMap.put(name, topic);
        return topic;
    }


    @Override
    public void createPipeline(String queueIdentifier) {
        queueMap.put(queueIdentifier, new ConcurrentLinkedQueue<String>());
    }

    @Override
    public void subscribeToPipeline(String queueIdentifier, QueueSubscriber qMemorySubscriber) {
        MemoryTopic MemoryTopic = createTopic(queueIdentifier);
        ConcurrentLinkedQueue<String> queue = queueMap.get(queueIdentifier);

        Thread MemorySubscriber = new Thread() {
            @Override
            public void run() {
                logger.info("Running subscription thread for " + MemoryTopic.getTopicName());

                while (!interrupted()) {
                    String message = queue.poll();
                    if (message != null) {
                        qMemorySubscriber.handleEvent(queueIdentifier, message.getBytes());
                    }
                    synchronized (queue) {
                        try {
                            queue.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }
        };
        activeSubscriptions.put(qMemorySubscriber, MemorySubscriber);
        MemorySubscriber.start();
    }

    @Override
    public void publishTask(String queueIdentifier, String task) {
        MemoryTopic MemoryTopic = createTopic(queueIdentifier);
        ConcurrentLinkedQueue<String> queue = queueMap.get(queueIdentifier);
        synchronized (queue) {
            queue.add(task);
            queue.notifyAll();
        }
    }

    @Override
    public boolean queueExists(String queueIdentifier) {
        return queueMap.get(queueIdentifier) != null;
    }

    @Override
    public void unsubscribePipeline(String queueIdentifier, QueueSubscriber subscriber) {
        activeSubscriptions.remove(subscriber).interrupt();
    }

    @Override
    public void removePipeline(String queueIdentifier) {
        queueMap.remove(queueIdentifier);
    }
}
