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

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import rapture.common.CallingContext;
import rapture.common.CategoryQueueBindings;
import rapture.common.CategoryQueueBindingsStorage;
import rapture.common.ExchangeDomain;
import rapture.common.ExchangeDomainStorage;
import rapture.common.PipelineTaskStatus;
import rapture.common.RapturePipelineTask;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.ServerCategory;
import rapture.common.ServerCategoryStorage;
import rapture.common.TableQuery;
import rapture.common.api.PipelineApi;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JsonContent;
import rapture.common.model.RaptureExchange;
import rapture.common.model.RaptureExchangeQueue;
import rapture.common.model.RaptureExchangeStorage;
import rapture.common.model.RaptureExchangeType;
import rapture.common.pipeline.PipelineConstants;
import rapture.exchange.ExchangeFactory;
import rapture.exchange.ExchangeHandler;
import rapture.exchange.QueueHandler;
import rapture.exchange.TopicMessageHandler;
import rapture.kernel.internalnotification.ExchangeChangeManager;
import rapture.kernel.pipeline.ExchangeConfigFactory;
import rapture.kernel.pipeline.PipelineIndexHelper;
import rapture.kernel.pipeline.PipelineTaskStatusManager;
import rapture.notification.NotificationMessage;
import rapture.notification.RaptureMessageListener;
import rapture.repo.RepoVisitor;

/**
 * App is all about defining applications for jnlp use
 *
 * @author amkimian
 */
public class PipelineApiImpl extends KernelBase implements PipelineApi, RaptureMessageListener<NotificationMessage> {
    private static final String ERROR = "Could not load document %s, continuing anyway";
    private final PipelineTaskStatusManager taskStatusManager;

    public PipelineApiImpl(Kernel raptureKernel) {
        super(raptureKernel);
        log.info("Ensure index exists");
        PipelineIndexHelper.ensureIndexExists();
        taskStatusManager = new PipelineTaskStatusManager();
    }

    private static Logger log = Logger.getLogger(PipelineApiImpl.class);

    public Boolean registerServerCategory(CallingContext context, String category, String description) {
        ServerCategory cat = new ServerCategory();
        cat.setDescription(description);
        cat.setName(category);
        ServerCategoryStorage.add(cat, context.getUser(), "Created server category");
        return true;
    }

    @Override
    public List<String> getExchanges(CallingContext context) {
        final List<String> ret = new ArrayList<>();
        RaptureExchangeStorage.visitAll(new RepoVisitor() {

            @Override
            public boolean visit(String name, JsonContent content, boolean isFolder) {
                if (!isFolder) {
                    if (content != null && content.getContent() != null) {
                        RaptureExchange exch = RaptureExchangeStorage.readFromJson(content);
                        ret.add(exch.getName());
                    } else {
                        ret.add(name);
                    }
                }
                return true;
            }

        });
        return ret;

    }

    @Override
    public void removeServerCategory(CallingContext context, String category) {
        CategoryQueueBindingsStorage.deleteByFields(category, context.getUser(), "Removed category bindings");
        ServerCategoryStorage.deleteByFields(category, context.getUser(), "Removed server category");
    }

    @Override
    public List<String> getServerCategories(CallingContext context) {
        final List<String> ret = new ArrayList<>();
        ServerCategoryStorage.visitAll(new RepoVisitor() {

            @Override
            public boolean visit(String name, JsonContent content, boolean isFolder) {
                if (!isFolder) {
                    ServerCategory cat = ServerCategoryStorage.readFromJson(content);
                    ret.add(cat.getName());
                }
                return true;
            }

        });
        return ret;
    }

    @Override
    public List<String> getExchangeDomains(CallingContext context) {
        final List<String> ret = new ArrayList<>();
        ExchangeDomainStorage.visitAll(new RepoVisitor() {

            @Override
            public boolean visit(String name, JsonContent content, boolean isFolder) {
                if (!isFolder) {
                    ExchangeDomain dom = ExchangeDomainStorage.readFromJson(content);
                    ret.add(dom.getName());
                }
                return true;
            }

        });
        return ret;
    }

    private CategoryQueueBindings getQueueBindings(String category) {
        return CategoryQueueBindingsStorage.readByFields(category);
    }

