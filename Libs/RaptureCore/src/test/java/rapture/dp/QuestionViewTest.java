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
package rapture.dp;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;

import rapture.common.dp.question.QTemplate;
import rapture.common.dp.question.Question;

import com.google.common.collect.ImmutableMap;

public class QuestionViewTest {

    @Test
    public void testNoVars() {
        Map<String,String> vars = ImmutableMap.of("var", "val");
        String before = "This has no var, so it should not change.";
        String after = QuestionView.expandVar(before, vars);
        assertEquals(before, after);
    }
    @Test
    public void testExpandAtStart() {
        Map<String,String> vars = ImmutableMap.of("var", "val");
        String expanded = QuestionView.expandVar("${var} is val", vars);
        assertEquals("val is val", expanded);
    }
    
    @Test
    public void testExpandAtEnd() {
        Map<String,String> vars = ImmutableMap.of("var", "val");
        String expanded = QuestionView.expandVar("val is ${var}", vars);
        assertEquals("val is val", expanded);
    }
    
    @Test
    public void testExpandRepeatedly() {
        Map<String,String> vars = ImmutableMap.of("var", "val");
        String expanded = QuestionView.expandVar("${var} is ${var}", vars);
        assertEquals("val is val", expanded);
    }
    
    @Test
    public void testExpandAdjacent() {
        Map<String,String> vars = ImmutableMap.of("var", "val");
        String expanded = QuestionView.expandVar("${var}${var}", vars);
        assertEquals("valval", expanded);
    }
    
    @Test
    public void testQTemplate() {
        QTemplate qt = new QTemplate();
        Question q = new Question();
        QuestionView qv = new QuestionView(q,qt);
        assertEquals(qt, qv.getQtemplate());
        assertEquals(q, qv.getQuestion());
    }
}
