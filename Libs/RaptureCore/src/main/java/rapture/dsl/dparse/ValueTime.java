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
package rapture.dsl.dparse;

import java.util.Calendar;

public class ValueTime {
    private Dimension dimension;

    private int amount;

    private int dimToCalField() {
        switch (dimension) {
        case YEAR:
            return Calendar.YEAR;
        case MONTH:
            return Calendar.MONTH;
        case WEEK:
            return Calendar.WEEK_OF_YEAR;
        case DAY:
            return Calendar.DAY_OF_MONTH;
        case HOUR:
            return Calendar.HOUR;
        case MINUTE:
            return Calendar.MINUTE;
        case SECOND:
            return Calendar.SECOND;
        default:
            break;
        }
        return Calendar.MILLISECOND;
    }

    public Dimension getDimension() {
        return dimension;
    }

    public void setAmount(String amount) {
        this.amount = Integer.parseInt(amount);
    }

    public void setDimension(Dimension dimension2) {
        this.dimension = dimension2;
    }

    public String toString() {
        return amount + " " + dimension;
    }

    public void windBack(Calendar testDate) {
        testDate.add(dimToCalField(), -1 * amount);
    }
}
