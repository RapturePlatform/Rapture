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
package rapture.repo.stage;

import rapture.common.repo.PerspectiveObject;

/**
 * A stage is used to temporarily store state before committing.
 * 
 * It is intended to be ephemeral before committing, not something you keep
 * around for too long
 * 
 * @author amkimian
 * 
 */
public class Stage {
    // By default, entries on the stage come from the base
    private StageTree stageBase;

    // Where this will be merged back in to
    private PerspectiveObject perspective;

    //
    private String perspectiveName;

    public PerspectiveObject getPerspective() {
        return perspective;
    }

    public String getPerspectiveName() {
        return perspectiveName;
    }

    public StageTree getStageBase() {
        return stageBase;
    }

    public void setPerspective(PerspectiveObject perspective) {
        this.perspective = perspective;
    }

    public void setPerspectiveName(String perspectiveName) {
        this.perspectiveName = perspectiveName;
    }

    public void setStageBase(StageTree stageBase) {
        this.stageBase = stageBase;
    }
}
