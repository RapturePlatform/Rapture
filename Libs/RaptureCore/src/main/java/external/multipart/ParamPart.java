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
// Copyright (C) 1999-2001 by Jason Hunter <jhunter_AT_acm_DOT_org>.
// All rights reserved.  Use of this class is limited.
// Please see the LICENSE for more information.

/*
 * Copyright 2010-2011 Alan Moore. All rights reserved.
 */
package external.multipart;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.servlet.ServletInputStream;

/**
 * A <code>ParamPart</code> is an upload part which represents a normal
 * <code>INPUT</code> (for example a non <code>TYPE="file"</code>) form
 * parameter.
 * 
 * @author Geoff Soutter
 * @author Jason Hunter
 * @version 1.1, 2002/04/30, added better encoding support, thanks to Changshin
 *          Lee
 * @version 1.0, 2000/10/27, initial revision
 */
public class ParamPart extends Part {

    /** contents of the parameter */
    private byte[] value;

    private String encoding;

    /**
     * Constructs a parameter part; this is called by the parser.
     * 
     * @param name
     *            the name of the parameter.
     * @param in
     *            the servlet input stream to read the parameter value from.
     * @param boundary
     *            the MIME boundary that delimits the end of parameter value.
     * @param encoding
     *            the byte-to-char encoding to use by default value.
     */
    ParamPart(String name, ServletInputStream in, String boundary, String encoding) throws IOException {
        super(name);
        this.encoding = encoding;

        // Copy the part's contents into a byte array
        PartInputStream pis = new PartInputStream(in, boundary);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
        byte[] buf = new byte[128];
        int read;
        while ((read = pis.read(buf)) != -1) {
            baos.write(buf, 0, read);
        }
        pis.close();
        baos.close();

        // save it for later
        this.value = baos.toByteArray();
    }

    /**
     * Returns the value of the parameter in as a string (using the
     * parser-specified encoding to convert from bytes) or the empty string if
     * the user entered no value for this parameter.
     * 
     * @return value of parameter as a string.
     */
    public String getStringValue() throws UnsupportedEncodingException {
        return getStringValue(this.encoding);
    }

    /**
     * Returns the value of the parameter in the supplied encoding or empty
     * string if the user entered no value for this parameter.
     * 
     * @return value of parameter as a string.
     */
    public String getStringValue(String encoding) throws UnsupportedEncodingException {
        return new String(this.value, encoding);
    }

    /**
     * Returns the value of the parameter as an array of bytes or a zero length
     * array if the user entered no value for this parameter.
     * 
     * @return value of parameter as raw bytes
     */
    public byte[] getValue() {
        return this.value;
    }

    /**
     * Returns <code>true</code> to indicate this part is a parameter.
     * 
     * @return true.
     */
    @Override
    public boolean isParam() {
        return true;
    }
}
