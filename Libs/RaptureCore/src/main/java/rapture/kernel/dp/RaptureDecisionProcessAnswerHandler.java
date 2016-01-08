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
package rapture.kernel.dp;

import static rapture.dp.DefaultDecisionProcessExecutor.getTransition;
import static rapture.dp.DefaultDecisionProcessExecutor.publishStep;
import static rapture.kernel.DecisionApiImpl.getStep;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import rapture.common.CallingContext;
import rapture.common.RaptureFolderInfo;
import rapture.common.RapturePipelineTask;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.dp.Step;
import rapture.common.dp.Transition;
import rapture.common.dp.Worker;
import rapture.common.dp.WorkerStorage;
import rapture.common.dp.Workflow;
import rapture.common.dp.WorkflowStorage;
import rapture.common.dp.question.AnswerRule;
import rapture.common.dp.question.QCallback;
import rapture.common.dp.question.QCallbackStorage;
import rapture.common.dp.question.QNotification;
import rapture.common.dp.question.QNotificationStorage;
import rapture.common.dp.question.QTemplate;
import rapture.common.dp.question.QTemplateStorage;
import rapture.common.dp.question.Question;
import rapture.common.dp.question.QuestionStorage;
import rapture.common.dp.question.ReplyProgress;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.dp.QuestionView;
import rapture.exchange.QueueHandler;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.kernel.pipeline.PipelineTaskStatusManager;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;

/**
 * When a response comes in to a question, this handler is responsible for aggregating the results and
 * advancing the decision process (if applicable)
 * 
 * @author mel
 */
public class RaptureDecisionProcessAnswerHandler implements QueueHandler {
    private static final Logger log = Logger.getLogger(RaptureDecisionProcessAnswerHandler.class);
    private final PipelineTaskStatusManager statusManager;

    public RaptureDecisionProcessAnswerHandler() {
        statusManager = new PipelineTaskStatusManager();
    }

    @Override
    public boolean handleMessage(String tag, String routing, String contentType, RapturePipelineTask task) {
        //TODO MEL Acquire the question lock (with try-finally safety)
        
        QNotification qn = JacksonUtil.objectFromJson(task.getContent(), QNotification.class);
        log.info("Processing Response for " + qn.getQuestionURI());
        RaptureURI qid = new RaptureURI(qn.getQuestionURI(), Scheme.QUESTION);
        Question q = QuestionStorage.readByAddress(qid);
        String selected;
        if (q.getProgress() != ReplyProgress.FINISHED) {
            RaptureURI qtuid = new RaptureURI(q.getQtemplateURI(), Scheme.QTEMPLATE);
            QTemplate qt = QTemplateStorage.readByAddress(qtuid);
            QuestionView qv = new QuestionView(q, qt);
            AnswerRule rule = qv.getAnswerRule();
            
            /* 
             * If there are enough responses to resolve the question, return the selected option. 
             * This matches the outbound transition name in the surrounding workflow
             */
            switch (rule) {
                case PLURALITY:
                    selected = pluralityWins(qv);
                    break;
                    
                case MAJORITY:
                    selected = majorityWins(qv);
                    break;
                    
                case FIRST:
                    selected = firstWins(qv);
                    break;
                    
                default:
                    log.error("Unknown Question Aggregation type");
                    statusManager.finishRunningWithFailure(task);
                    return true;
            }
            if (selected != null) {
                advanceWorkflow(qv, selected);
                markDone(qv, selected);
            }
        }
        statusManager.finishRunningWithSuccess(task);
        return true;
    }
    
    private void advanceWorkflow(QuestionView qv, String selected) {
        QCallback qc = QCallbackStorage.readByFields(qv.getCallback());
        RaptureURI uri = new RaptureURI(qc.getWorkerURI(), Scheme.WORKORDER);
        Worker worker = WorkerStorage.readByFields(uri.getShortPath(), uri.getElement());
        RaptureURI stepURI = new RaptureURI(worker.getStack().get(0), Scheme.WORKFLOW);
        String stepName = stepURI.getElement();
        Workflow workflow = WorkflowStorage.readByAddress(stepURI);
        Step fromStep = getStep(workflow, stepName);
        Transition t = getTransition(fromStep, selected);
        String targetStepName = t.getTargetStep();
        Step targetStep = getStep(workflow, targetStepName);
        updateStepInWorker(worker, stepURI, targetStepName);
        CallingContext context = ContextFactory.getKernelUser();
        WorkerStorage.add(worker, context.getUser(), "answered question");  
        String category = targetStep.getCategoryOverride();
        if (category == null) {
            category = workflow.getCategory();
        }
        publishStep(worker, category);
    }

