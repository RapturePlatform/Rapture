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
package rapture.repo;

import java.util.List;
import java.util.Map;

import rapture.common.sql.FieldMapping;
import rapture.common.sql.JoinMapping;
import rapture.repo.qrep.DistinctSpecification;
import rapture.repo.qrep.FieldValue;
import rapture.repo.qrep.WhereRestriction;

/**
 * A type of store that uses SQL as its underlying storage medium (cf. KeyStore)
 * 
 * @author amkimian
 * 
 */
public interface SQLStore {

    void setInstanceName(String instanceName);

    void setConfig(Map<String, String> config);

    void executeDistinct(DistinctSpecification distinct, List<WhereRestriction> whereClauses, RepoVisitor visitor, boolean isFolder);

    void executeDocument(List<WhereRestriction> whereClauses, FieldMapping fields, RepoVisitor visitor);

    String executeDocument(List<WhereRestriction> whereClauses, FieldMapping fields);

    void performUpdate(List<FieldValue> fieldValues, List<WhereRestriction> whereClauses, JoinMapping joins);

    Boolean validate();
}
