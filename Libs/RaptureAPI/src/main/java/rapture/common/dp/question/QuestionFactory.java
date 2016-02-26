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
package rapture.common.dp.question;

import java.util.List;
import java.util.Map;
import java.util.Random;

import com.google.common.collect.Maps;

import rapture.common.RaptureURI;
import rapture.common.Scheme;

/**
 * Builder for Question objects
 * 
 * @author David Tong <dave.tong@incapturetechnologies.com>
 * 
 */
public class QuestionFactory {

    private Question built;
    Random rand = new Random();

    public QuestionFactory() {
        init();
    }

    /**
     * Clone an existing template
     * 
     * @param template
     */
    public QuestionFactory(QTemplate template) {
        init();
        built.setQtemplateURI(template.getQtemplateURI());
        built.setProgress(ReplyProgress.UNSTARTED);
        addMapping(template.getForm());
    }

    /**
     * Allow factory to be reused
     * 
     * @return
     */
    public QuestionFactory init() {
        built = new Question();
        built.setQuestionURI(Scheme.QUESTION + "://" + rand.nextInt()+"/"+rand.nextInt());
        built.setMapping(Maps.<String,String>newHashMap());
        built.setProgress(ReplyProgress.UNSTARTED);
        return this;
    }

    public QuestionFactory addQTemplateURI(RaptureURI uri) {
        built.setQtemplateURI(uri.toString());
        return this;
    }

    public QuestionFactory addQTemplateURI(String uri) {
        built.setQtemplateURI(uri);
        return this;
    }

    public QuestionFactory addProgress(ReplyProgress progress) {
        built.setProgress(progress);
        return this;
    }

    public QuestionFactory addMapping(String key, String value) {
        built.getMapping().put(key, value);
        return this;
    }

    public QuestionFactory addMapping(Map<String, String> mapping) {
        built.getMapping().putAll(mapping);
        return this;
    }

    public QuestionFactory addMapping(List<QDetail> mapping) {
        for (QDetail q : mapping)
            addMapping(q);
        return this;
    }

    public QuestionFactory addMapping(QDetail q) {
        built.getMapping().put(q.getPrompt(), q.getKind());
        return this;
    }

    /**
     * Get the built template
     * 
     * @return
     */
    public Question build() {
        // TODO Perform validation
        return built;
    }

}
