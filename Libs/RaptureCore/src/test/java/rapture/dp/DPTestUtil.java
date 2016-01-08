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
package rapture.dp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import rapture.common.CallingContext;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.dp.Step;
import rapture.common.dp.Transition;
import rapture.common.model.RaptureExchange;
import rapture.common.model.RaptureExchangeQueue;
import rapture.common.model.RaptureExchangeType;
import rapture.kernel.Kernel;

import com.google.common.collect.Maps;

/**
 * This is a place to put static routines to be shared between DP tests.
 * 
 * @author mel
 */
public class DPTestUtil {
    public static final String VIEW_SIGNAL = new RaptureURI.Builder(Scheme.DP_JAVA_INVOCABLE, "ViewSignal").build().toString();
    public static final String EXCHANGE = "questionForkTestExchange";
    public static final String ALPHA = "alpha";

    public static Step makeSignalStep(String key) {
        Map<String, String> view = Maps.newHashMap();
        view.put("signal_name", "#" + key);
        Step step = new Step();
        step.setName(key);
        step.setExecutable(VIEW_SIGNAL);
        step.setView(view);
        step.setTransitions(new ArrayList<Transition>());
        return step;
    }

    public static Transition makeTransition(String name, String target) {
        Transition t = new Transition();
        t.setName(name);
        t.setTargetStep(target);
        return t;
    }

    public static void initPipeline(CallingContext ctx) {
        Kernel.getPipeline().setupStandardCategory(ctx, ALPHA);
        Kernel.getPipeline().registerExchangeDomain(ctx, "//questions", "EXCHANGE {} USING MEMORY {}");

        RaptureExchange exchange = new RaptureExchange();
        exchange.setName(EXCHANGE);
        exchange.setExchangeType(RaptureExchangeType.FANOUT);
        exchange.setDomain("questions");

        List<RaptureExchangeQueue> queues = new ArrayList<RaptureExchangeQueue>();
        RaptureExchangeQueue queue = new RaptureExchangeQueue();
        queue.setName("default");
        queue.setRouteBindings(new ArrayList<String>());
        queues.add(queue);

        exchange.setQueueBindings(queues);

        Kernel.getPipeline().getTrusted().registerPipelineExchange(ctx, EXCHANGE, exchange);
        Kernel.getPipeline().getTrusted().bindPipeline(ctx, ALPHA, EXCHANGE, "default");
        Kernel.setCategoryMembership(ALPHA);
    }

}
