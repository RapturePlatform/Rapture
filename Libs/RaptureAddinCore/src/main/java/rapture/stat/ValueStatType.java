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
package rapture.stat;

import java.util.Map;

/**
 * A value stat type is used to take a series of values and an operation to
 * perform on that series to create a new value.
 * 
 * @author amkimian
 * 
 */
public class ValueStatType extends StatType {
    private long seconds;

    private ValueOperation operation;

    public ValueStatType(Map<String, String> config) {
        if (config.containsKey("seconds")) {
            seconds = Long.valueOf(config.get("seconds"));
        } else {
            seconds = 60L;
        }
        if (config.containsKey("operation")) {
            operation = ValueOperation.valueOf(config.get("operation"));
        } else {
            operation = ValueOperation.SUM;
        }
    }

    public ValueOperation getOperation() {
        return operation;
    }

    public long getSeconds() {
        return seconds;
    }

    public void setOperation(ValueOperation operation) {
        this.operation = operation;
    }

    public void setSeconds(long seconds) {
        this.seconds = seconds;
    }
}
