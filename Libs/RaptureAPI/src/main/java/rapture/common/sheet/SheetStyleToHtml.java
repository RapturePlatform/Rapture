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
package rapture.common.sheet;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rapture.common.RaptureSheetStyle;

/**
 * This class takes a sheet style and some content (a string) and converts it to html that represents that style applied to the content
 * @author amkimian
 *
 */
public class SheetStyleToHtml {
    public static String formatNumberContent(String content, RaptureSheetStyleNumberFormat numberFormat) {
        if (numberFormat == RaptureSheetStyleNumberFormat.NORMAL || content == null || content.isEmpty()) {
            return content;
        } else {
            // Convert content to a number then format that
            Double value = Double.valueOf(content);
            NumberFormat nf = NumberFormat.getInstance();
            switch(numberFormat) {
            case MONEY:
                nf = new DecimalFormat("#,##0.00;(#,##0.00)");
                break;
            case DECIMAL2:
                nf = new DecimalFormat("##0.00");
                break;
            case DECIMAL4:
                nf = new DecimalFormat("##0.0000");
                break;
            default:
                break;
            }
            return nf.format(value);
        }
    }
    
    public static String getStyledText(String content, RaptureSheetStyle style) {
        StringBuilder ret = new StringBuilder();
        List<Tag> tags = new ArrayList<Tag>();
        tags.add(new Tag("html"));
        switch(style.getWeight()) {
        case BOLD:
            tags.add(new Tag("b"));
            break;
        case ITALIC:
            tags.add(new Tag("i"));
            break;
        case BOLDITALIC:
            tags.add(new Tag("b"));
            tags.add(new Tag("i"));
        case NORMAL:
            // do nothing
            break;
        }
        switch(style.getAlignment()) {
        case LEFT:
            tags.add(new Tag("P","ALIGN=\"left\""));
            break;
        case CENTER:
            tags.add(new Tag("center"));
            break;
        case RIGHT:
            tags.add(new Tag("P","ALIGN=\"right\""));
            break;
        }
        Tag fontTag = new Tag("font");
        switch(style.getColor()) {
        case BLACK:
            break;
        case BLUE:
            fontTag.appendInner("color=\"Blue\"");
            break;
        case GREEN:
            fontTag.appendInner("color=\"Green\"");
            break;
        case RED:
            fontTag.appendInner("color=\"Red\"");
            break;
        }
        switch(style.getSize()) {
        case LARGE:
            fontTag.appendInner("size=\"6\"");
            break;
        case MEDIUM:
            break;
        case SMALL:
            fontTag.appendInner("size=\"2\"");
            break;
        }
        
        tags.add(fontTag);
        for(Tag t : tags) {
            ret.append(t.getStart());
        }
        ret.append(formatNumberContent(content, style.getNumberFormat()));
        Collections.reverse(tags);
        for(Tag t : tags) {
            ret.append(t.getEnd());
        }
        return ret.toString();
    }
}

class Tag {
    private String tag;
    private String inner = "";
    public Tag(String tag) {
        this.tag = tag;
    }
    public String getStart() {
        if (inner.isEmpty()) {
            return String.format("<%s>", tag);
        } else {
            return String.format("<%s %s>", tag, inner);
        }
    }
    public String getEnd() {
        return String.format("</%s>", tag);
    }
    public void appendInner(String string) {
        if (inner.isEmpty()) {
            inner = string;
        } else {
            inner = inner + " " + string;
        }
    }
    public Tag(String tag, String inner) {
        this.tag = tag;
        this.inner = inner;
    }
}
