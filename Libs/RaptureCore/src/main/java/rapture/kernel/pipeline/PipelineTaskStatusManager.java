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
package rapture.kernel.pipeline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;

import rapture.common.PipelineTaskStatus;
import rapture.common.RapturePipelineTask;
import rapture.common.TableQuery;
import rapture.common.TableQueryResult;
import rapture.common.TableRecord;
import rapture.common.exception.ExceptionToString;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.impl.jackson.JacksonUtilChecked;
import rapture.dsl.iqry.IndexQuery;
import rapture.dsl.iqry.IndexQueryFactory;
import rapture.index.IndexHandler;
import rapture.util.NetworkUtil;

public final class PipelineTaskStatusManager {

    private static String serverName = NetworkUtil.getServerName();
    private static String serverIp = NetworkUtil.getServerIP();

    private static final Logger log = Logger.getLogger(PipelineTaskStatusManager.class);

    private final IndexHandler indexHandler;

    public PipelineTaskStatusManager() {
        indexHandler = PipelineIndexHelper.createIndexHandler();
    }

    private static String getServerIdentifier() {
        return serverName == null ? serverIp : serverName;
    }

    // We actually just need content here (in the passed parameters)

    private static Map<String, Object> getRowDetails(RapturePipelineTask task) {
        Map<String, Object> ret = new HashMap<String, Object>();
        ret.put("content", JacksonUtil.jsonFromObject(task));
        return ret;
    }

    public void initialCreation(RapturePipelineTask task) {
        if (task.isStatusEnabled()) {
            task.getStatus().beginCreation(getServerIdentifier());
            indexHandler.updateRow(task.getTaskId(), getRowDetails(task));
        }
    }

    public void startRunning(RapturePipelineTask task) {
        if (task.isStatusEnabled()) {
            task.getStatus().beginRunning(getServerIdentifier());
            indexHandler.updateRow(task.getTaskId(), getRowDetails(task));
        }
    }

    private void finishRunning(RapturePipelineTask task, boolean successful) {
        if (task.isStatusEnabled()) {
            task.getStatus().endRunning(getServerIdentifier(), successful);
            if (log.isDebugEnabled()) {
                log.debug(String.format("Setting currentState of %s to %s", task.getTaskId(), task.getStatus().getCurrentState()));
            }
            indexHandler.updateRow(task.getTaskId(), getRowDetails(task));
        }
    }

    public PipelineTaskStatus getStatus(String taskId) {
        // TODO: Allow double or single quotes in table handler parsing
        // String query = "SELECT content WHERE rowId=\"" + taskId + "\"";
        String query = "SELECT content WHERE rowId='" + taskId + "'";
        TableQueryResult res = indexHandler.query(query);
        if (res != null && res.getRows().size() != 0) {
            String content = res.getRows().get(0).get(0).toString();
            RapturePipelineTask task = JacksonUtil.objectFromJson(content, RapturePipelineTask.class);
            return task.getStatus();
        } else {
            return null;
        }
    }

    public List<RapturePipelineTask> queryTasks(String query) {
        IndexQuery parsedQuery = IndexQueryFactory.parseQuery(query);
        List<RapturePipelineTask> tasks = new LinkedList<>();
        if (parsedQuery != null && parsedQuery.getSelect() != null && parsedQuery.getSelect().getFieldList() != null && parsedQuery.getSelect().getFieldList()
                .contains("content")) {
            TableQueryResult result = indexHandler.query(query);
            if (result != null) {
                for (List<Object> row : result.getRows()) {
                    Object content = row.get(0);
                    if (content != null) {
                        RapturePipelineTask task = null;
                        try {
                            task = JacksonUtilChecked.objectFromJson(content.toString(), RapturePipelineTask.class);
                        } catch (IOException e) {
                            log.error(ExceptionToString.format(e));
                        }
                        tasks.add(task);
                    } else {
                        log.error(String.format(
                                "Error reading result, the task query did not return content. Make sure the SELECT clause contains content in it. The query is "
                                        + "[%s]",
                                query));
                    }
                }
            }
        } else {
            throw RaptureExceptionFactory.create(HttpStatus.SC_BAD_REQUEST, String.format("The select query must contain a field named Content"));
        }
        return tasks;
    }

    public List<RapturePipelineTask> queryTasksOld(TableQuery query) {
        return convertToRapturePipelineTask(indexHandler.queryTable(query));
    }

    private static List<RapturePipelineTask> convertToRapturePipelineTask(List<TableRecord> records) {
        List<RapturePipelineTask> ret = new ArrayList<>();
        if (records != null && records.size() > 0) {
            for (TableRecord record : records) {
                ret.add(JacksonUtil.objectFromJson(record.getContent(), RapturePipelineTask.class));
            }
        }
        return ret;
    }

    public void suspendedRunning(RapturePipelineTask task) {
        if (task.isStatusEnabled()) {
            task.getStatus().suspended(getServerIdentifier());
            indexHandler.updateRow(task.getTaskId(), (Map<String, Object>) JacksonUtil.getHashFromObject(task));
        }
    }

    public void finishRunningWithSuccess(RapturePipelineTask task) {
        finishRunning(task, true);
    }

    public void finishRunningWithFailure(RapturePipelineTask task) {
        finishRunning(task, false);
    }

    public Long getLatestEpoch() {
        return indexHandler.getLatestEpoch();
    }
}
