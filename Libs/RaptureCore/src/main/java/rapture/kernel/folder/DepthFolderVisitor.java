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
package rapture.kernel.folder;

public class DepthFolderVisitor extends BaseFolderVisitor {
    public DepthFolderVisitor(int depth, String prefix) {
        super(depth, prefix);
    }

    @Override
    public void folder(String folder) {
        // The depth indicates how far we want to go
        String[] p2 = folder.split("/");
        if (p2.length >= getDepth()) {
            StringBuilder toAdd = new StringBuilder();
            toAdd.append(getPrefix());
            for (int i = 0; i < p2.length && i < getDepth(); i++) {
                if (toAdd.length() != 0) {
                    toAdd.append("/");
                }
                toAdd.append(p2[i]);
            }
            addToSet(toAdd.toString());
        }
    }

}
