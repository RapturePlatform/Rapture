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

import java.io.IOException;

import javax.servlet.ServletInputStream;

/**
 * A <code>LimitedServletInputStream</code> wraps another
 * <code>ServletInputStream</code> in order to keep track of how many bytes have
 * been read and detect when the Content-Length limit has been reached. This is
 * necessary since some servlet containers are slow to notice the end of stream
 * and cause the client code to hang if it tries to read past it.
 * 
 * @author Jason Hunter
 * @author Geoff Soutter
 * @version 1.0, 2000/10/27, initial revision
 */
public class LimitedServletInputStream extends ServletInputStream {

    /** input stream we are filtering */
    private ServletInputStream in;

    /** number of bytes to read before giving up */
    private int totalExpected;

    /** number of bytes we have currently read */
    private int totalRead = 0;

    /**
     * Creates a <code>LimitedServletInputStream</code> with the specified
     * length limit that wraps the provided <code>ServletInputStream</code>.
     */
    public LimitedServletInputStream(ServletInputStream in, int totalExpected) {
        this.in = in;
        this.totalExpected = totalExpected;
    }

    /**
     * Implement length limitation on top of the <code>read</code> method of the
     * wrapped <code>ServletInputStream</code>.
     * 
     * @return the next byte of data, or <code>-1</code> if the end of the
     *         stream is reached.
     * @exception IOException
     *                if an I/O error occurs.
     */
    @Override
    public int read() throws IOException {
        if (this.totalRead >= this.totalExpected) {
            return -1;
        }

        int result = this.in.read();
        if (result != -1) {
            this.totalRead++;
        }
        return result;
    }

    /**
     * Implement length limitation on top of the <code>read</code> method of the
     * wrapped <code>ServletInputStream</code>.
     * 
     * @param b
     *            destination buffer.
     * @param off
     *            offset at which to start storing bytes.
     * @param len
     *            maximum number of bytes to read.
     * @return the number of bytes read, or <code>-1</code> if the end of the
     *         stream has been reached.
     * @exception IOException
     *                if an I/O error occurs.
     */
    @Override
    public int read(byte b[], int off, int len) throws IOException {
        int result, left = this.totalExpected - this.totalRead;
        if (left <= 0) {
            return -1;
        } else {
            result = this.in.read(b, off, Math.min(left, len));
        }
        if (result > 0) {
            this.totalRead += result;
        }
        return result;
    }

    /**
     * Implement length limitation on top of the <code>readLine</code> method of
     * the wrapped <code>ServletInputStream</code>.
     * 
     * @param b
     *            an array of bytes into which data is read.
     * @param off
     *            an integer specifying the character at which this method
     *            begins reading.
     * @param len
     *            an integer specifying the maximum number of bytes to read.
     * @return an integer specifying the actual number of bytes read, or -1 if
     *         the end of the stream is reached.
     * @exception IOException
     *                if an I/O error occurs.
     */
    @Override
    public int readLine(byte b[], int off, int len) throws IOException {
        int result, left = this.totalExpected - this.totalRead;
        if (left <= 0) {
            return -1;
        } else {
            result = (this.in).readLine(b, off, Math.min(left, len));
        }
        if (result > 0) {
            this.totalRead += result;
        }
        return result;
    }
}
