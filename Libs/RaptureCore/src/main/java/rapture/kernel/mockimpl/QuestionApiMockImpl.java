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
package rapture.kernel.mockimpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;

import rapture.common.CallingContext;
import rapture.common.Scheme;
import rapture.common.api.QuestionApi;
import rapture.common.dp.question.QNotification;
import rapture.common.dp.question.QTemplate;
import rapture.common.dp.question.QTemplateFactory;
import rapture.common.dp.question.Question;
import rapture.common.dp.question.QuestionFactory;
import rapture.common.dp.question.QuestionSearch;

public class QuestionApiMockImpl implements QuestionApi {

    private static Logger log = Logger.getLogger(QuestionApiMockImpl.class);
    private static Map<String, QTemplate> mockTemplateDb = new HashMap<String, QTemplate>();
    private static Map<String, Question> mockQuestionDb = new HashMap<String, Question>();
    private static Map<QTemplate, Question> qtmap = new HashMap<QTemplate, Question>();

    private static final QuestionApi instance = new QuestionApiMockImpl();

    public static QuestionApi getInstance() {
        return instance;
    }

    static {
        Random rand = new Random();
        Question question;
        QTemplate template;

        template = new QTemplateFactory().build();
        template = new QTemplateFactory().addQTemplateURI(Scheme.QTEMPLATE + "://" + rand.nextInt() + "/" + rand.nextInt()).addPrompt("Can I borrow $5")
                .addOption("Yes").addOption("No")
                .addOption("Different Amount").addFormElement("I'll only lend you", "Currency[EUR],Mandatory[Different Amount],Range[0.01-4.99]").build();

        question = new QuestionFactory(template).build();
        mockTemplateDb.put(template.getQtemplateURI(), template);
        mockQuestionDb.put(question.getQuestionURI(), question);
        qtmap.put(template, question);

        log.error(template.getQtemplateURI());
        log.error(question.getQuestionURI());

        template = new QTemplateFactory().addQTemplateURI(Scheme.QTEMPLATE + "://" + rand.nextInt() + "/" + rand.nextInt()).addPrompt("Is it raining")
                .addOption("Yes").addOption("No")
                .build();
        question = new QuestionFactory(template).build();
        mockTemplateDb.put(template.getQtemplateURI(), template);
        mockQuestionDb.put(question.getQuestionURI(), question);
        qtmap.put(template, question);

        log.error(template.getQtemplateURI());
        log.error(question.getQuestionURI());

        template = new QTemplateFactory().addQTemplateURI(Scheme.QTEMPLATE + "://" + rand.nextInt() + "/" + rand.nextInt()).addPrompt("Short question")
                .addOption("with a long rambling answer").addOption("with another long rambling answer").build();
        question = new QuestionFactory(template).build();
        mockTemplateDb.put(template.getQtemplateURI(), template);
        mockQuestionDb.put(question.getQuestionURI(), question);
        qtmap.put(template, question);

        log.error(template.getQtemplateURI());
        log.error(question.getQuestionURI());

        template = new QTemplateFactory().addQTemplateURI(Scheme.QTEMPLATE + "://" + rand.nextInt() + "/" + rand.nextInt())
                .addPrompt("Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor").addOption("Lorem ipsum dolor sit")
                .addOption("amet").build();
        question = new QuestionFactory(template).build();
        mockTemplateDb.put(template.getQtemplateURI(), template);
        mockQuestionDb.put(question.getQuestionURI(), question);
        qtmap.put(template, question);

        log.error(template.getQtemplateURI());
        log.error(question.getQuestionURI());

        template = new QTemplateFactory()
                .addQTemplateURI(Scheme.QTEMPLATE + "://" + System.currentTimeMillis() + "/" + rand.nextFloat())
                .addPrompt("What's Your Address")
                .addFormElement("House/Suite/Apt", "String,Mandatory")
                .addFormElement("Street", "String,Mandatory")
                .addFormElement("Street 2", "String,Optional")
                .addFormElement("City", "String,Mandatory")
                .addFormElement(
                        "State",
                        "SingleChoice[Alabama,Alaska,Arizona,Arkansas,California,Colorado,Connecticut,Delaware,"
                                + "Florida,Georgia,Hawaii,Idaho,Illinois,Indiana,Iowa,Kansas,Kentucky,Louisiana,Maine,Maryland,Massachusetts,Michigan,"
                                + "Minnesota,Mississippi,Missouri,Montana,Nebraska,Nevada,New Hampshire,New Jersey,New Mexico,New York,North Carolina,"
                                + "North Dakota,Ohio,Oklahoma,Oregon,Pennsylvania,Rhode Island,South Carolina,South Dakota,Tennessee,Texas,Utah,Vermont,"
                                + "Virginia,Washington,West Virginia,Wisconsin,Wyoming],Mandatory")
                .addFormElement("Zip Code", "Integer,Mandatory,Range[00000-99999]").build();
        question = new QuestionFactory(template).build();
        mockTemplateDb.put(template.getQtemplateURI(), template);
        mockQuestionDb.put(question.getQuestionURI(), question);
        qtmap.put(template, question);

        log.error(template.getQtemplateURI());
        log.error(question.getQuestionURI());
    }

    public QuestionApiMockImpl() {
        super();
    }

    @Override
    public void putTemplate(CallingContext context, String qTemplateURI, QTemplate template) {
        // Dummy method
    }

    @Override
    public QTemplate getTemplate(CallingContext context, String qTemplateURI) {
        QTemplate q = mockTemplateDb.get(qTemplateURI);
        log.debug("Lookup " + qTemplateURI + " returns " + (((q == null) ? "null" : q.getQtemplateURI())));
        return q;
    }

    @Override
    public Question getQuestion(CallingContext context, String questionURI) {
        Question q = mockQuestionDb.get(questionURI);
        log.debug("Lookup " + questionURI + " returns " + (((q == null) ? "null" : q.getQtemplateURI())));
        return q;
    }

    @Override
    public List<QNotification> getQNotifications(CallingContext context, QuestionSearch search) {
        List<QNotification> list = new ArrayList<QNotification>();
        // list.addAll(mockQuestionDb.values());
        return list;
    }

    @Override
    public void answerQuestion(CallingContext context, String questionURI, String response, Map<String, Object> data) {
        mockQuestionDb.get(questionURI);
    }

    @Override
    public String askQuestion(CallingContext context, String qTemplateURI, Map<String, String> variables, String callback) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<String> getQNotificationURIs(CallingContext context, QuestionSearch search) {
        // TODO Auto-generated method stub
        return null;
    }

}
