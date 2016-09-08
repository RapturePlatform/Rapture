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

import rapture.common.CallingContext;
import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureProgram;
import rapture.common.RaptureProgramStorage;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.api.ProgramApi;

public class ProgramApiImpl extends KernelBase implements ProgramApi {

     public ProgramApiImpl(Kernel raptureKernel) {
        super(raptureKernel);
    }

 	@Override
 	public RaptureProgram getProgram(CallingContext context, String programUri) {
 		RaptureURI internalURI = new RaptureURI(programUri, Scheme.PROGRAM);
 		return RaptureProgramStorage.readByAddress(internalURI);
 	}

 	@Override
 	public void putProgram(CallingContext context, String programUri,
 			RaptureProgram program) {
 		RaptureURI internalURI = new RaptureURI(programUri, Scheme.PROGRAM);
		program.setName(internalURI.getShortPath());
		RaptureProgramStorage.add(program, context.getUser(), "Added program");
 	}

 	@Override
 	public void deleteProgram(CallingContext context, String programUri) {
 		RaptureURI internalURI = new RaptureURI(programUri, Scheme.PROGRAM);
 		RaptureProgramStorage.deleteByAddress(internalURI, context.getUser(), "Remove program");
 	}

 	@Override
 	public List<RaptureFolderInfo> getChildren(CallingContext context,
 			String uriPrefix) {
 		return RaptureProgramStorage.getChildren(uriPrefix);
 	}
 
}
