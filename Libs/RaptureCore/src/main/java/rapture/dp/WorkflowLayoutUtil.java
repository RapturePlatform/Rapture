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
package rapture.dp;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import rapture.common.dp.Step;
import rapture.common.dp.Transition;
import rapture.common.dp.Workflow;
import rapture.common.dp.WorkflowArrowLayout;
import rapture.common.dp.WorkflowBoxLayout;
import rapture.common.dp.WorkflowColumnLayout;
import rapture.common.dp.WorkflowGridLayout;
import rapture.dp.WorkflowLayoutUtil.BoxNote.Kind;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Utility to create a customizable presentation of a workflow definition.
 * 
 * @author mel
 */
public class WorkflowLayoutUtil {
    private static final Logger log = Logger.getLogger(WorkflowLayoutUtil.class);

    public static WorkflowGridLayout makeGridLayout(Workflow workflow) {
        if (log.isTraceEnabled()) log.trace("Making layout from scratch for " + workflow.getWorkflowURI());
        return new LayoutMaker(workflow).make();
    }

    private static class LayoutMaker {
        final WorkflowGridLayout result = new WorkflowGridLayout();
        final Map<String, BoxNote> name2note = Maps.newHashMap();
        final List<BoxNote> boxes = Lists.newArrayList();
        final List<Panel> panels = Lists.newArrayList();
        final Workflow workflow;
        final Map<Integer, WorkflowColumnLayout> num2col = Maps.newHashMap();

        int idCount = 0;

        private LayoutMaker(Workflow workflow) {
            this.workflow = workflow;
        }

        WorkflowGridLayout make() {
            result.setArrows(new ArrayList<WorkflowArrowLayout>());
            result.setColumns(new ArrayList<WorkflowColumnLayout>());
            for (Step step : workflow.getSteps()) {
                BoxNote note = new BoxNote(step);
                name2note.put(step.getName(), note);
                boxes.add(note);
            }
            BoxNote startBox = new BoxNote(Kind.START);
            Panel startPanel = new Panel(startBox, null);
            BoxNote startStep = name2note.get(workflow.getStartStep());
            if (startStep != null) mapIsland(startBox, startStep, startPanel);
            panels.add(startPanel);
            for (BoxNote note : boxes) {
                if (note.panel == null) {
                    Panel panel = new Panel(note, null);
                    mapIsland(note, null, panel);
                    panels.add(panel);
                }
            }
            // now map the panels onto the grid.
            int left = 0;
            for (Panel panel : panels) {
                int min = panel.minX();
                int max = panel.maxX();
                int offset = left - min;
                left += min - max;
                for (BoxNote note : panel.boxNotes) {
                    note.gx = note.pgx + offset;
                    note.gy = note.pgy;
                }
            }
            // now make a layout from the notes
            for (BoxNote note : boxes) {
                WorkflowBoxLayout box = note.box;
                if (box == null) box = makeWorkflowBoxLayout(note);
                getOrMakeColumn(note.gx).getBoxes().add(box);
                for (ArrowNote arrowNote : note.inArrows) {
                    WorkflowArrowLayout arrow = new WorkflowArrowLayout();
                    arrow.setFromBoxName(getOrMakeBoxId(arrowNote.source));
                    arrow.setName(n2e(arrowNote.t.getName()));
                    result.getArrows().add(arrow);
                }
            }

            // Sort the Columns and add them to the result
            List<Integer> columnKeys = Lists.newArrayList(num2col.keySet());
            Collections.sort(columnKeys);
            for (Integer key : columnKeys) {
                result.getColumns().add(num2col.get(key));
            }
            result.setWorkflowURI(workflow.getWorkflowURI());
            return result;
        }

        private String n2e(String in) {
            return (in == null) ? "" : in;
        }

        private String getOrMakeBoxId(BoxNote note) {
            if (note.box == null) makeWorkflowBoxLayout(note);
            return note.box.getId();
        }

        private WorkflowColumnLayout getOrMakeColumn(int col) {
            WorkflowColumnLayout column = num2col.get(col);
            if (column == null) {
                column = new WorkflowColumnLayout();
                column.setColumnNumber(col);
                num2col.put(col, column);
            }
            return column;
        }

        private WorkflowBoxLayout makeWorkflowBoxLayout(BoxNote note) {
            WorkflowBoxLayout result = new WorkflowBoxLayout();
            result.setGx(note.gx);
            result.setGy(note.gy);
            if (note.kind == Kind.STEP) {
                result.setName(note.step.getName());
            } else {
                result.setName("");
            }
            result.setKind(note.kind.toString());
            result.setId(nextId());
            note.box = result;
            return result;
        }

        private String nextId() {
            idCount++;
            return "_" + idCount;
        }