    public Boolean bindPipeline(CallingContext context, String category, String exchangeName, String queue) {
        CategoryQueueBindings cqb = getQueueBindings(category);
        if (cqb == null) {
            cqb = new CategoryQueueBindings();
            cqb.setBindings(new HashMap<String, Set<String>>());
            cqb.setName(category);
        }

        Map<String, Set<String>> bindings = cqb.getBindings();
        if (!bindings.containsKey(exchangeName)) {
            bindings.put(exchangeName, new HashSet<String>());
        }
        bindings.get(exchangeName).add(queue);

        CategoryQueueBindingsStorage.add(cqb, context.getUser(), "Added binding");
        return true;
    }

    public Boolean removeBoundPipeline(CallingContext context, String category, String exchange, String queue) {
        CategoryQueueBindings cqb = getQueueBindings(category);
        if (cqb == null) {
            return false;
        }
        Map<String, Set<String>> bindings = cqb.getBindings();
        if (bindings.containsKey(exchange)) {
            bindings.get(exchange).remove(queue);
            if (bindings.get(exchange).isEmpty()) {
                bindings.remove(exchange);
            }
        }

        CategoryQueueBindingsStorage.add(cqb, context.getUser(), "Removed binding");
        return true;
    }

    private Map<String, ExchangeHandler> domainHandlers = new HashMap<>();

    public Boolean publishPipelineMessage(CallingContext context, String exchange, RapturePipelineTask task) {
        // Here we want to ensure that the exchange is running and then send it
        // to the appropriate exchange handler
        // for that exchange
        RaptureExchange exchangeConfig = getExchange(exchange);
        if (exchangeConfig != null) {
            ExchangeHandler handler = getExchangeHandler(exchangeConfig);
            handler.setupExchange(exchangeConfig);
            taskStatusManager.initialCreation(task);
            log.debug("Publishing pipeline task: " + task.getTaskId());
            String routingKey = ""; //$NON-NLS-1$
            if (!task.getCategoryList().isEmpty()) {
                routingKey = task.getCategoryList().get(0);
            }
            handler.putTaskOnExchange(exchange, task, routingKey);
            return true;
        } else {
            log.error("No exchange found for " + exchange + ", cannot publish Pipeline message");
        }
        return false;
    }

    @Override
    public void drainPipeline(CallingContext context, String exchange) {
        RaptureExchange exchangeConfig = getExchange(exchange);
        if (exchangeConfig != null) {
            ExchangeHandler handler = getExchangeHandler(exchangeConfig);
            handler.setupExchange(exchangeConfig);
            handler.tearDownExchange(exchangeConfig);
            // This needs to be communicated to all kernels
            Kernel.exchangeChanged(exchange);
        }
    }

    private ExchangeHandler getExchangeHandler(RaptureExchange exchangeConfig) {
        return getExchangeHandler(exchangeConfig.getDomain());
    }

    private ExchangeHandler getExchangeHandler(String domain) {
        if (!domainHandlers.containsKey(domain)) {
            log.info("Domain " + domain + " not found, setting up");
            setupDomain(domain);
        }
        ExchangeHandler handler = domainHandlers.get(domain);
        return handler;
    }

    private synchronized void setupDomain(String name) {
        ExchangeDomain domain = getExchangeDomain(name);
        if (domain != null) {
            ExchangeHandler handler = ExchangeFactory.getHandler(name, domain.getConfig());
            domainHandlers.put(name, handler);
        }
    }

    private RaptureExchange getExchange(String name) {
        return RaptureExchangeStorage.readByFields(name);
    }

    private ExchangeDomain getExchangeDomain(String name) {
        log.info("Getting domain for " + name);
        RaptureURI addressURI = new RaptureURI("//" + name, Scheme.EXCHANGE_DOMAIN);
        return ExchangeDomainStorage.readByAddress(addressURI);
    }

    @Override
    public List<CategoryQueueBindings> getBoundExchanges(CallingContext context, final String category) {
        final List<CategoryQueueBindings> ret = new ArrayList<>();
        CategoryQueueBindingsStorage.visitAll(new RepoVisitor() {

            @Override
            public boolean visit(String name, JsonContent content, boolean isFolder) {
                if (!isFolder) {
                    CategoryQueueBindings queueBinding;
                    try {
                        queueBinding = CategoryQueueBindingsStorage.readFromJson(content);
                        if (queueBinding.getName().equals(category)) {
                            ret.add(queueBinding);
                        }
                    } catch (RaptureException e) {
                        logError(name);
                    }
                }
                return true;
            }

        });
        return ret;
    }

    private void logError(String name) {
        log.error(String.format(ERROR, name));
    }

