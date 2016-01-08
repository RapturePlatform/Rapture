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
package rapture.batch.kernel.handler;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import rapture.batch.kernel.handler.fields.RaptureFieldBandListFactory;
import rapture.batch.kernel.handler.fields.RaptureFieldPathSetFactory;
import rapture.common.CallingContext;
import rapture.common.RaptureField;
import rapture.common.RaptureFieldBand;
import rapture.common.RaptureFieldPath;
import rapture.common.RaptureGroupingFn;
import rapture.kernel.Kernel;

public class CreateFieldHandler extends BaseHandler {
    private static Logger log = Logger.getLogger(CreateFieldHandler.class);

    @Override
    public void execute(CallingContext ctx, Map<String, String> params, OutputStream output) {
        if (validateParamsPresent(params, new String[] { HandlerConstants.NAME, HandlerConstants.AUTHORITY, HandlerConstants.CATEGORY,
                HandlerConstants.DESCRIPTION, HandlerConstants.UNITS, HandlerConstants.GROUPINGFN, HandlerConstants.BANDS, HandlerConstants.FIELDPATHS,
                HandlerConstants.TYPE })) {

            String authority = params.get(HandlerConstants.AUTHORITY);
            String name = params.get(HandlerConstants.NAME);
            String category = params.get(HandlerConstants.CATEGORY);
            String description = params.get(HandlerConstants.DESCRIPTION);
            String units = params.get(HandlerConstants.UNITS);
            RaptureGroupingFn fn = RaptureGroupingFn.valueOf(params.get(HandlerConstants.GROUPINGFN));
            List<RaptureFieldBand> fieldBands = RaptureFieldBandListFactory.createFrom(params.get(HandlerConstants.BANDS));
            Set<RaptureFieldPath> fieldPaths = RaptureFieldPathSetFactory.createFrom(params.get(HandlerConstants.FIELDPATHS));

            log.info("Setting up field through batch script");
            RaptureField field = new RaptureField();
            field.setName(name);
            field.setAuthority(authority);
            field.setCategory(category);
            field.setDescription(description);
            field.setUnits(units);
            field.setGroupingFn(fn);
            field.setBands(fieldBands);
            field.setFieldPaths(fieldPaths);
            log.info("Updating field " + name);
            Kernel.getFields().putField(ctx, field);
            log.info("Field updated");
        }
    }

}
