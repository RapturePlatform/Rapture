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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import rapture.common.CallingContext;
import rapture.common.FieldType;
import rapture.common.RaptureEntity;
import rapture.common.RaptureEntityStorage;
import rapture.common.RaptureField;
import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureStructure;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.StructureField;
import rapture.common.TableQueryResult;
import rapture.common.ViewColumn;
import rapture.common.ViewRecord;
import rapture.common.api.EntityApi;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;

public class EntityApiImpl extends KernelBase implements EntityApi {

	public EntityApiImpl(Kernel raptureKernel) {
		super(raptureKernel);
	}

	@Override
	public RaptureEntity getEntity(CallingContext context, String entityUri) {
		RaptureURI internalURI = new RaptureURI(entityUri, Scheme.ENTITY);
		return RaptureEntityStorage.readByAddress(internalURI);
	}

	@Override
	public void putEntity(CallingContext context, String entityUri,
			RaptureEntity entity) {
		RaptureURI internalURI = new RaptureURI(entityUri, Scheme.ENTITY);
		entity.setName(internalURI.getShortPath());
		RaptureEntityStorage.add(entity, context.getUser(), "Added entity");
		if (!entity.getIndexFields().isEmpty()) {
			createEntityIndex(context, entityUri, true);
		}
	}

	@Override
	public void deleteEntity(CallingContext context, String entityUri) {
		RaptureURI internalURI = new RaptureURI(entityUri, Scheme.ENTITY);
		RaptureEntityStorage.deleteByAddress(internalURI, context.getUser(),
				"Remove entity");
	}

	@Override
	public List<RaptureFolderInfo> getChildren(CallingContext context,
			String uriPrefix) {
		return RaptureEntityStorage.getChildren(uriPrefix);
	}

	@Override
	public String putEntityDocument(CallingContext context, String entityUri,
			String content) {
		// 1. Load the entity definition
		RaptureEntity e = getEntity(context, entityUri);
		checkEntity(e, entityUri);
		// 2. Validate document against the structure in that definition
		List<String> res = Kernel.getTransform().validateDocument(context,
				content, e.getStructureUri());
		if (res.isEmpty()) {
			// 3. Work out the URI based on the fields in the document
			Map<String, Object> docMap = JacksonUtil.getMapFromJson(content);
			String entityUriPart = getUriPart(docMap, e.getNamingFields());
			// 4. Do a put document on that
			String fullUri = e.getRepoUri() + e.getPrefixInRepo()
					+ entityUriPart;
			System.out.println("Document URI for entity is " + fullUri);
			Kernel.getDoc().putDoc(context, fullUri, content);
			return entityUriPart;
		} else {
			System.out.println(res);
		}
		return "";
	}

	private String getUriPart(Map<String, Object> docMap, List<String> fields) {
		StringBuilder sb = new StringBuilder();
		fields.forEach(s -> {
			sb.append("/");
			sb.append(getField(docMap, s.split("\\.")));
		});
		return sb.toString();
	}

	private String getField(Map<String, Object> docMap, String[] s) {
		Map<String, Object> current = docMap;
		for (int i = 0; i < s.length; i++) {
			if (i == (s.length - 1)) {
				return current.get(s[i]).toString();
			} else {
				current = (Map<String, Object>) current.get(s[i]);
			}
		}
		return "";
	}

	private String fixContentUri(String contentUri) {
		if (contentUri.startsWith("//")) {
			contentUri = contentUri.substring(1);
		} else if (!contentUri.startsWith("/")) {
			contentUri = "/" + contentUri;
		}
		return contentUri;
	}

	@Override
	public String getEntityDocument(CallingContext context, String entityUri,
			String contentUri) {
		// 1. Load the entity definition
		// 2. Construct the real URI for this document
		// 3. Load it.
		RaptureEntity e = getEntity(context, entityUri);
		checkEntity(e, entityUri);
		String fullUri = e.getRepoUri() + e.getPrefixInRepo()
				+ fixContentUri(contentUri);
		System.out.println("Full URI is " + fullUri);
		return Kernel.getDoc().getDoc(context, fullUri);
	}

	@Override
	public void deleteEntityDocument(CallingContext context, String entityUri,
			String contentUri) {
		// 1. Load the entity definition
		// 2. Construct the real URI for this document
		// 3. Delete it.
		RaptureEntity e = getEntity(context, entityUri);
		checkEntity(e, entityUri);
		String fullUri = e.getRepoUri() + e.getPrefixInRepo()
				+ fixContentUri(contentUri);
		System.out.println("Full URI is " + fullUri);
		Kernel.getDoc().deleteDoc(context, fullUri);
	}

	@Override
	public Map<String, RaptureFolderInfo> listDocsByUriPrefix(
			CallingContext context, String entityUri, String uriPrefix,
			int depth) {
		// 1. Load the entity definition
		// 2. Construct the prefix
		// 3. Call getChildren through the doc interface
		RaptureEntity e = getEntity(context, entityUri);
		checkEntity(e, entityUri);
		String fullUri = e.getRepoUri() + e.getPrefixInRepo()
				+ fixContentUri(uriPrefix);
		System.out.println("Full URI is " + fullUri);
		return Kernel.getDoc().listDocsByUriPrefix(context, fullUri, depth);
	}

