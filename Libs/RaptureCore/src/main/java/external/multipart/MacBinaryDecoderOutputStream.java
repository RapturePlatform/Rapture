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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A <code>MacBinaryDecoderOutput</code> filters MacBinary files to normal files
 * on the fly; optimized for speed more than readability.
 * 
 * @author Jason Hunter
 */
public class MacBinaryDecoderOutputStream extends FilterOutputStream {
    private int bytesFiltered = 0;
    private int dataForkLength = 0;

    public MacBinaryDecoderOutputStream(OutputStream out) {
        super(out);
    }

    @Override
    public void write(byte b[]) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte b[], int off, int len) throws IOException {
        // If the write is for content past the end of the data fork, ignore
        if (this.bytesFiltered >= (128 + this.dataForkLength)) {
            this.bytesFiltered += len;
        }
        // If the write is entirely within the data fork, write it directly
        else if (this.bytesFiltered >= 128 && (this.bytesFiltered + len) <= (128 + this.dataForkLength)) {
            this.out.write(b, off, len);
            this.bytesFiltered += len;
        }
        // Otherwise, do the write a byte at a time to get the logic above
        else {
            for (int i = 0; i < len; i++) {
                write(b[off + i]);
            }
        }
    }

    @Override
    public void write(int b) throws IOException {
        // Bytes 83 through 86 are a long representing the data fork length
        // Check <= 86 first to short circuit early in the common case
        if (this.bytesFiltered <= 86 && this.bytesFiltered >= 83) {
            int leftShift = (86 - this.bytesFiltered) * 8;
            this.dataForkLength = this.dataForkLength | (b & 0xff) << leftShift;
        }

        // Bytes 128 up to (128 + dataForkLength - 1) are the data fork
        else if (this.bytesFiltered < (128 + this.dataForkLength) && this.bytesFiltered >= 128) {
            this.out.write(b);
        }

        this.bytesFiltered++;
    }
}
