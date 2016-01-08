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
 * This file is autogenerated and any changes will be overwritten.
 */

using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Runtime.Serialization;
using DotNetRaptureAPI.Common.FixedTypes;

namespace DotNetRaptureAPI
{
/**
* 
**/

[DataContract]
public class LogMessage   {
    [DataMember (Name="timestamp")]
    public long timestamp { get; set; }

    [DataMember (Name="host")]
    public string host { get; set; }

    [DataMember (Name="level")]
    public string level { get; set; }

    [DataMember (Name="appName")]
    public string appName { get; set; }

    [DataMember (Name="threadName")]
    public string threadName { get; set; }

    [DataMember (Name="className")]
    public string className { get; set; }

    [DataMember (Name="lineNumber")]
    public int lineNumber { get; set; }

    [DataMember (Name="message")]
    public string message { get; set; }

    [DataMember (Name="workOrderURI")]
    public string workOrderURI { get; set; }

    [DataMember (Name="workerId")]
    public string workerId { get; set; }

    [DataMember (Name="rfxScript")]
    public string rfxScript { get; set; }

    [DataMember (Name="stepName")]
    public string stepName { get; set; }

   


}
} 

