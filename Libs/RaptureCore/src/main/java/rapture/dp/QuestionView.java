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

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import rapture.common.dp.question.AnswerRule;
import rapture.common.dp.question.QTemplate;
import rapture.common.dp.question.Question;
import rapture.common.exception.RaptureExceptionFactory;

import com.google.common.collect.Lists;

public class QuestionView {
    private Question q;
    private QTemplate qt;
    
    static WeakHashMap<String, QuestionView> cache = new WeakHashMap<String, QuestionView>();
    
    public static QuestionView fetch(Question q, QTemplate qt) {
        String key = q.getQuestionURI();
        QuestionView qv = cache.get(key);
        if (qv == null) {
            qv = new QuestionView(q, qt);
            cache.put(key, qv);
        }
        return qv;
    }
    
    public QuestionView(Question q, QTemplate qt) {
        this.q = q;
        this.qt = qt;
    }

    public List<String> getQuorum() {
        return expandVars(qt.getQuorum(), q.getMapping());
    }
    
    public static List<String> expandVars(List<String> raw, Map<String, String> map) {
        List<String> result = Lists.newArrayList();
        for(String s: raw) {
            result.add(expandVar(s, map));
        }
        return result;
    }
    
    public static String expandVar(String raw, Map<String, String> map) {
        int nut = raw.indexOf("$");
        if (nut<0) return raw;
        StringBuilder sb = new StringBuilder();
        int bolt = 0;
        while (nut >= 0) {
            sb.append(raw.substring(bolt, nut));
            try {
                switch(raw.charAt(nut + 1)) {
                    case '$':
                        sb.append('$');
                        bolt = nut + 2;
                        break;
                    case '{':
                        int startVar = nut + 2;
                        int endVar = raw.indexOf('}', nut);
                        if (endVar < 0) {
                            throw RaptureExceptionFactory.create("'${' has no matching '}' in " + raw);
                        }   
                        String varName = raw.substring(startVar, endVar);
                        String val = map.get(varName);
                        if (val == null) {
                            throw RaptureExceptionFactory.create("Variable ${" + varName + "} required but missing");
                        }
                        sb.append(val);
                        bolt = endVar + 1;
                        break;
                }
            } catch (IndexOutOfBoundsException ex) {
                throw RaptureExceptionFactory.create("$ with no variable in " + raw);
            }    
            nut = raw.indexOf("$", bolt);
        }
        sb.append(raw.substring(bolt));
        return sb.toString();
    }
    
    public String getCallback() {
        //the callback appears after the last slash of the uri.  if we change that, this need to change too.
        String uri = q.getQuestionURI();
        int index = uri.lastIndexOf("/");
        return uri.substring(index + 1);
    }

    public String getQuestionURI() {
        return q.getQuestionURI();
    }

    public AnswerRule getAnswerRule() {
        return qt.getRule();
    }

    private Integer priority;
    public int getPriority() {
        if (priority == null) {
            try {
                priority = Integer.valueOf(expandVar(q.getPriority(), q.getMapping()));
            } catch (Exception ex) {
                priority = 0;
            }
        }
        return priority;
    }

    public List<String> getCategoryList() {
        // TODO Auto-generated method stub
        return null;
    }

    public Question getQuestion() {
        return q;
    }

    public QTemplate getQtemplate() {
        return qt;
    }
}
