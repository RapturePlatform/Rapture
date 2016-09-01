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

import java.util.List;
import java.util.Map;

import rapture.common.CallingContext;
import rapture.common.RaptureField;
import rapture.common.RaptureFieldStorage;
import rapture.common.RaptureFieldTransform;
import rapture.common.RaptureFieldTransformStorage;
import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureStructure;
import rapture.common.RaptureStructureStorage;
import rapture.common.RaptureTransform;
import rapture.common.RaptureTransformStorage;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.api.TransformApi;
import rapture.field.FieldEngine;
import rapture.kernel.field.ScriptedLoader;

public class TransformApiImpl extends KernelBase implements TransformApi {

     public TransformApiImpl(Kernel raptureKernel) {
        super(raptureKernel);
    }

	@Override
	public RaptureField getField(CallingContext context, String fieldUri) {
		RaptureURI internalURI = new RaptureURI(fieldUri, Scheme.FIELD);
		return RaptureFieldStorage.readByAddress(internalURI);
	}

	@Override
	public void putField(CallingContext context, String fieldUri,
			RaptureField field) {
		RaptureURI internalURI = new RaptureURI(fieldUri, Scheme.FIELD);
		field.setName(internalURI.getShortPath());
		RaptureFieldStorage.add(field, context.getUser(), "Added field");
	}

	@Override
	public void deleteField(CallingContext context, String fieldUri) {
		RaptureURI internalURI = new RaptureURI(fieldUri, Scheme.FIELD);
		RaptureFieldStorage.deleteByAddress(internalURI, context.getUser(), "Remove field");
		// Referential integrity?
	}


	@Override
	public RaptureStructure getStructure(CallingContext context,
			String structureUri) {
		RaptureURI structureURI = new RaptureURI(structureUri, Scheme.STRUCTURE);
		return RaptureStructureStorage.readByAddress(structureURI);
	}

	@Override
	public void putStructure(CallingContext context, String structureUri,
			RaptureStructure structure) {
		RaptureURI structureURI = new RaptureURI(structureUri, Scheme.STRUCTURE);
		structure.setName(structureURI.getShortPath());
		RaptureStructureStorage.add(structure, context.getUser(), "Added structure");
	}

	@Override
	public void deleteStructure(CallingContext context, String structureUri) {
		RaptureURI structureURI = new RaptureURI(structureUri, Scheme.STRUCTURE);
		RaptureStructureStorage.deleteByAddress(structureURI, context.getUser(), "Removed structure");
	}


	@Override
	public RaptureTransform getTransform(CallingContext context,
			String transformUri) {
		RaptureURI transformURI = new RaptureURI(transformUri, Scheme.TRANSFORM);
		return RaptureTransformStorage.readByAddress(transformURI);
	}

	@Override
	public void putTransform(CallingContext context, String transformUri,
			RaptureTransform transform) {
		RaptureURI transformURI = new RaptureURI(transformUri, Scheme.TRANSFORM);
		transform.setName(transformURI.getShortPath());
		RaptureTransformStorage.add(transform, context.getUser(), "Added transform");
	}

	@Override
	public void deleteTransform(CallingContext context, String transformUri) {
		RaptureURI transformURI = new RaptureURI(transformUri, Scheme.TRANSFORM);
		RaptureTransformStorage.deleteByAddress(transformURI, context.getUser(), "Removed transform");
	}


	@Override
	public List<RaptureFolderInfo> getFieldChildren(CallingContext context,
			String uriPrefix) {
		return RaptureFieldStorage.getChildren(uriPrefix);
	}

	@Override
	public List<RaptureFolderInfo> getStructureChildren(CallingContext context,
			String uriPrefix) {
		return RaptureStructureStorage.getChildren(uriPrefix);
	}

	@Override
	public List<RaptureFolderInfo> getTransformChildren(CallingContext context,
			String uriPrefix) {
		return RaptureTransformStorage.getChildren(uriPrefix);
	}

	@Override
	public RaptureFieldTransform getFieldTransform(CallingContext context,
			String transformUri) {
		RaptureURI transformURI = new RaptureURI(transformUri, Scheme.FIELDTRANSFORM);
		return RaptureFieldTransformStorage.readByAddress(transformURI);
	}

	@Override
	public void putFieldTransform(CallingContext context, String transformUri,
			RaptureFieldTransform transform) {
		RaptureURI transformURI = new RaptureURI(transformUri, Scheme.FIELDTRANSFORM);
		transform.setName(transformURI.getShortPath());
		RaptureFieldTransformStorage.add(transform, context.getUser(), "Add field transform");
	}

	@Override
	public void deleteFieldTransform(CallingContext context, String transformUri) {
		RaptureURI transformURI = new RaptureURI(transformUri, Scheme.FIELDTRANSFORM);
		RaptureFieldTransformStorage.deleteByAddress(transformURI, context.getUser(), "Remove field transform");
	}

	@Override
	public List<RaptureFolderInfo> getFieldTransformChildren(
			CallingContext context, String uriPrefix) {
		return RaptureFieldTransformStorage.getChildren(uriPrefix);
	}

	@Override
	public List<String> validateDocument(CallingContext context, String content, String structureUri) {
		ScriptedLoader loader = new ScriptedLoader(context);
		FieldEngine engine = new FieldEngine(loader, loader, loader, loader);
		return engine.validateDocument(content, structureUri);
	}
}
