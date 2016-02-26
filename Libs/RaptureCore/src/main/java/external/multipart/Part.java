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
// Copyright (C) 1999-2001 by Jason Hunter <jhunter_AT_acm_DOT_org>.
// All rights reserved.  Use of this class is limited.
// Please see the LICENSE for more information.

/*
 * Copyright 2010-2011 Alan Moore. All rights reserved.
 */
package external.multipart;

/**
 * A <code>Part</code> is an abstract upload part which represents an
 * <code>INPUT</code> form element in a <code>multipart/form-data</code> form
 * submission.
 * 
 * @see FilePart
 * @see ParamPart
 * 
 * @author Geoff Soutter
 * @version 1.0, 2000/10/27, initial revision
 */
public abstract class Part {
    private String name;

    /**
     * Constructs an upload part with the given name.
     */
    Part(String name) {
        this.name = name;
    }

    /**
     * Returns the name of the form element that this Part corresponds to.
     * 
     * @return the name of the form element that this Part corresponds to.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns true if this Part is a FilePart.
     * 
     * @return true if this is a FilePart.
     */
    public boolean isFile() {
        return false;
    }

    /**
     * Returns true if this Part is a ParamPart.
     * 
     * @return true if this is a ParamPart.
     */
    public boolean isParam() {
        return false;
    }
}
