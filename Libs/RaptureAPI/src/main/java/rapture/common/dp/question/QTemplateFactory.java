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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import rapture.common.RaptureURI;
import rapture.common.Scheme;

/**
 * Builder for QTemplate objects
 * @author David Tong <dave.tong@incapturetechnologies.com>
 *
 */
public class QTemplateFactory {
    private QTemplate built;

    public QTemplateFactory() {
        init();
    }

    /** Clone an existing template
     * 
     * @param template
     */
    public QTemplateFactory(QTemplate template) {
        built = new QTemplate();
        built.setPrompt(template.getPrompt());
        built.setRule(template.getRule());
        built.setQtemplateURI(template.getQtemplateURI());
        
        List<String> quorum = new ArrayList<String>();
        quorum.addAll(template.getQuorum());
        built.setQuorum(quorum);
        
        List<QDetail> form = new ArrayList<QDetail>();
        form.addAll(template.getForm());
        built.setForm(form);
    }
    
    /**
     * Allow factory to be reused
     * @return
     */
    public QTemplateFactory init() {
        built = new QTemplate();
        built.setPrompt("UNDEFINED");
        built.setQuorum(new ArrayList<String>());
        built.setForm(new ArrayList<QDetail>());
        built.setRule(AnswerRule.FIRST);
        built.setQtemplateURI(Scheme.QTEMPLATE+"://");
        built.setOptions(new ArrayList<String>());
        return this;
    }
    
    public QTemplateFactory addQTemplateURI(RaptureURI uri) {
        built.setQtemplateURI(uri.toString());
        return this;
    }
    
    public QTemplateFactory addQTemplateURI(String uri) {
        built.setQtemplateURI(uri);
        return this;
    }
    
    public QTemplateFactory addRule(AnswerRule rule) {
        built.setRule(rule);
        return this;
    }
    
    public QTemplateFactory addPrompt(String prompt) {
        built.setPrompt(prompt);
        return this;
    }
    
    public QTemplateFactory addQuorum(String quorum) {
        built.getQuorum().add(quorum);
        return this;
    }
    
    public QTemplateFactory addQuorum(Collection<String> quora) {
        built.getQuorum().addAll(quora);
        return this;
    }
    
    public QTemplateFactory addOption(String option) {
        built.getOptions().add(option);
        return this;
    }
    
    public QTemplateFactory addOption(Collection<String> option) {
        built.getOptions().addAll(option);
        return this;
    }
    
    public QTemplateFactory addFormElement(QDetail qDetail) {
        built.getForm().add(qDetail);
        return this;
    }
    
    public QTemplateFactory addFormElement(Collection<QDetail> qDetails) {
        built.getForm().addAll(qDetails);
        return this;
    }
    
    public QTemplateFactory addFormElement(String prompt, String kind) {
        QDetail qDetail = new QDetail();
        qDetail.setPrompt(prompt);
        qDetail.setKind(kind);
        built.getForm().add(qDetail);
        return this;
    }
    
    /**
     * Get the built template
     * @return
     */
    public QTemplate build() {
        // TODO Perform validation
        return built;
    }

    public static void main(String[] args) {
        QTemplate template = new QTemplateFactory().build();
        template = new QTemplateFactory().addPrompt("Can I borrow $5").addOption("Yes").addOption("No").addOption("Different Amount").addFormElement("I'll only lend you $", "Integer,Optional").build();
        System.out.println(template.debug());
        template = new QTemplateFactory().addPrompt("Is it raining").addOption("Yes").addOption("No").build();
        System.out.println(template.debug());
        template = new QTemplateFactory().addPrompt("What's Your Address")
                .addFormElement("House/Suite/Apt", "String,Mandatory")
                .addFormElement("Street", "String,Mandatory")
                .addFormElement("Street 2", "String,Optional")
                .addFormElement("City", "String,Mandatory")
                .addFormElement("State","SingleChoice,Mandatory,[Alabama,Alaska,Arizona,Arkansas,California,Colorado,Connecticut,Delaware,Florida,Georgia,Hawaii,Idaho,Illinois,Indiana,Iowa,Kansas,Kentucky,Louisiana,Maine,Maryland,Massachusetts,Michigan,Minnesota,Mississippi,Missouri,Montana,Nebraska,Nevada,New Hampshire,New Jersey,New Mexico,New York,North Carolina,North Dakota,Ohio,Oklahoma,Oregon,Pennsylvania,Rhode Island,South Carolina,South Dakota,Tennessee,Texas,Utah,Vermont,Virginia,Washington,West Virginia,Wisconsin,Wyoming]")
                .addFormElement("Zip Code", "Integer,Range[10000-99999]")
                .build();
        System.out.println(template.debug());
    }

}
