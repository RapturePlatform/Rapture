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
package rapture.common.uri;

import java.net.HttpURLConnection;

import org.apache.log4j.Logger;

import rapture.common.RaptureURI;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;

/**
 * Contains some methods for parsing URIs
 * 
 * @author bardhi
 * 
 */
public class URIParser {

    private static final Logger log = Logger.getLogger(URIParser.class);

    /**
     * Parse a potential document URI into a URI string. If the potentialURI is
     * already a string, it leaves it untouched. Otherwise, it builds a string
     * that can be parsed as a URI
     * 
     * @param potentialUri
     * @return
     */
    public static String convertDocURI(String potentialUri) {
        if (potentialUri == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "Trying to push to a null URI!");
        } else {
            if (potentialUri.startsWith("//")) {
                return potentialUri;
            } else {
                try {
                    RaptureURI uri = new RaptureURI(potentialUri);
                    return uri.toString();
                } catch (RaptureException parseError) {
                    log.error("WARNING: Using deprecated URI: " + potentialUri);
                    return "//" + potentialUri.replace("/official", "");
                }
            }
        }
    }
}