	@Override
	public void createEntityIndex(CallingContext context, String entityUri,
			Boolean replaceIfExist) {
		// If the RaptureEntity contains a non-zero size index fields
		RaptureEntity e = getEntity(context, entityUri);
		checkEntity(e, entityUri);
		// Compute the index configuration and save it, with the same name as
		// the repo
		// The configuration types come from the structure
		RaptureStructure s = Kernel.getTransform().getStructure(context,
				e.getStructureUri());
		if (!e.getIndexFields().isEmpty()) {
			StringBuilder sb = new StringBuilder();
			// id(id) string, orderDate(orderDate) string, strategy(strategy)
			// string, fund(fund) string, symbol0(legs.0.symbol) string,
			// symbol1(legs.1.symbol) string
			e.getIndexFields().forEach(ifield -> {
                                System.out.println("Looking at " + ifield);
				String x = getFieldType(ifield, s, context);
				if (!x.isEmpty()) {
					sb.append(", ");
					sb.append(ifield);
					sb.append("(");
					sb.append(ifield);
					sb.append(") ");
					sb.append(x);
				}
			});
			String indexConfig = sb.toString().substring(2);
			Kernel.getIndex().createIndex(context, e.getRepoUri(), indexConfig);
		}

	}

	private String getFieldType(String ifield, RaptureStructure s,
			CallingContext context) {
		StringBuilder ret = new StringBuilder();
		s.getFields().forEach(
				f -> {
					if (f.getKey().equals(ifield)) {
						RaptureField rf = Kernel.getTransform().getField(
								context, f.getFieldUri());
						ret.append(flip(rf.getFieldType()));
					}
				});
		return ret.toString();
	}

	private String flip(FieldType fieldType) {
		switch (fieldType) {
		case ARRAY:
			return "";
		case BOOLEAN:
			return "boolean";
		case STRING:
			return "string";
		case DATE:
			return "string";
		case INTEGER:
			return "integer";
		case NUMBER:
			return "double";
		case MAP:
			return "";
		}
		return "";
	}

	@Override
	public List<ViewColumn> getViewConfiguration(CallingContext context,
			String entityUri) {
		// Look at getViewFields(). for each one, add a ViewColumn containing
		// the field name + the Rapture Field as determined by the structure of
		// the entity
		RaptureEntity e = getEntity(context, entityUri);
		checkEntity(e, entityUri);
		// Compute the index configuration and save it, with the same name as
		// the repo
		// The configuration types come from the structure
		RaptureStructure s = Kernel.getTransform().getStructure(context,
				e.getStructureUri());
		List<ViewColumn> ret = new ArrayList<ViewColumn>();
		e.getViewFields().forEach(
				vf -> {
					ViewColumn vc = new ViewColumn();
					vc.setName(vf);
					StructureField sf = s.getFields().stream()
							.filter(f -> f.getKey().equals(vf)).findFirst()
							.get();
					RaptureField rf = Kernel.getTransform().getField(context,
							sf.getFieldUri());
					vc.setField(rf);
					ret.add(vc);
				});
		return ret;
	}

	@Override
	public List<ViewRecord> getViewData(CallingContext context,
			String entityUri, String where, int skip, int limit) {
		// Get the entity and therefore the repo uri
		// Construct a query which is select rowId (where ...) LIMIT limit SKIP
		// skip
		// Run that query
		// For each entry, load the document and extract out the fields as
		// defined in getViewFields()
		// Add that map as a row to the result
		// Return that.

		// If the viewFields is a subset (or equal) to indexFields, we can do
		// this with a query alone as an optimization

		// Do the slow one first
		RaptureEntity e = getEntity(context, entityUri);
		checkEntity(e, entityUri);
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT rowId");
		if (!where.isEmpty()) {
			sb.append(" WHERE ");
			sb.append(where);
		}
		if (limit != 0) {
			sb.append(" LIMIT ");
			sb.append(limit);
		}
		if (skip != 0) {
			sb.append(" SKIP ");
			sb.append(skip);
		}
		
		TableQueryResult res = Kernel.getIndex().findIndex(context, e.getRepoUri(), sb.toString());
		List<ViewRecord> ret = new ArrayList<ViewRecord>();
		res.getRows().forEach(row -> {
			ViewRecord record = new ViewRecord();
			String doc = Kernel.getDoc().getDoc(context, e.getRepoUri() + "/" + row.get(0));
			Map<String, Object> dataMap = JacksonUtil.getMapFromJson(doc);
			Map<String, Object> filteredData = 
			dataMap.entrySet().stream().filter(map -> e.getViewFields().contains(map.getKey())).collect(Collectors.toMap(map -> map.getKey(), map -> map.getValue()));
			record.setData(filteredData);
			record.setRowId(row.get(0).toString());
			ret.add(record);
		});
		return ret;
	}

	private void checkEntity(RaptureEntity e, String name) {
		if (e == null) {
			throw RaptureExceptionFactory.create("No entity found - " + name);
		}
	}
	
	@Override
	public String getEntityDocByKey(CallingContext context, String entityUri,
			String key) {
		RaptureEntity e = getEntity(context, entityUri);
		checkEntity(e, entityUri);
		if (e.getPrimeIndexField().isEmpty()) {
			throw RaptureExceptionFactory.create("No prime index field on this entity");
		}
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT rowId WHERE ");
		sb.append(e.getPrimeIndexField());
		sb.append("='");
		sb.append(key);
		sb.append("'");
		System.out.println("Query is " + sb.toString());
		TableQueryResult res = Kernel.getIndex().findIndex(context, e.getRepoUri(), sb.toString());
		if (res != null && !res.getRows().isEmpty()) {
			return getEntityDocument(context, entityUri, res.getRows().get(0).get(0).toString());
		}
		return null;
	}

}
