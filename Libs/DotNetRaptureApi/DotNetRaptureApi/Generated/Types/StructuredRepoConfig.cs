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
public class StructuredRepoConfig   {
    [DataMember (Name="description")]
    public string description { get; set; }

    [DataMember (Name="config")]
    public string config { get; set; }

    [DataMember (Name="authority")]
    public string authority { get; set; }

   


}
} 

