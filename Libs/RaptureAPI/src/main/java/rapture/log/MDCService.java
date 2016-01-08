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
package rapture.log;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.log4j.MDC;

import com.google.common.collect.ImmutableSet;

public enum MDCService {
    INSTANCE;

    static final String RFX_SCRIPT = "rfx";

    private static final Set<String> REFLEX_FIELDS;

    static {
        REFLEX_FIELDS = ImmutableSet.<String>builder().add(RFX_SCRIPT).build();
    }

    static final String WORKFLOW_FORMATTED = "workflow";

    static final String STEP_NAME = "stepName";
    static final String STEP_START_TIME = "stepStartTime";
    static final String WORK_ORDER_URI = "workOrderURI";
    static final String WORKER_ID = "workerId";

    private static final Set<String> WORK_ORDER_FIELDS;

    static {
        WORK_ORDER_FIELDS = ImmutableSet.<String>builder().add(WORK_ORDER_URI).add(WORKER_ID).build();
    }

    private static final Set<String> WORK_ORDER_STEP_FIELDS;

    static {
        WORK_ORDER_STEP_FIELDS = ImmutableSet.<String>builder().add(STEP_NAME).add(STEP_START_TIME).build();
    }

    private InheritableThreadLocal<Stack<Map<String, String>>> scriptStack = createStack();
    private InheritableThreadLocal<Stack<Map<String, String>>> workflowStack = createStack();
    private InheritableThreadLocal<Stack<Map<String, String>>> workflowStepStack = createStack();

    public synchronized void clearReflexMDC() {
        clearWithStack(REFLEX_FIELDS, scriptStack.get());
    }

    private void clearWithStack(Set<String> toRemove, Stack<Map<String, String>> stack) {
        for (String key : toRemove) {
            MDC.remove(key);
        }

        if (stack != null && !stack.isEmpty()) {
            stack.pop();
            if (!stack.isEmpty()) {
                Map<String, String> top = stack.peek();
                for (Map.Entry<String, String> entry : top.entrySet()) {
                    MDC.put(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    private InheritableThreadLocal<Stack<Map<String, String>>> createStack() {
        return new InheritableThreadLocal<Stack<Map<String, String>>>() {
            @Override
            protected Stack<Map<String, String>> childValue(Stack<Map<String, String>> parentValue) {
                Stack<Map<String, String>> stack = new Stack<Map<String, String>>();
                stack.addAll(Collections.unmodifiableCollection(parentValue));
                return stack;
            }

            @Override
            public Stack<Map<String, String>> initialValue() {
                return new Stack<Map<String, String>>();
            }

        };
    }

    public synchronized void setReflexMDC(String scriptName) {
        String formatted;
        if (scriptName == null) {
            formatted = "unknown_script";
        } else {
            formatted = scriptName;
        }
        Map<String, String> map = new HashMap<>();
        map.put(RFX_SCRIPT, formatted);
        Stack<Map<String, String>> stack = scriptStack.get();
        stack.push(map);
        MDC.put(RFX_SCRIPT, formatted);
    }

    public synchronized void setWorkOrderMDC(String workOrderURI, String workerId) {
        Map<String, String> map = new HashMap<>();
        map.put(WORK_ORDER_URI, workOrderURI);
        map.put(WORKER_ID, workerId);

        Stack<Map<String, String>> stack = workflowStack.get();
        stack.push(map);
        for (Map.Entry<String, String> entry : map.entrySet()) {
            MDC.put(entry.getKey(), entry.getValue());

        }
        updateFormattedWorkflow();
    }

    public synchronized void clearWorkOrderMDC() {
        clearWithStack(WORK_ORDER_FIELDS, workflowStack.get());
        updateFormattedWorkflow();
    }

    public synchronized void setWorkOrderStepMDC(String stepName, Long stepStartTime) {
        Map<String, String> map = new HashMap<>();
        map.put(STEP_NAME, stepName);
        map.put(STEP_START_TIME, stepStartTime == null ? "" : stepStartTime.toString());

        Stack<Map<String, String>> stack = workflowStepStack.get();
        stack.push(map);
        for (Map.Entry<String, String> entry : map.entrySet()) {
            MDC.put(entry.getKey(), entry.getValue());

        }
        updateFormattedWorkflow();
    }

    public synchronized void clearWorkOrderStepMDC(String stepName, Long stepStartTime) {
        Stack<Map<String, String>> stack = workflowStepStack.get();
        if (stepName != null && stepStartTime != null && stack.size() > 0) {
            Map<String, String> peek = stack.peek();
            if (stepName.equals(peek.get(STEP_NAME)) && stepStartTime.toString()
                    .equals(peek.get(STEP_START_TIME))) {
                clearWithStack(WORK_ORDER_STEP_FIELDS, stack);
                updateFormattedWorkflow();
            }
        }
    }

    private void updateFormattedWorkflow() {
        Stack<Map<String, String>> stepStack = workflowStepStack.get();
        Stack<Map<String, String>> woStack = workflowStack.get();

        String workOrderURI = "";
        String workerId = "";
        if (!woStack.isEmpty()) {
            Map<String, String> top = woStack.peek();
            workOrderURI = top.get(WORK_ORDER_URI);
            workerId = top.get(WORKER_ID);
        }
        String stepName = "";
        if (!stepStack.isEmpty()) {
            stepName = stepStack.peek().get(STEP_NAME);
        }

        String formatted = null;

        if (workOrderURI.length() > 0) {
            if (woStack.size() == stepStack.size()) {
                formatted = String.format("%s-%s-%s", workOrderURI, workerId, stepName);
            } else if (woStack.size() > stepStack.size()) {
                //If step stack  is smaller than workflow stack, don't use the step name. Otherwise we'll use the step name from an unrelated workflow.
                formatted = String.format("%s-%s", workOrderURI, workerId);
            }
        }
        if (formatted != null) {
            MDC.put(WORKFLOW_FORMATTED, formatted);
        } else {
            MDC.remove(WORKFLOW_FORMATTED);
        }
    }
}
