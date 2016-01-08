/**
 * Copyright (C) 2011-2013 Incapture Technologies LLC
 * 
 * This is an autogenerated license statement. When copyright notices appear below
 * this one that copyright supercedes this statement.
 *
 * Unless required by applicable law or agreed to in writing, software is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 *
 * Unless explicit permission obtained in writing this software cannot be distributed.
 */

/**
 * This is an autogenerated file. You should not edit this file as any changes
 * will be overwritten.
 */

using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using DotNetRaptureAPI.Common.FixedTypes;

namespace DotNetRaptureAPI
{
    public interface EnvironmentApi {
     /**
     * Retrieves the unique identifier and name for this Rapture instance.
     * 
     */
     RaptureNetwork getNetworkInfo(CallingContext context);

     /**
     * Applies network information matching the passed parameter to the current instance of the Rapture network.
     * 
     */
     RaptureNetwork setNetworkInfo(CallingContext context, RaptureNetwork network);

     /**
     * Retrieves the unique identifier and name for this Rapture server instance.
     * 
     */
     RaptureServerInfo getThisServer(CallingContext context);

     /**
     * Returns a list of the unique identifiers and names for all Rapture servers in the network.
     * 
     */
     List<RaptureServerInfo> getServers(CallingContext context);

     /**
     * Sets the passed parameter as information for the current server instance.
     * 
     */
     RaptureServerInfo setThisServer(CallingContext context, RaptureServerInfo info);

     /**
     * Configures the instance into or out of appliance mode.
     * 
     */
     void setApplianceMode(CallingContext context, bool mode);

     /**
     * Determines whether the instance is currently in appliance mode.
     * 
     */
     bool getApplianceMode(CallingContext context);

     /**
     * Returns the last reported state for each server in the network. This includes a numerical status, a human readable message, and a Date object indicating the time that the status was last updated.
     * 
     */
     List<RaptureServerStatus> getServerStatus(CallingContext context);

     /**
     * Returns the licensee name, license expiration date, and whether the license is a developer license.
     * 
     */
     LicenseInfo getLicenseInfo(CallingContext context);

	}
}

