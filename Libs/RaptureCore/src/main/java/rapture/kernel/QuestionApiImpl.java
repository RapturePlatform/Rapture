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
package rapture.kernel;

import rapture.common.CallingContext;
import rapture.common.RapturePipelineTask;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.TableQueryResult;
import rapture.common.api.QuestionApi;
import rapture.common.dp.question.QDetail;
import rapture.common.dp.question.QNotification;
import rapture.common.dp.question.QNotificationStorage;
import rapture.common.dp.question.QTemplate;
import rapture.common.dp.question.QTemplateStorage;
import rapture.common.dp.question.Question;
import rapture.common.dp.question.QuestionSearch;
import rapture.common.dp.question.QuestionStorage;
import rapture.common.dp.question.ReplyProgress;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.mime.MimeDecisionProcessAnswer;
import rapture.dp.QuestionView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class QuestionApiImpl extends KernelBase implements QuestionApi {

    static Logger log = Logger.getLogger(QuestionApiImpl.class);

    public QuestionApiImpl(Kernel raptureKernel) {
        super(raptureKernel);
    }

    @Override
    public void putTemplate(CallingContext context, String qTemplateURI, QTemplate template) {
        if (!qTemplateURI.equals(template.getQtemplateURI())) {
            throw RaptureExceptionFactory.create("URI does not match inserted object");
        }
        QTemplateStorage.add(template, context.getUser(), "Define");
    }

    @Override
    public QTemplate getTemplate(CallingContext context, String qTemplateURI) {
        RaptureURI address = new RaptureURI(qTemplateURI, Scheme.QTEMPLATE);
        QTemplate template = QTemplateStorage.readByAddress(address);
        if (template == null) {
            throw RaptureExceptionFactory.create("Template not found: " + qTemplateURI);
        }
        return template;
    }

    @Override
    public Question getQuestion(CallingContext context, String questionURI) {
        RaptureURI address = new RaptureURI(questionURI, Scheme.QTEMPLATE);
        Question question = QuestionStorage.readByAddress(address);
        if (question == null) {
            throw RaptureExceptionFactory.create("Question not found: " + questionURI);
        }
        return question;
    }

    @Override
    public List<QNotification> getQNotifications(CallingContext context, QuestionSearch search) {
        List<String> uris = getQNotificationURIs(context, search);
        List<QNotification> matches = new ArrayList<QNotification>();
        if (uris != null) for (String uri : uris)
            matches.add(QNotificationStorage.readByAddress(new RaptureURI(uri)));

        return matches;
    }

    @Override
    public List<String> getQNotificationURIs(CallingContext context, QuestionSearch search) {
        List<String> uris = new ArrayList<String>();

        String user = search.getUser();
        ReplyProgress progress = search.getProgress();

        // Seems rather silly to have to build a string that's going to get parsed.
        // Would be so much easier to do it like this.
        // But right now this has dependency issues

        // IndexQuery indexQuery = new IndexQuery().setSelect("qnotificationURI");
        // if (user != null)
        // indexQuery.addWhereStatement(WhereJoiner.AND, new WhereStatement("addressee", WhereTest.EQUAL, new WhereValue(user)));
        //
        // if (progress != null)
        // indexQuery.addWhereStatement(WhereJoiner.AND, new WhereStatement("progress", WhereTest.EQUAL, new WhereValue(progress)));

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT qnotificationURI, addressee, progress WHERE ");
        if (user != null) sb.append("addressee=\"").append(user).append("\"");

        if (progress != null) {
            if (user != null) sb.append(" AND ");
            sb.append("progress=\"").append(progress).append("\"");
        }
        String indexQuery = sb.toString();

        TableQueryResult results = QNotificationStorage.queryIndex(indexQuery);
        if (results != null) {
            for (List<Object> row : results.getRows())
                if (row != null) {
                    Object obj = row.get(0);
                    if (obj != null) uris.add(row.get(0).toString());
                }
        }

        return uris;
    }

    @Override
    public void answerQuestion(CallingContext context, String questionURI, String response, Map<String, Object> data) {
        if (response == null) {
            throw RaptureExceptionFactory.create("Response may not be null");
        }
        RaptureURI qURI = new RaptureURI(questionURI, Scheme.QUESTION);
        Question q = QuestionStorage.readByAddress(qURI);
        if (q == null) {
            throw RaptureExceptionFactory.create("No such question " + questionURI);
        }
        RaptureURI qtURI = new RaptureURI(q.getQtemplateURI(), Scheme.QTEMPLATE);
        QTemplate qt = QTemplateStorage.readByAddress(qtURI);
        if (qt == null) {
            throw RaptureExceptionFactory.create("Template for question is missing");
        }

        QuestionView qv = new QuestionView(q, qt);
        String qnotificationURI = makeNotificationURI(extractCallback(questionURI), context.getUser());
        QNotification qn = QNotificationStorage.readByFields(qnotificationURI);
        if (qn == null) {
            throw RaptureExceptionFactory.create("No QNotification for URI: " + qnotificationURI);
        }
        if (qn.getProgress() == ReplyProgress.FINISHED) {
            throw RaptureExceptionFactory.create("Duplicate Response rejected");
        }

        qn.setFormData(data);
        qn.setResponse(response);
        qn.setProgress(ReplyProgress.FINISHED);
        QNotificationStorage.add(qn, context.getUser(), "Respond");

        RapturePipelineTask task = new RapturePipelineTask();
        task.setPriority(qv.getPriority());
        task.setCategoryList(qv.getCategoryList());
        task.addMimeObject(qn);
        task.setContentType(MimeDecisionProcessAnswer.getMimeType());
        task.initTask();
        Kernel.getPipeline().publishMessageToCategory(context, task);
    }

    @Override
    public String askQuestion(CallingContext context, String qTemplateURI, Map<String, String> variables, String callback) {
        if ((callback == null) || (callback.indexOf('/') >= 0)) {
            throw RaptureExceptionFactory.create("invalid callback: " + callback);
        }
        RaptureURI qtURI = new RaptureURI(qTemplateURI, Scheme.QTEMPLATE);
        QTemplate qt = QTemplateStorage.readByAddress(qtURI);
        if (qt == null) {
            throw RaptureExceptionFactory.create("Failed to load question template from " + qTemplateURI);
        }
        String questionURI = makeQuestionURI(callback);
        postQuestion(context, qt, questionURI, variables);
        return questionURI;
    }

    public void postQuestion(CallingContext context, QTemplate qt, String questionURI, Map<String, String> variables) {
        Question q = new Question();
        q.setMapping(variables);
        q.setProgress(ReplyProgress.UNSTARTED);
        q.setQtemplateURI(qt.getQtemplateURI());
        q.setQuestionURI(questionURI);
        QuestionView qv = new QuestionView(q, qt);
        QuestionStorage.add(q, context.getUser(), "ask question");
        for (String name : qv.getQuorum()) {
            createAndStoreNotification(qv, name);
        }
    }

    public static Set<String> getVariablesFromTemplate(QTemplate qt) {
        Set<String> result = Sets.newHashSet();
        extractVarNames(qt.getPrompt(), result);
        for (String option : qt.getOptions()) {
            extractVarNames(option, result);
        }
        for (QDetail qd : qt.getForm()) {
            extractVarNames(qd, result);
        }
        return result;
    }

    public static void extractVarNames(String in, Set<String> out) {
        for (int i = 0; i < in.length(); i++) {
            if ('$' == in.charAt(i)) {
                i++;
                switch (in.charAt(i)) {
                    case '$':
                        continue;
                    case '{':
                        i++;
                        String clip = in.substring(i);
                        int len = clip.indexOf('}');
                        if (len >= 0) {
                            i += len + 1;
                            String name = clip.substring(0, len);
                            out.add(name);
                            break;
                        }
                    default:
                        log.warn("Incomplete variable specification (character " + i + "):" + in);
                }
            }
        }
    }

    private static void extractVarNames(QDetail in, Set<String> out) {
        extractVarNames(in.getPrompt(), out);
        extractVarNames(in.getKind(), out);
    }

    private void createAndStoreNotification(QuestionView qv, String name) {
        String callback = qv.getCallback();
        String qnotificationURI = makeNotificationURI(callback, name);
        QNotification qn = new QNotification();
        long now = System.currentTimeMillis();
        Long timeout = qv.getQtemplate().getTimeout();
        if (timeout == null) {
            timeout = Long.MAX_VALUE;
        } else {
            timeout += now;
            if (timeout <= now) { // overflow
                timeout = Long.MAX_VALUE; // ie never
            }
        }
        qn.setWhenAsked(now);
        qn.setDeadline(timeout);
        qn.setAddressee(name);
        qn.setProgress(ReplyProgress.UNSTARTED);
        qn.setQuestionURI(qv.getQuestionURI());
        qn.setQnotificationURI(qnotificationURI);

        QNotificationStorage.add(qn, "I don't know what goes here", "or here");
    }

    private String makeNotificationURI(String callback, String name) {
        return "qnotification://sys.questions/" + callback + "/" + name;
    }

    private String makeQuestionURI(String callback) {
        // WARNING: Don't change this without a matching change to
        // QuestionView::getCallback
        return "question://sys.questions/" + callback;
    }

    private String extractCallback(String questionURI) {
        int index = questionURI.lastIndexOf('/');
        if (index < 0) {
            throw RaptureExceptionFactory.create("Invalid Question URI");
        }
        return questionURI.substring(index + 1);
    }

    public String askWorkflowQuestion(CallingContext context, String qTemplateURI, String workerURI, String callback) {
        if ((callback == null) || (callback.indexOf('/') >= 0)) {
            throw RaptureExceptionFactory.create("invalid callback: " + callback);
        }
        RaptureURI qtURI = new RaptureURI(qTemplateURI, Scheme.QTEMPLATE);
        QTemplate qt = QTemplateStorage.readByAddress(qtURI);
        if (qt == null) {
            throw RaptureExceptionFactory.create("Failed to load question template from " + qTemplateURI);
        }
        String questionURI = makeQuestionURI(callback);
        Map<String, String> variables = lookupVariables(context, workerURI, qt);
        postQuestion(context, qt, questionURI, variables);
        return questionURI;
    }

    private static Map<String, String> lookupVariables(CallingContext context, String workerURI, QTemplate qt) {
        Map<String, String> map = Maps.newHashMap();
        for (String name : getVariablesFromTemplate(qt)) {
            map.put(name, Kernel.getDecision().getContextValue(context, workerURI, name));
        }
        return map;
    }
}
