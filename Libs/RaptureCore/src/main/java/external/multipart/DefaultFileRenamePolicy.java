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
// Copyright (C) 2002 by Jason Hunter <jhunter_AT_acm_DOT_org>.
// All rights reserved.  Use of this class is limited.
// Please see the LICENSE for more information.

/*
 * Copyright 2010-2011 Alan Moore. All rights reserved.
 */
package external.multipart;

import java.io.File;
import java.io.IOException;

/**
 * Implements a renaming policy that adds increasing integers to the body of any
 * file that collides. For example, if foo.gif is being uploaded and a file by
 * the same name already exists, this logic will rename the upload foo1.gif. A
 * second upload by the same name would be foo2.gif. Note that for safety the
 * rename() method creates a zero-length file with the chosen name to act as a
 * marker that the name is taken even before the upload starts writing the
 * bytes.
 * 
 * @author Jason Hunter
 * @version 1.1, 2002/11/05, making thread safe with createNewFile()
 * @version 1.0, 2002/04/30, initial revision, thanks to Yoonjung Lee for this
 *          idea
 */
public class DefaultFileRenamePolicy implements FileRenamePolicy {

    private boolean createNewFile(File f) {
        try {
            return f.createNewFile();
        } catch (IOException ignored) {
            return false;
        }
    }

    // This method does not need to be synchronized because createNewFile()
    // is atomic and used here to mark when a file name is chosen
    @Override
    public File rename(File f) {
        if (createNewFile(f)) {
            return f;
        }
        String name = f.getName();
        String body = null;
        String ext = null;

        int dot = name.lastIndexOf(".");
        if (dot != -1) {
            body = name.substring(0, dot);
            ext = name.substring(dot); // includes "."
        } else {
            body = name;
            ext = "";
        }

        // Increase the count until an empty spot is found.
        // Max out at 9999 to avoid an infinite loop caused by a persistent
        // IOException, like when the destination dir becomes non-writable.
        // We don't pass the exception up because our job is just to rename,
        // and the caller will hit any IOException in normal processing.
        int count = 0;
        while (!createNewFile(f) && count < 9999) {
            count++;
            String newName = body + count + ext;
            f = new File(f.getParent(), newName);
        }

        return f;
    }
}
