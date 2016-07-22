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
package rapture.common.dp;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.log4j.Logger;

import rapture.common.RaptureURI;
import rapture.common.exception.RaptureExceptionFactory;

public class StepHelper {

    public static final String RETURN_PREFIX = "$RETURN";
    public static final String JOIN_PREFIX = "$JOIN";
    public static final String FORK_PREFIX = "$FORK";
    public static final String SPLIT_PREFIX = "$SPLIT";
    public static final String RETURN_PREFIX_COLUMN = RETURN_PREFIX + ":";
    private static Logger log = Logger.getLogger(StepHelper.class);

    public static boolean isSpecialStep(String decoded) {
        return decoded.startsWith("$");
    }

    /**
     * this is for the subset of special steps that can appear as the target of a transition
     * 
     * @param decoded
     *            the decoded element from qualified workOrderURI
     */
    public static boolean isImpliedStep(String decoded) {
        return isReturnStep(decoded);
    }

    public static boolean isSpecialStep(Step step) {
        return isSpecialStep(decodedExecutable(step));
    }

    public static boolean isReturnStep(String decoded) {
        return decoded.startsWith(RETURN_PREFIX);
    }

    public static boolean isReturnStep(Step step) {
        return isReturnStep(decodedExecutable(step));
    }

    private static String decodedExecutable(Step step) {
        String raw = step.getExecutable();
        if ((raw == null) || (raw.length() == 0)) log.warn("No executable found for step " + step);
        else if (isSpecialStep(raw)) return raw;
        String authority = new RaptureURI(step.getExecutable()).getAuthority();
        return decode(authority);
    }

    public static String getReturnValue(Step step) {
        return decodedExecutable(step).replace(RETURN_PREFIX_COLUMN, "");
    }

    public static String encode(String rawName) {
        try {
            return URIUtil.encodeWithinQuery(rawName, CharEncoding.UTF_8);
        } catch (URIException e) {
            throw RaptureExceptionFactory.create(String.format("Error encoding step %s", rawName), e);
        }
    }

    public static String decode(String encoded) {
        try {
            return URIUtil.decode(encoded, CharEncoding.UTF_8);
        } catch (URIException e) {
            throw RaptureExceptionFactory.create(String.format("Error decoding step name %s:", encoded), e);
        }
    }
    
    public static boolean isSplitStep(Step step) {
        return isSplitStep(decodedExecutable(step));
    }
    
    public static boolean isSplitStep(String decoded) {
        return decoded.startsWith(SPLIT_PREFIX);
    }

    public static boolean isJoinStep(Step step) {
        return isJoinStep(decodedExecutable(step));
    }

    private static boolean isJoinStep(String decoded) {
        return decoded.startsWith(JOIN_PREFIX);
    }

    public static boolean isForkStep(Step step) {
        return isForkStep(decodedExecutable(step));
    }

    private static boolean isForkStep(String decoded) {
        return decoded.startsWith(FORK_PREFIX);
    }
}