        private void mapIsland(BoxNote next, BoxNote previous, Panel panel) {
            if (previous == null) {
                next.pgx = 0;
                next.pgy = 0;
            } else {
                panel.placeNear(next, previous);
            }
            mapForward(next, panel);
            mapReverse(next, panel);
        }

        private void mapForward(BoxNote from, Panel panel) {
            List<BoxNote> nextGen = Lists.newArrayList();
            for (Transition t : from.step.getTransitions()) {
                if (StringUtils.isEmpty(t.getName())) handleTransition(t, from, panel, nextGen);
            }
            for (Transition t : from.step.getTransitions()) {
                if (!StringUtils.isEmpty(t.getName())) handleTransition(t, from, panel, nextGen);
            }
            for (BoxNote note : nextGen) {
                mapForward(note, panel);
            }
        }

        private void handleTransition(Transition t, BoxNote from, Panel panel, List<BoxNote> future) {
            String toName = t.getTargetStep();
            BoxNote toNote = name2note.get(toName);
            if (toNote == null) {
                toNote = makeBoxNoteFromToName(toName, panel, from, future);
                name2note.put(toName, toNote);
            }
            toNote.addArrow(from, t);
            // protects against infinite loop -- don't place the same box twice
            if (toNote.panel == null) {
                panel.placeNear(toNote, from);
            }
        }

        private static final Transition JOIN_TRANSITION;
        static {
            JOIN_TRANSITION = new Transition();
            JOIN_TRANSITION.setName("");
            JOIN_TRANSITION.setTargetStep("$JOIN");
        }

        private BoxNote makeBoxNoteFromToName(String toName, Panel parent, BoxNote fromNote, List<BoxNote> future) {
            BoxNote result = new BoxNote();
            if (toName.startsWith("$")) {
                // handle special forms in transition
                if (toName.startsWith("$RETURN")) {
                    result.kind = Kind.RETURN;
                    if (toName.startsWith("$RETURN:")) {
                        result.decoration = toName.substring("$RETURN:".length());
                    }
                } else if (toName.startsWith("$JOIN")) {
                    result.kind = Kind.JOIN;
                    parent.tails.add(result);
                }
            } else {
                BoxNote note = name2note.get(toName);
                if (note == null) {
                    result.decoration = toName;
                    result.kind = Kind.UNDEFINED;
                    return result;
                } else {
                    if (note.step != null) {
                        if (note.step.getExecutable().startsWith("$SPLIT:")) {
                            if (note.panel != null) {
                                return note;
                            }
                            result.kind = Kind.SPLIT;
                            String forkNames[] = toName.substring("$SPLIT:".length()).split(",");
                            List<Panel> forkPanels = Lists.newArrayList();
                            for (String forkName : forkNames) {
                                Panel p = makePanelForCall(forkName, parent);
                                forkPanels.add(p);
                            }
                            int width = 0;
                            int height = 1;
                            for (Panel p : forkPanels) {
                                width += p.getWidth();
                                int pHeight = p.getHeight();
                                if (pHeight > height) height = pHeight;
                            }
                            result.gw = width > 0 ? width : 1;
                            Point pos = parent.findVacantBlock(width, height + 2, fromNote);
                            parent.placeAt(pos.x, pos.y, result, true);
                            BoxNote footer = new BoxNote(Kind.JOIN);
                            footer.step = note.step; // header and footer use the same step
                            parent.placeAt(pos.x, pos.y + height + 1, footer, isTail(footer));
                            int x = pos.x;
                            for (Panel p : forkPanels) {
                                parent.transfer(x, pos.y + 1, p);
                                for (BoxNote tail : p.tails) {
                                    footer.addArrow(tail, JOIN_TRANSITION);
                                }
                            }
                            // extend from the footer, not the header
                            future.add(footer);
                            return note;
                        } else if (note.step.getExecutable().startsWith("$FORK:")) {
                            // TODO handle fork
                        } else if (note.step.getExecutable().startsWith("$FAIL")) {
                            note.kind = Kind.ERROR;
                            return note;
                        } else if (note.step.getExecutable().startsWith("$RETURN")) {
                            note.kind = Kind.RETURN;
                            if (note.step.getExecutable().startsWith("$RETURN:")) {
                                note.decoration = note.step.getExecutable().substring("$RETURN:".length());
                            }
                            return note;
                        }
                    }
                    future.add(note);
                    return note;
                }
            }
            return result;
        }

        private boolean isTail(BoxNote note) {
            List<Transition> ts = note.step.getTransitions();
            if (ts == null) return false;
            for (Transition t : ts) {
                if (t.getTargetStep().startsWith("$JOIN")) return true;
            }
            return false;
        }

        private Panel makePanelForCall(String stepName, Panel parent) {
            BoxNote root = name2note.get(stepName);
            if (root == null) {
                BoxNote call = new BoxNote(Kind.CALL);
                Panel p = new Panel(call, parent);
                p.tails.add(call);
            }
            Panel result = new Panel(root, parent);
            mapIsland(root, null, result);
            return result;
        }

