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

import java.util.Map;

public class RelationshipQueryConfig {
    public Long getDepth() {
        return depth;
    }

    public Boolean getIncludeTo() {
        return includeTo;
    }

    public Boolean getIncludeFrom() {
        return includeFrom;
    }

    private Long depth;
    private Boolean includeTo;
    private Boolean includeFrom;
    private Long ageInSeconds;
    
    public RelationshipQueryConfig(Map<String, String> options) {
        if (options.containsKey("DEPTH")) {
            depth = Long.valueOf(options.get("DEPTH"));
        } else {
            depth = 2L;
        }
        if (options.containsKey("INCTO")) {
            includeTo = Boolean.valueOf(options.get("INCTO"));
        } else {
            includeTo = true;
        }
        if (options.containsKey("INCFROM")) {
            includeFrom = Boolean.valueOf(options.get("INCFROM"));
        } else {
            includeFrom = true;
        }
        if (options.containsKey("AGE")) {
            ageInSeconds = Long.valueOf(options.get("AGE"));
        } else {
            ageInSeconds = -1L;
        }
    }

    public Long getAgeInSeconds() {
        return ageInSeconds;
    }

}
