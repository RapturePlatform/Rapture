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

public class BadReflexSuspendHandler implements IReflexSuspendHandler {

    private int suspendTimeInSeconds = 0;
    private String resumePoint = ""; //$NON-NLS-1$

    private int nextNodeId = 1;

    @Override
    public String getNewNodeId() {
        nextNodeId++;
        return "" + nextNodeId; //$NON-NLS-1$
    }

    @Override
    public void suspendTime(Integer suspendTimeInSeconds) {
        this.suspendTimeInSeconds = suspendTimeInSeconds.intValue();
    }

    @Override
    public void addResumeContext(String nodeId, String contextName, Object value) {
    }

    @Override
    public void addResumePoint(String nodeId) {
        throw new ReflexException(-1, Messages.getString("BadReflexSuspendHandler.CannotSuspend")); //$NON-NLS-1$
    }

    @Override
    public boolean containsResume(String nodeId, String contextName) {
        return false;
    }

    @Override
    public Object getResumeContext(String nodeId, String string) {
        return null;
    }

    @Override
    public String getResumePoint() {
        return resumePoint;
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
