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
package rapture.kernel;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import rapture.common.CallingContext;
import rapture.common.ExchangeDomain;
import rapture.common.ExchangeDomainStorage;
import rapture.common.PipelineTaskState;
import rapture.common.QueueSubscriber;
import rapture.common.RapturePipelineTask;
import rapture.common.RaptureTransferObject;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.TaskStatus;
import rapture.common.api.Pipeline2Api;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.model.RaptureExchange;
import rapture.config.ConfigLoader;
import rapture.kernel.pipeline.ExchangeConfigFactory;
import rapture.pipeline.Pipeline2Factory;
import rapture.pipeline.Pipeline2Handler;

/**
 * Simplified non-Rabbit-centric version of the pipeline API
 *
 * @author qqqq
 */
public class Pipeline2ApiImpl extends KernelBase implements Pipeline2Api {

    Map<String, TaskStatus> currentTasks = new ConcurrentHashMap<>();
    Map<String, List<QueueSubscriber>> watchmen = new ConcurrentHashMap<>();

    // This flag will be set if the DefaultExchange config value starts with PIPELINE
    public static boolean usePipeline2 = false;

    // I don't believe that we currently have a unique Rapture cluster ID.
    // Individual servers have IDs but the cluster as a whole is just called 'Rapture'
    // This needs to be configured. as multiple Rapture instances can use the same namespace (GCP project ID)
    public static final String BROADCAST = "TBD";

    class TaskWatcher extends QueueSubscriber {
        public TaskWatcher(String queueName, String subscriberId) {
            super(queueName, subscriberId);
        }

        @Override
        public boolean handleEvent(byte[] data) {
            log.error("Received event " + new String(data) + " - expoected Task");
            return false;
        }

        @Override
        public boolean handleTask(TaskStatus update) {
            System.err.println("TASK WATCHER " + update);

            if (update == null) return false;
            String taskId = update.getTaskId();
            PipelineTaskState state = update.getCurrentState();
            if (state.equals(PipelineTaskState.DELETED)) {
                currentTasks.remove(taskId).notifyAll();
                return true;
            }
            TaskStatus status = currentTasks.get(update.getTaskId());
            if (status != null) {
                synchronized (status) {
                    System.err.println("TASK WATCHER found existing task");

                    if (update.getLastUpdateTime().after(status.getLastUpdateTime())) {
                        status.getOutput().addAll(update.getOutput());
                        status.setLastUpdateTime(update.getLastUpdateTime());
                        status.setCurrentState(state);
                        if (state.finished()) {
                            status.notifyAll();
                        }
                    }
                }
            } else {
                System.err.println("TASK WATCHER did not find existing task");
                currentTasks.put(update.getTaskId(), update);
            }
            return state.finished();
        }
    }

    // FOR TESTING PURPOSES ONLY
    @Override
    public void resetRepo() {
        if (getKernel() != null) super.resetRepo();
    }

    // There needs to be a common ID shared by members of a Rapture cluster since instances can share a GCP Project ID
    public Pipeline2ApiImpl(Kernel raptureKernel) {
        super(raptureKernel);
        usePipeline2 = (ConfigLoader.getConf().DefaultExchange.startsWith("PIPELINE"));
        System.out.println("Default Exchange is " + ConfigLoader.getConf().DefaultExchange);
    }

    @Override
    public String createBroadcastQueue(CallingContext context, String queueIdentifier, String queueConfig) {
        Pipeline2Handler handler = null;
        if (queueConfig != null) handler = Pipeline2Factory.getHandler(queueIdentifier, queueConfig);
        if (handler == null) {
            RaptureExchange exchangeConfig = ExchangeConfigFactory.createStandardDirect(queueIdentifier);
            ExchangeDomain domain = getExchangeDomain(exchangeConfig.getDomain());
            if (domain != null) {
                handler = Pipeline2Factory.getHandler(domain.getName(), domain.getConfig());
                pipelineHandlers.put(exchangeConfig.getDomain(), handler);
            } else throw new RuntimeException("Cannot set up " + queueIdentifier + " - no valid configuration defined");
        }
        handler.createQueue(queueIdentifier);
        pipelineHandlers.put(queueIdentifier, handler);
        return null; // make this void?
    }