        private void mapReverse(BoxNote from, Panel panel) {
            // TODO glom connected islands together by backtracing transitions - may require prep phase
        }
    }

    static class BoxNote {
        public WorkflowBoxLayout box;

        BoxNote(Step step) {
            this.step = step;
            this.kind = Kind.STEP;
        }

        BoxNote(Kind k) {
            kind = k;
        }

        BoxNote() {
            this.kind = Kind.UNDEFINED;
        }

        enum Kind {
            STEP, START, RETURN, ERROR, UNDEFINED, SPLIT, JOIN, FORK, CALL
        };

        void addArrow(BoxNote from, Transition t) {
            inArrows.add(new ArrowNote(from, t));
        }

        Kind kind;
        Panel panel = null;
        Integer pgx = null; // grid position relative to panel
        Integer pgy = null;
        Integer gx = null; // master grid position
        Integer gy = null;
        int gw = 1; // width on the grid
        Step step;
        String decoration = null;
        List<ArrowNote> inArrows = Lists.newArrayList();
    }

    static class ArrowNote {
        final Transition t;
        final BoxNote source;

        ArrowNote(BoxNote from, Transition t) {
            this.t = t;
            this.source = from;
        }
    }

    static class Panel {
        final Panel parent;
        final List<BoxNote> boxNotes = Lists.newArrayList();
        final List<BoxNote> heads = Lists.newArrayList();
        final List<BoxNote> tails = Lists.newArrayList();

        Panel(BoxNote anchor, Panel parent) {
            this.parent = parent;
            anchor.pgx = 0;
            anchor.pgy = 0;
            boxNotes.add(anchor);
            heads.add(anchor);
        }

        public void transfer(int x, int y, Panel p) {
            for (BoxNote note : boxNotes) {
                placeAt(x + note.pgx, y + note.pgy, note);
            }
        }

        public int minX() {
            int result = Integer.MAX_VALUE;
            for (BoxNote note : boxNotes) {
                if (note.pgx < result) result = note.pgx;
            }
            return result;
        }

        public int maxX() {
            int result = Integer.MIN_VALUE;
            for (BoxNote note : boxNotes) {
                if (note.pgx > result) result = note.pgx;
            }
            return result;
        }

        public int minY() {
            int result = Integer.MAX_VALUE;
            for (BoxNote note : boxNotes) {
                if (note.pgy < result) result = note.pgy;
            }
            return result;
        }

        public int maxY() {
            int result = Integer.MIN_VALUE;
            for (BoxNote note : boxNotes) {
                if (note.pgy > result) result = note.pgy;
            }
            return result;
        }

        public int getWidth() {
            return maxX() - minX() + 1;
        }

        public int getHeight() {
            return maxY() - minY() + 1;
        }

        // TODO hash this for performance x+":"+y as key -- wide elements need multiple keys
        public boolean isVacant(int x, int y) {
            for (BoxNote note : boxNotes) {
                if (note.pgx >= x && note.pgx + note.gw <= x && note.pgy == y) return false;
            }
            return true;
        }

        boolean placeAt(int x, int y, BoxNote note) {
            return placeAt(x, y, note, true);
        }

        /**
         * @returns false if already occupied
         */
        boolean placeAt(int x, int y, BoxNote note, boolean isTail) {
            if (isVacant(x, y)) {
                note.pgx = x;
                note.pgy = y;
                note.panel = this;
                if (isTail) tails.add(note);
                return true;
            }
            return false;
        }

        void placeNear(BoxNote note, BoxNote near) {
            int fx = near.pgx;
            int fy = near.pgy;
            if (!placeAt(fx, fy + 1, note)) if (!placeAt(fx + 1, fy, note)) if (!placeAt(fx - 1, fy, note)) {
                if (!placeAt(fx - 1, fy + 1, note)) if (!placeAt(fx + 1, fy + 1, note)) {
                    // TODO ESCALATE -- HARD TO PLACE
                }
            }
        }

        // TODO replace this brute force check with something vaguely performant
        Point findVacantBlock(int width, int height, BoxNote near) {
            int sx = near.gx;
            int sy = near.gy + 1;
            if (isVacantBlock(sx, sy, width, height)) return new Point(sx, sy);

            int roam = 1;
            while (true) {
                if (isVacantBlock(sx + roam, sy, width, height)) return new Point(sx, sy);
                if (isVacantBlock(sx, sy + roam, width, height)) return new Point(sx, sy);
                if (isVacantBlock(sx - roam, sy, width, height)) return new Point(sx, sy);
                roam++;
            }
        }

        boolean isVacantBlock(int x, int y, int w, int h) {
            for (int i = 0; i < w; i++) {
                for (int j = 0; j < h; j++) {
                    if (!isVacant(x + i, y + j)) return false;
                }
            }
            return true;
        }
    }
}
