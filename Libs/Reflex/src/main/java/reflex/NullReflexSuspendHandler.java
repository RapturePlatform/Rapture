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
package reflex;

import reflex.suspend.SuspendContext;

public class NullReflexSuspendHandler implements IReflexSuspendHandler {

    private int suspendTimeInSeconds = 0;
    private SuspendContext context = new SuspendContext();

    private int nextNodeId = 1;

    @Override
    public String getNewNodeId() {
        nextNodeId++;
        return "" + nextNodeId;
    }

    @Override
    public void suspendTime(Integer suspendTimeInSeconds) {
        this.suspendTimeInSeconds = suspendTimeInSeconds.intValue();
    }

    @Override
    public void addResumeContext(String nodeId, String contextName, Object value) {
        context.addResumeContext(nodeId, contextName, value);
    }

    @Override
    public void addResumePoint(String nodeId) {
        context.addResumePoint(nodeId);
    }

    @Override
    public boolean containsResume(String nodeId, String contextName) {
        return context.containsResume(nodeId, contextName);
    }

    @Override
    public Object getResumeContext(String nodeId, String string) {
        return context.retrieveResumeContext(nodeId, string);
    }

    @Override
    public String getResumePoint() {
        return context.getResumePoint();
    }

    @Override
    public int getSuspendTime() {
        return suspendTimeInSeconds;
    }

    @Override
    public boolean hasCapability() {
        return false;
    }

}
