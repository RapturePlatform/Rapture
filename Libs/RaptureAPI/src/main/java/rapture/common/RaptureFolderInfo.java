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
package rapture.common;

import rapture.common.RaptureURI.Parser;

/**
 * Note that this class is referenced in types.api - any changes to this file
 * should be reflected there.
 **/

public class RaptureFolderInfo implements RaptureTransferObject {

	public RaptureFolderInfo() {
		super();
	}

	public RaptureFolderInfo(String name, boolean folder) {
		this();
		this.name = name;
		this.folder = folder;
	}

	@Override
	public String toString() {
		return "RaptureFolderInfo [name=" + name + ", folder=" + folder + "]";
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isFolder() {
		return folder;
	}

	public void setFolder(boolean isFolder) {
		this.folder = isFolder;
	}

	// Remove any prepended path from the name
	public RaptureFolderInfo trimName() {
		if (name.charAt(name.length() - 1) == Parser.SEPARATOR_CHAR)
			name = name.substring(0, name.length() - 1);
		int last = name.lastIndexOf('/');
		if (last >= 0)
			name = name.substring(last + 1);
		return this;
	}

	private String name;
	private boolean folder;
}
