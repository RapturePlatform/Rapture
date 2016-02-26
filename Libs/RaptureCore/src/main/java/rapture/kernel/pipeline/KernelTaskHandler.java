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

import java.util.Map;

import rapture.exchange.QueueHandler;
import rapture.kernel.PipelineApiImpl;

/**
 * The kernel task handler is intended to bind to task queues, listen for
 * messages on those queues and execute those tasks. Each server has a number of
 * categories they are part of - this is also managed at this level.
 * 
 * @author amkimian
 * 
 */
public class KernelTaskHandler extends StandardTaskHandler {
    public KernelTaskHandler(PipelineApiImpl pipeline) {
        super(pipeline);
    }

    @Override
    protected void registerListener(String exchangeName, String queue, Map<String, QueueHandler> customHandlers) {
        // Need to get to the exchange handler, then we are all set
        getPipeline().lowRegisterListener(exchangeName, queue, new PipelineQueueHandler(customHandlers));
    }

    @Override
    protected void registerDefaultListeners(String category, Map<String, QueueHandler> customHandlers) {
        getPipeline().lowRegisterStandard(category, new PipelineQueueHandler(customHandlers));
    }

}
