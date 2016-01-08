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

public interface ScriptRepoApi {
     /**
     * 
     * @deprecated The Repo API is being eliminated
     */
     ContentEnvelope getContent(string raptureURI);

     /**
     * 
     * @deprecated The Repo API is being eliminated
     */
     void putContent(string raptureURI, Object content, string comment);

     /**
     * 
     * @deprecated The Repo API is being eliminated
     */
     void deleteContent(string raptureURI, string comment);

}
}

