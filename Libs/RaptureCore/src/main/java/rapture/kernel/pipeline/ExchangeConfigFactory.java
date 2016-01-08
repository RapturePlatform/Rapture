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
package rapture.kernel.pipeline;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import rapture.common.model.RaptureExchange;
import rapture.common.model.RaptureExchangeQueue;
import rapture.common.model.RaptureExchangeType;
import rapture.common.pipeline.PipelineConstants;
import rapture.util.IDGenerator;

import com.google.common.collect.ImmutableList;

/**
 * Class used to create {@link RaptureExchange}
 * 
 * @author bardhi
 * 
 */
public class ExchangeConfigFactory {

    /**
     * Create the standard fanout exchange configuration, and fill it in so it
     * contains an anonymous queue for this server. This will allow a server to
     * bind to the fanout exchange and receive broadcast messages
     * 
     * @param category
     * @return
     */
    public static RaptureExchange createStandardFanout() {
        RaptureExchange xFanout = new RaptureExchange();
        xFanout.setName(PipelineConstants.FANOUT_EXCHANGE);
        xFanout.setExchangeType(RaptureExchangeType.FANOUT);
        xFanout.setDomain(PipelineConstants.DEFAULT_EXCHANGE_DOMAIN);

        List<RaptureExchangeQueue> qsFanout = new LinkedList<RaptureExchangeQueue>();
        RaptureExchangeQueue qBroadcast = new RaptureExchangeQueue();
        qBroadcast.setName(PipelineConstants.ANONYMOUS_PREFIX + IDGenerator.getUUID());
        qBroadcast.setRouteBindings(new ArrayList<String>());
        qsFanout.add(qBroadcast);
        xFanout.setQueueBindings(qsFanout);

        return xFanout;
    }

    /**
     * Create the standard direct exchange configuration, and fill it in so it
     * contains the 1. category-specific broadcast and 2. category-specific
     * load-balancer for this category. This will allow this server to receive
     * these types of messages
     * 
     * @param category
     * @return
     */
    public static RaptureExchange createStandardDirect(String category) {
        RaptureExchange xDirect = new RaptureExchange();
        xDirect.setName(PipelineConstants.DIRECT_EXCHANGE);
        xDirect.setExchangeType(RaptureExchangeType.DIRECT);
        xDirect.setDomain(PipelineConstants.DEFAULT_EXCHANGE_DOMAIN);

        List<RaptureExchangeQueue> qsDirect = new LinkedList<RaptureExchangeQueue>();

        RaptureExchangeQueue qCategoryBroadcast = new RaptureExchangeQueue();
        qCategoryBroadcast.setName(PipelineConstants.ANONYMOUS_PREFIX + IDGenerator.getUUID());
        qCategoryBroadcast.setRouteBindings(ImmutableList.<String> of(createBroadcastRoutingKey(category)));
        qsDirect.add(qCategoryBroadcast);

        RaptureExchangeQueue qLoadBalance = new RaptureExchangeQueue();
        qLoadBalance.setName(category);
        qLoadBalance.setRouteBindings(ImmutableList.<String> of(createLoadBalancingRoutingKey(category)));
        qsDirect.add(qLoadBalance);
        xDirect.setQueueBindings(qsDirect);

        return xDirect;
    }

    public static String createBroadcastRoutingKey(String category) {
        return category + "-broadcast";
    }

    public static String createLoadBalancingRoutingKey(String category) {
        return category + "-loadBalance";
    }
}
