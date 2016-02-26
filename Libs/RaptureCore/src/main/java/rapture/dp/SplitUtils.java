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

import org.apache.log4j.Logger;

import rapture.common.dp.Step;
import rapture.common.dp.WorkOrder;
import rapture.common.dp.Worker;
import rapture.common.dp.WorkerExecutionState;
import rapture.common.dp.Workflow;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class SplitUtils {
    public static Logger log = Logger.getLogger(SplitUtils.class);
    
    public static String makeChildName(String stem, int index) {
        char last = stem.charAt(stem.length() - 1);
        return stem + (Character.isDigit(last) ? alpha(index) : index);
    }
    
    public static String getParentName(String name) {
        int cut = name.length() - 1;
        char last = name.charAt(cut);
        boolean digit = Character.isDigit(last);
        boolean digit2 = digit;
        while (digit == digit2) {
            cut--;
            if (cut < 0) return "";
            last = name.charAt(cut);
            digit2 = Character.isDigit(last);
        }
        return name.substring(0, cut+1);
    }

    /**
     * Encode a digit as capital letters, A=0, B=1, ..., Z=25, BA=26, BB=27, etc.
     */
    public static String alpha(int index) {
        if (index > 25) return alpha(index / 26) + alpha(index % 26);
        return Character.toString((char) ('A' + index));
    }

    public static Worker createSplitChild(Worker parent, Workflow flow, int index, int total, Step target) {
        Worker result = new Worker();
        result.setWorkOrderURI(parent.getWorkOrderURI());
        result.setId(makeChildName(parent.getId(), index));
        result.setSiblingPosition(index);
        result.setSiblingCount(total);
        result.setParent(parent.getId());
        result.setWaitCount(0);
        List<Map<String,String>> globalView = Lists.newArrayList();
        globalView.add(flow.getView());
        result.setLocalView(globalView);
        result.setEffectiveUser(parent.getEffectiveUser());
        result.setStatus(WorkerExecutionState.READY);
        result.setPriority(parent.getPriority());
        result.setCallingContext(parent.getCallingContext());
        if (target != null) {
            Map<String, String> viewWF = flow.getView();
            Map<String, String> viewS = target.getView();
            Map<String, String> composite = Maps.newHashMap();
            composite.putAll(viewWF);
            composite.putAll(viewS);
            result.getStack().add(flow.getWorkflowURI() + "#" + target.getName());
        }
        result.setActivityId(parent.getActivityId());
        result.setAppStatusNameStack(parent.getAppStatusNameStack());
        return result;
    }

    public static Worker createForkChild(WorkOrder workOrder, Worker parent, Workflow flow, int nextId, Step target) {
        Worker result = new Worker();
        result.setWorkOrderURI(workOrder.getWorkOrderURI());
        result.setId(""+nextId);
        result.setSiblingPosition(0);
        result.setSiblingCount(0);
        result.setParent("");
        result.setWaitCount(0);
        List<Map<String,String>> globalView = Lists.newArrayList();
        globalView.add(flow.getView());
        result.setLocalView(globalView);
        result.setEffectiveUser(parent.getEffectiveUser());
        result.setStatus(WorkerExecutionState.READY);
        result.setPriority(parent.getPriority());
        result.setCallingContext(parent.getCallingContext());
        if (target != null) {
            Map<String, String> viewWF = flow.getView();
            Map<String, String> viewS = target.getView();
            Map<String, String> composite = Maps.newHashMap();
            composite.putAll(viewWF);
            composite.putAll(viewS);
            result.getStack().add(flow.getWorkflowURI() + "#" + target.getName());
        }
        result.setActivityId(parent.getActivityId());
        result.setAppStatusNameStack(parent.getAppStatusNameStack());
        return result;
    }
}
