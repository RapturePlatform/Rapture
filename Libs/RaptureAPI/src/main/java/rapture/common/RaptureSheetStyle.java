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
package rapture.common;

import rapture.common.sheet.RaptureSheetStyleAlignment;
import rapture.common.sheet.RaptureSheetStyleColor;
import rapture.common.sheet.RaptureSheetStyleNumberFormat;
import rapture.common.sheet.RaptureSheetStyleSize;
import rapture.common.sheet.RaptureSheetStyleWeight;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Note that this class is referenced in types.api - any changes to this file should be reflected there.
**/

public class RaptureSheetStyle {
    @JsonIgnore
    public String getDisplayDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(RaptureSheetStyleWeight.displayDescription(weight));
        if (sb.length() > 0) {
            sb.append(" ");
        }
        sb.append(RaptureSheetStyleAlignment.displayDescription(alignment));
        sb.append(RaptureSheetStyleColor.displayDescription(color));
        sb.append(RaptureSheetStyleSize.displayDescription(size));
        sb.append(RaptureSheetStyleNumberFormat.displayDescription(numberFormat));
        sb.append(" ");
        sb.append(numberFormatString);
        return sb.toString();
    }
    public RaptureSheetStyleWeight getWeight() {
        return weight;
    }
    public void setWeight(RaptureSheetStyleWeight weight) {
        this.weight = weight;
    }
    public RaptureSheetStyleAlignment getAlignment() {
        return alignment;
    }
    public void setAlignment(RaptureSheetStyleAlignment alignment) {
        this.alignment = alignment;
    }
    public RaptureSheetStyleColor getColor() {
        return color;
    }
    public void setColor(RaptureSheetStyleColor color) {
        this.color = color;
    }
    public RaptureSheetStyleSize getSize() {
        return size;
    }
    public void setSize(RaptureSheetStyleSize size) {
        this.size = size;
    }
    public RaptureSheetStyleNumberFormat getNumberFormat() {
        return numberFormat;
    }
    public void setNumberFormat(RaptureSheetStyleNumberFormat numberFormat) {
        this.numberFormat = numberFormat;
    }   
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    
    public String getNumberFormatString() {
        return numberFormatString;
    }
    public void setNumberFormatString(String numberFormatString) {
        this.numberFormatString = numberFormatString;
    }

    private String name = "";
    private String numberFormatString = "";
    private RaptureSheetStyleWeight weight = RaptureSheetStyleWeight.NORMAL;
    private RaptureSheetStyleAlignment alignment = RaptureSheetStyleAlignment.LEFT;
    private RaptureSheetStyleColor color = RaptureSheetStyleColor.BLACK;
    private RaptureSheetStyleSize size = RaptureSheetStyleSize.MEDIUM;
    private RaptureSheetStyleNumberFormat numberFormat = RaptureSheetStyleNumberFormat.NORMAL;
}
