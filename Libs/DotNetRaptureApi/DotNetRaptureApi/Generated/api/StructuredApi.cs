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
    public interface StructuredApi {
     /**
     * Create a repository for structured data
     * 
     */
     void createStructuredRepo(CallingContext context, string uri, string config);

     /**
     * Delete a repository for structured data
     * 
     */
     void deleteStructuredRepo(CallingContext context, string uri);

     /**
     * check existence
     * 
     */
     bool structuredRepoExists(CallingContext context, string uri);

     /**
     * get a specific structured repo config given a uri
     * 
     */
     StructuredRepoConfig getStructuredRepoConfig(CallingContext context, string uri);

     /**
     * get list of all configurations
     * 
     */
     List<StructuredRepoConfig> getStructuredRepoConfigs(CallingContext context);

     /**
     * create a structured table using raw sql
     * 
     */
     void createTableUsingSql(CallingContext context, string schema, string rawSql);

     /**
     * create a structured table using a column name to SQL column type map
     * 
     */
     void createTable(CallingContext context, string tableUri, Dictionary<string, string> columns);

     /**
     * drop a structured table and all of its data
     * 
     */
     void dropTable(CallingContext context, string tableUri);

     /**
     * check if table exists
     * 
     */
     bool tableExists(CallingContext context, string tableUri);

     /**
     * get table description
     * 
     */
     Dictionary<string, string> describeTable(CallingContext context, string tableUri);

     /**
     * add column(s) to an existing table.  Table must exist beforehand
     * 
     */
     void addTableColumns(CallingContext context, string tableUri, Dictionary<string, string> columns);

     /**
     * remove column(s) from an existing table.  Table must exist beforehand
     * 
     */
     void deleteTableColumns(CallingContext context, string tableUri, List<string> columnNames);

     /**
     * update column(s) in an existing table.  Table must exist beforehand
     * 
     */
     void updateTableColumns(CallingContext context, string tableUri, Dictionary<string, string> columns);

     /**
     * rename column(s) in an existing table.  Table must exist beforehand
     * 
     */
     void renameTableColumns(CallingContext context, string tableUri, Dictionary<string, string> columnNames);

     /**
     * create an index on a structured table
     * 
     */
     void createIndex(CallingContext context, string tableUri, string indexName, List<string> columnNames);

     /**
     * remove an index that was previously created on a table
     * 
     */
     void dropIndex(CallingContext context, string tableUri, string indexName);

     /**
     * retrieve data from multiple tables
     * 
     */
     List<Dictionary<string, Object>> selectJoinedRows(CallingContext context, List<string> tableUris, List<string> columnNames, string from, string where, List<string> order, bool ascending, int limit);

     /**
     * retrieve data with raw sql
     * 
     */
     List<Dictionary<string, Object>> selectUsingSql(CallingContext context, string schema, string rawSql);

     /**
     * retrieve data from a single table
     * 
     */
     List<Dictionary<string, Object>> selectRows(CallingContext context, string tableUri, List<string> columnNames, string where, List<string> order, bool ascending, int limit);

     /**
     * insert new data with raw sql
     * 
     */
     void insertUsingSql(CallingContext context, string schema, string rawSql);

     /**
     * insert new data into a single table
     * 
     */
     void insertRow(CallingContext context, string tableUri, Dictionary<string, Object> values);

     /**
     * insert one or more rows of data into a single table
     * 
     */
     void insertRows(CallingContext context, string tableUri, List<Dictionary<string, Object>> values);

     /**
     * delete data with raw sql
     * 
     */
     void deleteUsingSql(CallingContext context, string schema, string rawSql);

     /**
     * delete data from a single table
     * 
     */
     void deleteRows(CallingContext context, string tableUri, string where);

     /**
     * update existing data with raw sql
     * 
     */
     void updateUsingSql(CallingContext context, string schema, string rawSql);

     /**
     * update existing data from a single table
     * 
     */
     void updateRows(CallingContext context, string tableUri, Dictionary<string, Object> values, string where);

     /**
     * start a transaction
     * 
     */
     bool begin(CallingContext context);

     /**
     * commit a transaction
     * 
     */
     bool commit(CallingContext context);

     /**
     * rollback a transaction
     * 
     */
     bool rollback(CallingContext context);

     /**
     * abort a transaction of given id
     * 
     */
     bool abort(CallingContext context, string transactionId);

     /**
     * get active transactions
     * 
     */
     List<string> getTransactions(CallingContext context);

     /**
     * generate the DDL sql that represents an entire schema or an individual table in the schema
     * 
     */
     string getDdl(CallingContext context, string uri, bool includeTableData);

     /**
     * retrieve a cursor for row-by-row access to data using raw sql
     * 
     */
     string getCursorUsingSql(CallingContext context, string schema, string rawSql);

     /**
     * retrieve a cursor for row-by-row access to data
     * 
     */
     string getCursor(CallingContext context, string tableUri, List<string> columnNames, string where, List<string> order, bool ascending, int limit);

     /**
     * retrieve a cursor for data from multiple tables
     * 
     */
     string getCursorForJoin(CallingContext context, List<string> tableUris, List<string> columnNames, string from, string where, List<string> order, bool ascending, int limit);

     /**
     * given a cursor id, get the next row in the result set
     * 
     */
     List<Dictionary<string, Object>> next(CallingContext context, string tableUri, string cursorId, int count);

     /**
     * given a cursor id, get the next row in the result set
     * 
     */
     List<Dictionary<string, Object>> previous(CallingContext context, string tableUri, string cursorId, int count);

     /**
     * close a cursor once done with it
     * 
     */
     void closeCursor(CallingContext context, string tableUri, string cursorId);

     /**
     * Create a stored procedure with raw SQL
     * 
     */
     void createProcedureCallUsingSql(CallingContext context, string procUri, string rawSql);

     /**
     * Call a stored procedure a value
     * 
     */
     StoredProcedureResponse callProcedure(CallingContext context, string procUri, StoredProcedureParams rparams);

     /**
     * Delete a stored procedure with raw SQL
     * 
     */
     void dropProcedureUsingSql(CallingContext context, string procUri, string rawSql);

	}
}

