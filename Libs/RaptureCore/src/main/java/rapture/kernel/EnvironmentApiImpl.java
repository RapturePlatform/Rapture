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

import rapture.common.CallingContext;
import rapture.common.LicenseInfo;
import rapture.common.api.EnvironmentApi;
import rapture.common.model.RaptureNetwork;
import rapture.common.model.RaptureNetworkStorage;
import rapture.common.model.RaptureServerInfo;
import rapture.common.model.RaptureServerInfoStorage;
import rapture.common.model.RaptureServerStatus;
import rapture.common.model.RaptureServerStatusStorage;
import rapture.config.MultiValueConfigLoader;

import java.util.List;

import org.apache.log4j.Logger;

public class EnvironmentApiImpl extends KernelBase implements EnvironmentApi {
    private static Logger log = Logger.getLogger(EnvironmentApiImpl.class);

    public EnvironmentApiImpl(Kernel raptureKernel) {
        super(raptureKernel);
    }

    @Override
    public RaptureNetwork getNetworkInfo(CallingContext context) {
        List<RaptureNetwork> ret = RaptureNetworkStorage.readAll();
        if (ret.size() > 0) {
            return ret.get(0);
        } else {
            return null;
        }
    }

    @Override
    public RaptureNetwork setNetworkInfo(CallingContext context, RaptureNetwork network) {
        log.info("Setting network info " + network.getNetworkName());
        RaptureNetworkStorage.add(network, context.getUser(), "Updated Rapture Network information");
        return network;
    }

    @Override
    public RaptureServerInfo getThisServer(CallingContext context) {
        String serverId = MultiValueConfigLoader.getConfig("ENVIRONMENT-id");
        if (serverId == null) {
            log.info("No server id found!");
            return null;
        } else {
            log.info("Loading information for server " + serverId);
            return RaptureServerInfoStorage.readByFields(serverId);
        }
    }

    @Override
    public List<RaptureServerInfo> getServers(CallingContext context) {
        return RaptureServerInfoStorage.readAll();
    }

    @Override
    public RaptureServerInfo setThisServer(CallingContext context, RaptureServerInfo info) {
        log.info("Writing server information out id is " + info.getServerId());
        MultiValueConfigLoader.writeConfig("ENVIRONMENT-id", info.getServerId());
        log.info("Name is " + info.getName() + ", storing");
        RaptureServerInfoStorage.add(info, context.getUser(), "Set initial ID");
        return info;
    }

    @Override
    public void setApplianceMode(CallingContext context, Boolean mode) {
        MultiValueConfigLoader.writeConfig("ENVIRONMENT-appliance", mode.toString());
    }

    @Override
    public Boolean getApplianceMode(CallingContext context) {
        return Boolean.valueOf(MultiValueConfigLoader.getConfig("ENVIRONMENT-appliance", "false"));
    }

    @Override
    public List<RaptureServerStatus> getServerStatus(CallingContext context) {
        return RaptureServerStatusStorage.readAll();
    }

}
