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
package rapture.kernel.relationship;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import rapture.common.CallingContext;
import rapture.kernel.Kernel;

/**
 * Oh the irony
 * @author alanmoore
 *
 */
public class RelationshipHelper {
    @SuppressWarnings("unused")
    private static Logger log = Logger.getLogger(RelationshipHelper.class);
    public static void createRelationship(CallingContext ctx, String to, String label, Map<String, String> properties) {
        String from = Kernel.getStackContainer().peekStack(ctx);
        if (from != null) {
            //log.info("Logging relationship from " + from + " to " + to);
            Kernel.getRelationship().getTrusted().createRelationship(ctx, "//sysrel", from, to, label, properties == null ? new HashMap<String, String>() : properties);
        } else {
            //      log.info("No context for " + to);
        }
    }
}
