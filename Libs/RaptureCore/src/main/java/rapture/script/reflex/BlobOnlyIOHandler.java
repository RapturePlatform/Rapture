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
package rapture.script.reflex;

import reflex.DefaultReflexIOHandler;
import reflex.IReflexIOHandler;
import reflex.ReflexException;
import reflex.value.ReflexArchiveFileValue;
import reflex.value.ReflexFileValue;
import reflex.value.ReflexValue;

public class BlobOnlyIOHandler extends DefaultReflexIOHandler implements IReflexIOHandler {

	@Override
	public void writeFile(ReflexFileValue asFile, String data) {
		throw new ReflexException(-1, "Not allowed");
	}

	@Override
	public ReflexValue isFile(ReflexFileValue asFile) {
		throw new ReflexException(-1, "Not allowed");
	}

	@Override
	public ReflexValue isFolder(ReflexFileValue asFile) {
		throw new ReflexException(-1, "Not allowed");
	}

	@Override
	public ReflexValue readdir(ReflexFileValue asFile) {
		throw new ReflexException(-1, "Not allowed");
	}

	@Override
	public ReflexValue getArchiveEntry(ReflexArchiveFileValue fVal) {
		throw new ReflexException(-1, "Not allowed");
	}

	@Override
	public void writeArchiveEntry(ReflexArchiveFileValue asArchive, ReflexValue value) {
		throw new ReflexException(-1, "Not allowed");
	}

	@Override
	public void close(ReflexArchiveFileValue asArchive) {
		throw new ReflexException(-1, "Not allowed");
	}

	@Override
	public void remove(ReflexFileValue toDelete) {
		throw new ReflexException(-1, "Not allowed");
	}

}
