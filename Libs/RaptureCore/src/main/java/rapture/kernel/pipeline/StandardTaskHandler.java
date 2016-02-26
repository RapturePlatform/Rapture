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
package rapture.kernel.pipeline;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import rapture.common.CategoryQueueBindings;
import rapture.common.pipeline.PipelineConstants;
import rapture.exchange.QueueHandler;
import rapture.kernel.ContextFactory;
import rapture.kernel.PipelineApiImpl;

/**
 * The kernel task handler is intended to bind to task queues, listen for
 * messages on those queues and execute those tasks. Each server has a number of
 * categories they are part of - this is also managed at this level.
 * 
 * @author amkimian
 * 
 */
public abstract class StandardTaskHandler {
    private static final Logger log = Logger.getLogger(StandardTaskHandler.class);
    private PipelineApiImpl pipeline;

    public StandardTaskHandler(PipelineApiImpl pipeline) {
        this.pipeline = pipeline;
    }

    protected PipelineApiImpl getPipeline() {
        return pipeline;
    }

    public void setCategoryMembership(String category) {
        manageCategoryMembership(category, true);
    }

    public void setCategoryMembership(String category, Map<String, QueueHandler> customHandlers) {
        manageCategoryMembership(category, customHandlers, true);
    }

    public void revokeCategoryMembership(String category) {
        manageCategoryMembership(category, false);
    }

    private void manageCategoryMembership(String category, boolean register) {
        manageCategoryMembership(category, null, register);
    }

    private void manageCategoryMembership(String category, Map<String, QueueHandler> customHandlers, boolean register) {
        // Get the bindings for this category, and bind to the exchange queues
        // as consumers
        log.info(String.format("Managing category membership for category %s", category));
        List<CategoryQueueBindings> bindings = pipeline.getBoundExchanges(ContextFactory.getKernelUser(), category);
        for (CategoryQueueBindings binding : bindings) {
            Map<String, Set<String>> bindingsByExchange = binding.getBindings();
            log.info("Looking at " + binding.getName());
            for (Map.Entry<String, Set<String>> exchangeBinding : bindingsByExchange.entrySet()) {
                String exchangeName = exchangeBinding.getKey();
                log.info("Exchange name is " + exchangeName);
                if (PipelineConstants.DEFAULT_EXCHANGE.equals(exchangeName)) {
                    log.info("Registering default listeners");
                    registerDefaultListeners(category, customHandlers);
                } else {
                    Set<String> queues = exchangeBinding.getValue();
                    for (String queue : queues) {
                        log.info("Working with queue " + queue);
                        if (register) {
                            registerListener(exchangeName, queue, customHandlers);
                        } else {
                            deregisterListener(exchangeName, queue);
                        }
                    }
                }
            }
        }
    }

    protected abstract void registerDefaultListeners(String category, Map<String, QueueHandler> customHandlers);

    protected abstract void registerListener(String exchangeName, String queue, Map<String, QueueHandler> customHandlers);

    private void deregisterListener(String exchangeName, String queue) {
        pipeline.lowDeregisterListener(exchangeName, queue);
    }

}
