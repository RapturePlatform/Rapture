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
package rapture.plugin.validators;

import java.util.List;
import java.util.Map;

import rapture.common.RaptureURI;
import rapture.common.dp.Step;
import rapture.common.dp.Transition;
import rapture.common.dp.Workflow;

import com.google.common.collect.Maps;

public class WorkflowValidator extends JsonValidator<Workflow> {
    public static final Validator singleton = new WorkflowValidator(Workflow.class);

    public static Validator getValidator() {
        return singleton;
    }

    public WorkflowValidator(Class<Workflow> clazz) {
        super(clazz);
    }

    @Override
    void validateObject(Workflow thing, RaptureURI uri, List<Note> errors) {
        if (!thing.getWorkflowURI().equals(uri.toString())) {
            errors.add(new Note(Note.Level.ERROR, "Mismatched URI " + thing.getWorkflowURI() + " in " + uri.toString()));
        }
        Map<String, Step> name2step = Maps.newHashMap();
        for (Step step : thing.getSteps()) {
            Step old = name2step.put(step.getName(), step);
            if (old != null) {
                errors.add(new Note(Note.Level.ERROR, "Duplicate step name " + step.getName() + " in workflow " + uri.toString()));
            }
        }
        for (Step step : thing.getSteps()) {
            for (Transition t : step.getTransitions()) {
                String target = t.getTargetStep();
                if (target.startsWith("$")) {
                    // TODO check for SPLIT/FORK
                } else {
                    Step targetStep = name2step.get(target);
                    if (targetStep == null) {
                        errors.add(new Note(Note.Level.ERROR, "Transition from step " + step.getName() + " to non-extant step " + target));
                    }
                }
            }
        }
    }

    @Override
    void validateRaw(String content, RaptureURI uri, List<Note> errors) {
    }
}
