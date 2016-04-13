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
package rapture.common.exception;

/**
 * This exception should be thrown when we wish to deliver an exception to the
 * API.
 * 
 * @author bardhi
 * 
 */
public class RaptureException extends RuntimeException {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private String id;
    private Integer status;

    public String getId() {
        return id;
    }

    public Integer getStatus() {
        return status;
    }

    public RaptureException(String exceptionId, Integer status, String message) {
        super(message);
        this.id = exceptionId;
        this.status = status;
    }

    public RaptureException(String exceptionId, Integer status, String message, Throwable cause) {
        super(message, cause);
        this.id = exceptionId;
        this.status = status;
    }

    /**
     * 
     * 
     * @return Returns a formatted {@link RaptureException}, containing the id of
     *         this exception as well as complete stack trace, including the
     *         stack trace of any {@link Throwable} objects that caused this
     */
    public String getFormattedMessage() {
        return String.format("exceptionId %s (start):\n" + "---\n" + "%s\n" + "---\n" + "exceptionId %s (end)", getId(), ExceptionToString.format(this), getId());
    }
}
