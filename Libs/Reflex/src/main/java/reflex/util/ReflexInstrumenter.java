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
package reflex.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * We want to record the amount of time spent in the program, and the % (and
 * amount) of that time spent in each line.
 * 
 * So each line in our program has a number of entries the line of code the
 * amount of times we have entered it the total time spent in it
 * 
 * And then we need to record the total entry points The total time
 * 
 * So we can calculate % for each line as well
 * 
 * @author amkimian
 * 
 */
public class ReflexInstrumenter {
    private static final Logger log = Logger.getLogger(ReflexInstrumenter.class);
    private List<InstrumentationLine> lines = new ArrayList<InstrumentationLine>();

    private long totalMillis = 0L;
    private long totalEntryCount = 0L;
    private long currentStart = 0L;
    private int currentLine = -1;

    public List<InstrumentationLine> getLines() {
        return lines;
    }

    public long getTotalCount() {
        return totalEntryCount;
    }

    public long getTotalTime() {
        return totalMillis;
    }

    public void setProgram(String program) {
        String[] parts = program.split("\n");
        lines = new ArrayList<InstrumentationLine>(parts.length);
        for (String p : parts) {
            // if (p.length() != 0) {
            InstrumentationLine line = new InstrumentationLine();
            line.setCode(p);
            line.setTotalCount(0L);
            line.setTotalTime(0L);
            lines.add(line);
            // }
        }
    }

    public void endLine(int lineNumber) {
        if (lineNumber != -1L && lineNumber == currentLine) {
            long elapsed = System.currentTimeMillis() - currentStart;
            InstrumentationLine l = lines.get(lineNumber - 1);
            l.addEntry(elapsed);
            totalMillis += elapsed;
            totalEntryCount++;
        }

    }

    public void startLine(int lineNumber) {
        if (lineNumber > 0 && lineNumber <= lines.size()) {
            currentLine = lineNumber;
            currentStart = System.currentTimeMillis();
        }

    }

    public void log() {
        log.debug(getInstrumentLogs());
    }
    
    public String getInstrumentLogs() {
        StringBuilder sb = new StringBuilder();
        if (totalEntryCount > 0 && totalMillis > 0) {
            for (InstrumentationLine l : lines) {
                String line = String.format("%d [%d%%] %d[%d%%] %s", l.getTotalCount(), l.getTotalCount() * 100 / totalEntryCount, l.getTotalTime(),
                        l.getTotalTime() * 100 / totalMillis, l.getCode());
                sb.append(line);
                sb.append("\n");
            }
        }
        sb.append(String.format("Total Counts %d, Total Time %d", totalEntryCount, totalMillis));
        sb.append("\n");
        return sb.toString();
    }

	public List<String> getTextLog() {
		List<String> ret = new ArrayList<String>();
        if (totalEntryCount > 0 && totalMillis > 0) {
            for (InstrumentationLine l : lines) {
                String line = String.format("%d [%d%%] %d[%d%%] %s", l.getTotalCount(), l.getTotalCount() * 100 / totalEntryCount, l.getTotalTime(),
                        l.getTotalTime() * 100 / totalMillis, l.getCode());
                ret.add(line);
            }
        }
        ret.add(String.format("Total Counts %d, Total Time %d", totalEntryCount, totalMillis));
        return ret;
	}

}