    @Override
    public String createTaskQueue(CallingContext context, String queueIdentifier, String queueConfig) {
        Pipeline2Handler handler = Pipeline2Factory.getHandler(queueIdentifier, queueConfig);
        handler.createQueue(queueIdentifier);
        pipelineHandlers.put(queueIdentifier, handler);
        String responseIdentifier = queueIdentifier + "-response";
        handler.createQueue(responseIdentifier);
        pipelineHandlers.put(responseIdentifier, handler);
        return null; // make this void?
    }

    @Override
    public String createQueueIdentifier(CallingContext context, Map<String, String> queueValues) {
        return JacksonUtil.jsonFromObject(queueValues);
    }

    @Override
    public Boolean queueExists(CallingContext context, String queueIdentifier) {
        Pipeline2Handler handler = getHandler(queueIdentifier);
        if (handler == null) return false;
        throw new RuntimeException("TODO DO NOT CHECK THIS IN");
        // return handler.queueExists(queueIdentifier);
    }

    @Override
    public void removeBroadcastQueue(CallingContext context, String queueIdentifier) {
        Pipeline2Handler handler = getHandler(queueIdentifier);
        throw new RuntimeException("TODO DO NOT CHECK THIS IN");
        // handler.removePipeline(queueIdentifier);
    }

    @Override
    public void removeTaskQueue(CallingContext context, String queueIdentifier) {
        Pipeline2Handler handler = getHandler(queueIdentifier);
        throw new RuntimeException("TODO DO NOT CHECK THIS IN");
        // handler.removePipeline(queueIdentifier);
        // handler.removePipeline(queueIdentifier + "-response");
    }

    @Override
    public void subscribeToQueue(CallingContext context, QueueSubscriber subscriber) {
        Pipeline2Handler handler = getHandler(subscriber.getQueueName());
        if (handler == null) {
            throw new RuntimeException("Queue does not exist");
        }
        handler.subscribe(subscriber.getQueueName(), subscriber);
    }

    @Override
    public void unsubscribeQueue(CallingContext context, QueueSubscriber subscriber) {
        Pipeline2Handler handler = getHandler(subscriber.getQueueName());
        if (handler == null) {
            log.error("Queue does not exist");
        } else handler.unsubscribe(subscriber);
    }

    private static Map<String, Pipeline2Handler> pipelineHandlers = new ConcurrentHashMap<>();

    public Pipeline2Handler getHandler(String queueIdentifier) {
        Pipeline2Handler handler = pipelineHandlers.get(queueIdentifier);
        if (handler == null) {
            handler = setupHandler(queueIdentifier);
        }
        return handler;
    }

    private synchronized Pipeline2Handler setupHandler(String name) {
        log.info("Getting domain for " + name);
        RaptureURI addressURI = new RaptureURI("//" + name, Scheme.EXCHANGE_DOMAIN);
        ExchangeDomain domain = ExchangeDomainStorage.readByAddress(addressURI);
        if (domain != null) {
            Pipeline2Handler handler = Pipeline2Factory.getHandler(name, domain.getConfig());
            pipelineHandlers.put(name, handler);
            return handler;
        }
        return null;
    }

    @Override
    public Boolean broadcastTask(CallingContext context, String queueIdentifier, RaptureTransferObject task) {
        return broadcastMessage(context, queueIdentifier, JacksonUtil.jsonFromObject(task));
    }

    @Override
    public Boolean broadcastMessage(CallingContext context, String queueIdentifier, String message) {
        Pipeline2Handler handler = getHandler(queueIdentifier);
        if (handler == null) {
            RaptureExchange exchangeConfig = ExchangeConfigFactory.createStandardDirect(queueIdentifier);
            ExchangeDomain domain = getExchangeDomain(exchangeConfig.getDomain());
            if (domain != null) {
                handler = Pipeline2Factory.getHandler(domain.getName(), domain.getConfig());
                pipelineHandlers.put(exchangeConfig.getDomain(), handler);
            } else {
                log.info("Cannot set up " + queueIdentifier + " - no valid configuration defined");
                return false;
            }
        }
        handler.publishTask(queueIdentifier, message);
        return true;
    }