    public Boolean registerPipelineExchange(CallingContext context, String name, RaptureExchange exchange) {
        RaptureExchangeStorage.add(exchange, context.getUser(), "Created pipeline exchange");
        return true;
    }

    @Override
    public void deregisterPipelineExchange(CallingContext context, String name) {
        RaptureExchangeStorage.deleteByFields(name, context.getUser(), "Removed pipeline exchange");
    }

    @Override
    public RaptureExchange getExchange(CallingContext context, String name) {
        return RaptureExchangeStorage.readByFields(name);
    }

    @Override
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

    @Override
    public void deregisterExchangeDomain(CallingContext context, String domainURI) {
        RaptureURI addressURI = new RaptureURI(domainURI, Scheme.SCRIPT);
        ExchangeDomainStorage.deleteByAddress(addressURI, context.getUser(), "Removed exchange domain");
    }

    /**
     * Register listeners for a standard category. These are categories set up using {@link #setupStandardCategory(CallingContext, String)}
     *
     * @param categoryName
     * @param queueHandler
     */
    public void lowRegisterStandard(String categoryName, QueueHandler queueHandler) {
        RaptureExchange xFanout = ExchangeConfigFactory.createStandardFanout();
        ExchangeHandler fanoutHandler = getExchangeHandler(xFanout);
        fanoutHandler.setupExchange(xFanout);
        for (RaptureExchangeQueue queue : xFanout.getQueueBindings()) {
            fanoutHandler.startConsuming(xFanout.getName(), queue.getName(), queueHandler);
        }

        RaptureExchange xDirect = ExchangeConfigFactory.createStandardDirect(categoryName);
        ExchangeHandler directHandler = getExchangeHandler(xDirect);
        directHandler.setupExchange(xDirect);
        for (RaptureExchangeQueue queue : xDirect.getQueueBindings()) {
            directHandler.startConsuming(xDirect.getName(), queue.getName(), queueHandler);
        }
    }

    public void lowRegisterListener(String exchangeName, String queue, QueueHandler queueHandler) {
        log.info("Looking for exchange " + exchangeName);
        RaptureExchange exchangeConfig = getExchange(exchangeName);
        if (exchangeConfig != null) {
            log.info("Retrieving handler for exchange");
            ExchangeHandler handler = getExchangeHandler(exchangeConfig);
            handler.setupExchange(exchangeConfig);
            handler.startConsuming(exchangeName, queue, queueHandler);
        } else {
            log.error("No exchange found for " + exchangeName + ", cannot start consuming Pipeline messages");
        }
    }

    public void lowDeregisterListener(String exchangeName, String queue) {
        /**
         * RaptureExchange exchangeConfig = getExchange(exchangeName); if (exchangeConfig != null) { ExchangeHandler handler =
         * getExchangeHandler(exchangeConfig); // TBD }
         */
    }

    @Override
    public void signalMessage(NotificationMessage message) {
        if (ExchangeChangeManager.isExchangeMessage(message)) {
            handleExchangeChanged(ExchangeChangeManager.getExchangeFromMessage(message));
        }
    }

    void handleExchangeChanged(String exchangeName) {
        if (exchangeName != null) {
            RaptureExchange exchangeConfig = getExchange(exchangeName);
            if (exchangeConfig != null) {
                ExchangeHandler handler = getExchangeHandler(exchangeConfig);
                handler.ensureExchangeUnAvailable(exchangeConfig);
            }
        }
    }

    @Override
    public PipelineTaskStatus getStatus(CallingContext context, String taskId) {
        return taskStatusManager.getStatus(taskId);
    }

    @Override
    public List<RapturePipelineTask> queryTasks(CallingContext context, String query) {
        return taskStatusManager.queryTasks(query);
    }

    @Override
    public List<RapturePipelineTask> queryTasksOld(CallingContext context, TableQuery query) {
        return taskStatusManager.queryTasksOld(query);
    }

    @Override
    public Long getLatestTaskEpoch(CallingContext context) {
        return taskStatusManager.getLatestEpoch();
    }

    @Override
    public void setupStandardCategory(CallingContext context, String category) {
        if (Pipeline2ApiImpl.usePipeline2) {
            Kernel.getPipeline2().createBroadcastQueue(context, category, null);
        } else {
            if (registerServerCategory(context, category, "description")) {
                bindPipeline(context, category, PipelineConstants.DEFAULT_EXCHANGE, "");
            } else {
                throw RaptureExceptionFactory.create("Error registering server category " + category);
            }
        }
    }

