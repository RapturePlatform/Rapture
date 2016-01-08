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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletInputStream;

/**
 * A <code>FilePart</code> is an upload part which represents a
 * <code>INPUT TYPE="file"</code> form parameter. Note that because file upload
 * data arrives via a single InputStream, each FilePart's contents must be read
 * before moving onto the next part. Don't try to store a FilePart object for
 * later processing because by then their content will have been passed by.
 * 
 * @author Geoff Soutter
 * @version 1.2, 2001/01/22, getFilePath() addition thanks to Stefan Eissing
 * @version 1.1, 2000/11/26, writeTo() bug fix thanks to Mike Shivas
 * @version 1.0, 2000/10/27, initial revision
 */
public class FilePart extends Part {

    /** "file system" name of the file */
    private String fileName;

    /** path of the file as sent in the request, if given */
    private String filePath;

    /** content type of the file */
    private String contentType;

    /** input stream containing file data */
    private PartInputStream partInput;

    /** file rename policy */
    @SuppressWarnings("unused")
    private FileRenamePolicy policy;

    /**
     * Construct a file part; this is called by the parser.
     * 
     * @param name
     *            the name of the parameter.
     * @param in
     *            the servlet input stream to read the file from.
     * @param boundary
     *            the MIME boundary that delimits the end of file.
     * @param contentType
     *            the content type of the file provided in the MIME header.
     * @param fileName
     *            the file system name of the file provided in the MIME header.
     * @param filePath
     *            the file system path of the file provided in the MIME header
     *            (as specified in disposition info).
     * 
     * @exception IOException
     *                if an input or output exception has occurred.
     */
    FilePart(String name, ServletInputStream in, String boundary, String contentType, String fileName, String filePath) throws IOException {
        super(name);
        this.fileName = fileName;
        this.filePath = filePath;
        this.contentType = contentType;
        this.partInput = new PartInputStream(in, boundary);
    }

    /**
     * Returns the content type of the file data contained within.
     * 
     * @return content type of the file data.
     */
    public String getContentType() {
        return this.contentType;
    }

    /**
     * Returns the name that the file was stored with on the remote system, or
     * <code>null</code> if the user didn't enter a file to be uploaded. Note:
     * this is not the same as the name of the form parameter used to transmit
     * the file; that is available from the <code>getName</code> method. Further
     * note: if file rename logic is in effect, the file name can change during
     * the writeTo() method when there's a collision with another file of the
     * same name in the same directory. If this matters to you, be sure to pay
     * attention to when you call the method.
     * 
     * @return name of file uploaded or <code>null</code>.
     * 
     * @see Part#getName()
     */
    public String getFileName() {
        return this.fileName;
    }

    /**
     * Returns the full path and name of the file on the remote system, or
     * <code>null</code> if the user didn't enter a file to be uploaded. If path
     * information was not supplied by the remote system, this method will
     * return the same as <code>getFileName()</code>.
     * 
     * @return path of file uploaded or <code>null</code>.
     * 
     * @see Part#getName()
     */
    public String getFilePath() {
        return this.filePath;
    }

    /**
     * Returns an input stream which contains the contents of the file supplied.
     * If the user didn't enter a file to upload there will be <code>0</code>
     * bytes in the input stream. It's important to read the contents of the
     * InputStream immediately and in full before proceeding to process the next
     * part. The contents will otherwise be lost on moving to the next part.
     * 
     * @return an input stream containing contents of file.
     */
    public InputStream getInputStream() {
        return this.partInput;
    }

    /**
     * Returns <code>true</code> to indicate this part is a file.
     * 
     * @return true.
     */
    @Override
    public boolean isFile() {
        return true;
    }

    /**
     * Puts in place the specified policy for handling file name collisions.
     */
    public void setRenamePolicy(FileRenamePolicy policy) {
        this.policy = policy;
    }

    /**
     * Internal method to write this file part; doesn't check to see if it has
     * contents first.
     * 
     * @return number of bytes written.
     * @exception IOException
     *                if an input or output exception has occurred.
     */
    long write(OutputStream out) throws IOException {
        // decode macbinary if this was sent
        if (this.contentType.equals("application/x-macbinary")) {
            out = new MacBinaryDecoderOutputStream(out);
        }
        long size = 0;
        int read;
        byte[] buf = new byte[8 * 1024];
        while ((read = this.partInput.read(buf)) != -1) {
            out.write(buf, 0, read);
            size += read;
        }
        return size;
    }

    /**
     * Write this file part to a file or directory. If the user supplied a file,
     * we write it to that file, and if they supplied a directory, we write it
     * to that directory with the filename that accompanied it. If this part
     * doesn't contain a file this method does nothing.
     * 
     * @return number of bytes written
     * @exception IOException
     *                if an input or output exception has occurred.
     */
    public long writeTo(File fileOrDirectory) {
        return 0L;
    }

    /**
     * Write this file part to the given output stream. If this part doesn't
     * contain a file this method does nothing.
     * 
     * @return number of bytes written.
     * @exception IOException
     *                if an input or output exception has occurred.
     */
    public long writeTo(OutputStream out) throws IOException {
        long size = 0;
        // Only do something if this part contains a file
        if (this.fileName != null) {
            // Write it out
            size = write(out);
        }
        return size;
    }
}
