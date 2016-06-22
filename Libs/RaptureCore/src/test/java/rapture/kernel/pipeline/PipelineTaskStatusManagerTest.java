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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

import rapture.common.RapturePipelineTask;
import rapture.common.dp.Worker;
import rapture.common.mime.MimeDecisionProcessAdvance;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

/**
 * @author bardhi
 * @since 4/28/15.
 */
public class PipelineTaskStatusManagerTest {

    private String id1;
    private String id2;
    private String id3;
    private PipelineTaskStatusManager statusManager;

    @Before
    public void setUp() throws Exception {
        Kernel.INSTANCE.restart();
        Kernel.initBootstrap();
        statusManager = new PipelineTaskStatusManager();

        RapturePipelineTask task1 = createTask();
        id1 = task1.getTaskId();
        statusManager.initialCreation(task1);

        RapturePipelineTask task2 = createTask();
        id2 = task2.getTaskId();
        statusManager.initialCreation(task2);

        RapturePipelineTask task3 = createTask(false);
        id3 = task3.getTaskId();
        statusManager.initialCreation(task3);
    }

    @After
    public void tearDown() throws Exception {
        Kernel.INSTANCE.restart();
    }

    private RapturePipelineTask createTask(boolean statusEnabled) {
        RapturePipelineTask task = new RapturePipelineTask();
        Worker worker = new Worker();
        worker.setPriority(2);
        task.setPriority(worker.getPriority());
        task.setCategoryList(ImmutableList.of("cat"));
        task.addMimeObject(worker);
        task.setContentType(MimeDecisionProcessAdvance.getMimeType());
        task.setStatusEnabled(statusEnabled);
        task.initTask();
        Kernel.getPipeline().publishMessageToCategory(ContextFactory.getKernelUser(), task);
        return task;
    }

    private RapturePipelineTask createTask() {
        return createTask(true);
    }

    @Test
    public void testQueryTasks() throws Exception {

        List<RapturePipelineTask> result = statusManager.queryTasks("SELECT content");
        assertEquals(2, result.size());
        RapturePipelineTask p1 = result.get(0);
        RapturePipelineTask p2 = result.get(1);
        if (p1.getTaskId().equals(id1)) {
            assertEquals(id2, p2.getTaskId());
        } else if (p2.getTaskId().equals(id1)) {
            assertEquals(id2, p1.getTaskId());
        }

        assertEquals(1, p1.getCategoryList().size());
        assertEquals(1, p2.getCategoryList().size());
        assertEquals("cat", p1.getCategoryList().get(0));
        assertEquals(MimeDecisionProcessAdvance.getMimeType(), p1.getContentType());

        assertEquals(p1.getContent(), p2.getContent());
        assertTrue(p1.isStatusEnabled());
        assertTrue(p2.isStatusEnabled());
    }
}