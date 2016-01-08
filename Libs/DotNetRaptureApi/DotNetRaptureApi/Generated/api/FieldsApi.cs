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
    public interface FieldsApi {
     /**
     * Returns a list of URIs of all documents and folders below this point, mapping the URI to a RaptureFolderInfo object
     * 
     */
     Dictionary<string, RaptureFolderInfo> listFieldsByUriPrefix(CallingContext context, string authority, int depth);

     /**
     * Retrieves the field definition.
     * 
     */
     RaptureField getField(CallingContext context, string fieldUri);

     /**
     * Create or replace the field definition
     * 
     */
     void putField(CallingContext context, RaptureField field);

     /**
     * Check whether a field definition with the given uri exists
     * 
     */
     bool fieldExists(CallingContext context, string fieldUri);

     /**
     * Delete a field definition
     * 
     */
     void deleteField(CallingContext context, string fieldUri);

     /**
     * Returns a list of values referenced by the fields. Note that there is not a simple 1:1 mapping between the returned list and the list of fields supplied as a parameter.
     * 
     */
     List<string> getDocumentFields(CallingContext context, string docURI, List<string> fields);

     /**
     * Behaves similarly to getFieldsFromDocument, except that the supplied content is first added to the document cache, overwriting any previous values.
     * 
     */
     List<string> putDocumentAndGetDocumentFields(CallingContext context, string docURI, string content, List<string> fields);

	}
}