    public Boolean broadcastMessageToAll(CallingContext context, RapturePipelineTask task) {
        Pipeline2Handler handler = getHandler(BROADCAST);
        if (handler == null) throw new RuntimeException("Cannot set up " + BROADCAST);
        handler.publishTask(BROADCAST, JacksonUtil.jsonFromObject(task));
        return true;
    }

    @Override
    public void publishTaskResponse(CallingContext context, String queueIdentifier, TaskStatus task) {
        String responseQueue = queueIdentifier + "-response";
        Pipeline2Handler responseHandler = pipelineHandlers.get(responseQueue);
        responseHandler.publishTask(responseQueue, JacksonUtil.jsonFromObject(task));
    }

    @Override
    public TaskStatus publishTask(CallingContext context, String queueIdentifier, String message, Long timeout, QueueSubscriber subscriber) {
        Pipeline2Handler requestHandler = pipelineHandlers.get(queueIdentifier);
        Pipeline2Handler responseHandler = pipelineHandlers.get(queueIdentifier + "-response");
        TaskStatus status = new TaskStatus();
        status.setCreationTime(new Date());
        status.setOutput(new ArrayList<>());
        status.setTaskId(queueIdentifier + ":" + UUID.randomUUID().toString());
        status.setCurrentState(PipelineTaskState.SUBMITTED);
        currentTasks.put(status.getTaskId(), status);

        List<QueueSubscriber> watchers = watchmen.get(queueIdentifier);
        if (watchers == null) {
            watchers = new ArrayList<>();
            watchmen.put(queueIdentifier, watchers);
        }
        if (subscriber == null) subscriber = new TaskWatcher(queueIdentifier, "Watcher" + UUID.randomUUID());
        if (!watchers.contains(subscriber)) {
            responseHandler.subscribe(queueIdentifier + "-response", subscriber);
            watchers.add(subscriber);
        }
        subscriber.setStatus(status);
        // Map<String, Object> task = new HashMap<>();
        // task.put("payload", message);
        // task.put("status", status);
        // task.put("queue", queueIdentifier);
        requestHandler.publishTask(queueIdentifier, JacksonUtil.jsonFromObject(status));

        if (timeout > 0) {
            synchronized (status) {
                try {
                    System.out.println("Waiting");
                    status.wait(timeout);
                } catch (Exception e) {
                    System.out.println("Interrupt");
                }
            }
        }
        System.out.println("Done");
        return status;
    }

    @Override
    public TaskStatus getStatus(CallingContext context, String taskId) {
        return currentTasks.get(taskId);
    }

    @Override
    public List<TaskStatus> listTasks(CallingContext context) {
        return new ArrayList<>(currentTasks.values());
    }

    @Override
    protected void finalize() throws Throwable {
        for (String queueIdentifier : watchmen.keySet()) {
            for (QueueSubscriber sub : watchmen.get(queueIdentifier)) {
                try {
                    unsubscribeQueue(null, sub);
                } catch (Exception e) {

                }
                try {
                    removeTaskQueue(null, queueIdentifier);
                } catch (Exception e) {

                }
            }

        }
        super.finalize();
    }

    // @Override
    public void registerExchangeDomain(CallingContext context, String domainURI, String config) {
        RaptureURI internalURI = new RaptureURI(domainURI, Scheme.EXCHANGE_DOMAIN);
        ExchangeDomain exchangeDomain = new ExchangeDomain();
        String name = internalURI.getFullPath();
        if (name.endsWith("/")) {
            name = name.substring(0, name.length() - 1);
        }
        exchangeDomain.setName(name);
        exchangeDomain.setConfig(config);
        ExchangeDomainStorage.add(exchangeDomain, context.getUser(), "Created exchange domain");
    }

    private ExchangeDomain getExchangeDomain(String name) {
        log.info("Getting domain for " + name);
        RaptureURI addressURI = new RaptureURI("//" + name, Scheme.EXCHANGE_DOMAIN);
        return ExchangeDomainStorage.readByAddress(addressURI);
    }

}