    @Override
    public void publishMessageToCategory(CallingContext context, RapturePipelineTask task) {
        if (task.getCategoryList() == null || task.getCategoryList().isEmpty()) {
            log.error("Attempt to publish message without a target category");
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "Bad message, a category must be specified for load balanced messages!");
        } else {
            taskStatusManager.initialCreation(task);
            log.debug("Publishing pipeline task: " + task.getTaskId());
            RaptureExchange exchangeConfig = ExchangeConfigFactory.createStandardDirect(task.getCategoryList().get(0));
            ExchangeHandler handler = getExchangeHandler(exchangeConfig);

            // Should this be an exception? Seems to happen in unit tests executed from gradle but not from Eclipse.
            if (handler == null) {
                log.warn("Cannot publish message: No handler defined for " + exchangeConfig.toString() + " for task " + task.toString());
            } else {
                String routingKey = ExchangeConfigFactory.createLoadBalancingRoutingKey(task.getCategoryList().get(0));
                handler.putTaskOnExchange(exchangeConfig.getName(), task, routingKey);
                log.debug("Publishing complete");
            }
        }
    }

    @Override
    public void broadcastMessageToCategory(CallingContext context, RapturePipelineTask task) {
        if (task.getCategoryList() == null || task.getCategoryList().isEmpty()) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "Bad message, a category must be specified for broadcast to category!");

        } else {
            taskStatusManager.initialCreation(task);
            log.debug("Publishing pipeline task: " + task.getTaskId());
            RaptureExchange exchangeConfig = ExchangeConfigFactory.createStandardDirect(task.getCategoryList().get(0));
            ExchangeHandler handler = getExchangeHandler(exchangeConfig);
            String routingKey = ExchangeConfigFactory.createBroadcastRoutingKey(task.getCategoryList().get(0));
            handler.putTaskOnExchange(exchangeConfig.getName(), task, routingKey);
        }
    }

    @Override
    public void broadcastMessageToAll(CallingContext context, RapturePipelineTask task) {
        if (task.getCategoryList() != null && !task.getCategoryList().isEmpty()) {
            throw RaptureExceptionFactory
                    .create(HttpURLConnection.HTTP_BAD_REQUEST, "Bad message, category cannot be specified for broadcast to all messages!");
        } else {
            taskStatusManager.initialCreation(task);
            log.debug("Publishing pipeline task: " + task.getTaskId());
            RaptureExchange exchangeConfig = ExchangeConfigFactory.createStandardFanout();
            ExchangeHandler handler = getExchangeHandler(exchangeConfig);
            handler.putTaskOnExchange(exchangeConfig.getName(), task, "");
        }
    }

    @Override
    public void createTopicExchange(CallingContext context, String domain, String exchange) {

        if (Pipeline2ApiImpl.usePipeline2) {
            Kernel.getPipeline2().createBroadcastQueue(context, domain, null);
        } else {
            RaptureExchange exc = new RaptureExchange();
            exc.setDomain(domain);
            exc.setExchangeType(RaptureExchangeType.TOPIC);
            exc.setName(exchange);
            registerPipelineExchange(context, exchange, exc);
        }
    }

    @Override
    public void publishTopicMessage(CallingContext context, String domain, String exchange, String topic, String message) {

        RaptureExchange exchangeConfig = getExchange(exchange);
        if (exchangeConfig != null) {
            ExchangeHandler handler = getExchangeHandler(exchangeConfig);
            handler.setupExchange(exchangeConfig);
            handler.publishTopicMessage(exchange, topic, message);
        }
    }

    // You can only use this in a Kernel application
    public long subscribeTopic(String domain, String exchange, String topic, TopicMessageHandler messageHandler) {
        log.info("Subscribing attempt to domain " + domain + ", topic " + exchange + ", subscriber ID " + topic);
        RaptureExchange exchangeConfig = getExchange(exchange);
        if (exchangeConfig != null) {
            ExchangeHandler handler = getExchangeHandler(exchangeConfig);
            handler.setupExchange(exchangeConfig);
            long ret = handler.subscribeTopic(exchange, topic, messageHandler);
            return ret;
        } else {
            log.error("No config found for " + exchange);
        }
        return -1L;
    }

    public void unsubscribeTopic(String domain, String exchange, long handle) {
        RaptureExchange exchangeConfig = getExchange(exchange);
        if (exchangeConfig != null) {
            ExchangeHandler handler = getExchangeHandler(exchangeConfig);
            handler.setupExchange(exchangeConfig);
            handler.unsubscribeTopic(handle);
        }
    }
}
