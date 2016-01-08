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
 * This file is autogenerated and any changes made to it will be overwritten
 */

using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Runtime.Serialization;
using DotNetRaptureAPI.Common.FixedTypes;

namespace DotNetRaptureAPI.schedule
{
    [DataContract]
    public class CreateWorkflowJobPayload
    {
		    [DataMember (Name="jobURI")]
			public string jobURI { get; set; }



		    [DataMember (Name="description")]
			public string description { get; set; }



		    [DataMember (Name="workflowURI")]
			public string workflowURI { get; set; }



		    [DataMember (Name="cronExpression")]
			public string cronExpression { get; set; }



		    [DataMember (Name="timeZone")]
			public string timeZone { get; set; }



		    [DataMember (Name="jobParams")]
			public Dictionary<string, string> jobParams { get; set; }



		    [DataMember (Name="autoActivate")]
			public bool autoActivate { get; set; }



		    [DataMember (Name="maxRuntimeMinutes")]
			public int maxRuntimeMinutes { get; set; }



		    [DataMember (Name="appStatusNamePattern")]
			public string appStatusNamePattern { get; set; }



	}
}