    public static void updateStepInWorker(Worker worker, RaptureURI workflowURI, String targetStepName) {
        RaptureURI uri = RaptureURI.builder(workflowURI).element(targetStepName).build();
        worker.getStack().set(0, uri.toString());
        WorkerStorage.add(worker, ContextFactory.getKernelUser().getUser(), "update stack");
    }

    private String pluralityWins(QuestionView qv) {
        List<QNotification> qrs = getResponses(qv.getQuestionURI());
        int quorumSize = qv.getQuorum().size();
        int pending = quorumSize - qrs.size();
        if (pending >= quorumSize) return null;
        
        Multiset<String> counts = countResponses(qrs); 
        int topCount = 0;
        int closeCount = 0;
        String top = null;
        for (Multiset.Entry<String> entry: counts.entrySet()) {
            if (entry.getCount() > topCount) {
                top = entry.getElement();
                closeCount = topCount;
                topCount = entry.getCount();
            } else if (entry.getCount() > closeCount) {
                closeCount = entry.getCount();
            }
        }
        if (pending == 0 && topCount == closeCount) return "TIE_VOTE";
        return (topCount > pending + closeCount) ? top : null;
    }

    private String firstWins(QuestionView qv) {
        List<QNotification> qrs = getResponses(qv.getQuestionURI());
        if (qrs.isEmpty()) return null;
        QNotification qr = qrs.get(0);
        String selected = qr.getResponse();
        //TODO MEL put formData somewhere useful
        return selected;
    }
    
    private String majorityWins(QuestionView qv) {
        List<QNotification> qrs = getResponses(qv.getQuestionURI());
        int quorumSize = qv.getQuorum().size();
        int limit = (quorumSize + 1) / 2;
        if (qrs.size() < limit) return null;
        int pending = quorumSize - qrs.size();

        Multiset<String> counts = countResponses(qrs); 
        boolean majorityPossible = false;
        for (Multiset.Entry<String> entry: counts.entrySet()) {
            if (entry.getCount() >= limit) {
                String selected = entry.getElement();
                return selected;
            } else if (entry.getCount() + pending >= limit) {
                majorityPossible = true;
            }
        }
        return majorityPossible ? null : "MAJORITY_BLOCKED";
    }

    private Multiset<String> countResponses(List<QNotification> qrs) {
        Multiset<String> result = HashMultiset.create();
        for (QNotification qr : qrs) {
            result.add(qr.getResponse());
        }
        return result;
    }

    private void markDone(QuestionView qv, String selected) {
        Question q = qv.getQuestion();
        q.setAnswer(selected);
        q.setProgress(ReplyProgress.FINISHED);
        QuestionStorage.add(q, ContextFactory.getKernelUser().getUser(), "done");
    }

    private List<QNotification> getResponses(String questionURI) {
        List<QNotification> result = Lists.newArrayList();
        String base = makeNotificationParentURI(questionURI);
        Map<String, RaptureFolderInfo> dirMap = Kernel.getDoc().listDocsByUriPrefix(ContextFactory.getKernelUser(), base, 1);
        for(RaptureFolderInfo rfi : dirMap.values()){
                if (rfi.isFolder()) continue;
                RaptureURI qnURI = new RaptureURI(base + rfi.getName(), Scheme.QNOTIFICATION);
                QNotification qn = QNotificationStorage.readByAddress(qnURI);
                if (qn.getProgress() == ReplyProgress.FINISHED) {
                    result.add(qn);
                }
        }
        return result;
    }

    private String makeNotificationParentURI(String questionURI) {
        String callback = extractCallback(questionURI);
        return "qnotification://sys.questions/" + callback + "/";
    }
    
    private String extractCallback(String questionURI) {
        int index = questionURI.lastIndexOf('/');
        if (index < 0) {
            throw RaptureExceptionFactory.create("Invalid Question URI");
        }
        return questionURI.substring(index + 1);
    }
}
