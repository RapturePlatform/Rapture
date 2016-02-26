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
package rapture.sheet.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.Writer;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import rapture.common.RaptureSheetNote;
import rapture.common.RaptureSheetRange;
import rapture.common.RaptureSheetScript;
import rapture.common.RaptureSheetStyle;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.kernel.file.FileRepoUtils;
import rapture.util.IDGenerator;

/**
 * Stores meta information for a sheet (scripts, ranges, styles) in file based storage
 * 
 * @author qqqq
 */
public class FileMetaSheetStore {
    private File parentDir;
    private String instanceName;        // ?? is what?

    private static Logger log = Logger.getLogger(FileMetaSheetStore.class);

    public FileMetaSheetStore() {
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public void setConfig(Map<String, String> config) {
        if (parentDir != null) throw RaptureExceptionFactory.create("Calling setConfig twice is currently not supported");
        String prefix = config.get(FileRepoUtils.PREFIX);
        if (StringUtils.trimToNull(prefix) == null) throw RaptureExceptionFactory.create("prefix must be specified");
        parentDir = FileRepoUtils.ensureDirectory(prefix + "_sheetmeta");
    }

    public void drop() {
        try {
            FileUtils.deleteDirectory(parentDir);
        } catch (IOException e) {
            log.debug("Can't delete "+parentDir, e);
        }
    }

    public List<RaptureSheetStyle> getSheetStyles(final String name) {
        List<RaptureSheetStyle> styles = new ArrayList<>();        
        File file = FileRepoUtils.makeGenericFile(parentDir, name+"_style");
        try {
            List<String> lines = FileUtils.readLines(file);
            for (String line : lines)
                styles.add(JacksonUtil.objectFromJson(line, RaptureSheetStyle.class));
        } catch (IOException e) {
            log.debug("Can't read "+file.getName(), e);
        }
        return styles;
    }

    public Boolean deleteSheetStyle(String name, String styleName) {
        boolean found = false;
        List<RaptureSheetStyle> styles = getSheetStyles(name);
        File file = FileRepoUtils.makeGenericFile(parentDir, name+"_style");
        try (PrintStream out = new PrintStream(new FileOutputStream(file, false))) {
            for (RaptureSheetStyle style : styles) {
                if (!style.getName().equals(styleName)) out.println(JacksonUtil.jsonFromObject(style));
                else found = true;
            }
        } catch (IOException e) {
            log.debug("Can't read "+file.getName(), e);
        }
        return found;
    }

    public RaptureSheetStyle putSheetStyle(String name, RaptureSheetStyle style) {
        style.setName(style.getName());
        File file = FileRepoUtils.makeGenericFile(parentDir, name+"_style");
        String data = JacksonUtil.jsonFromObject(style);
        try (PrintStream out = new PrintStream(new FileOutputStream(file, true))) {
            out.append(data);
            out.append("\n");
        } catch (IOException e) {
            log.debug("Can't read "+file.getName(), e);
        }
        return style;
    }

    public List<RaptureSheetScript> getSheetScripts(final String name) {
        List<RaptureSheetScript> scripts = new ArrayList<>();
        File file = FileRepoUtils.makeGenericFile(parentDir, name+"_script");
        try {
            List<String> lines = FileUtils.readLines(file);
            for (String line : lines)
                scripts.add(JacksonUtil.objectFromJson(line, RaptureSheetScript.class));
        } catch (IOException e) {
            log.debug("Can't read "+file.getName(), e);
        }
        return scripts;
    }

    public Boolean deleteSheetScript(String name, String scriptName) {
        boolean found = false;
        List<RaptureSheetScript> scripts = getSheetScripts(name);
        File file = FileRepoUtils.makeGenericFile(parentDir, name+"_script");
        try (PrintStream out = new PrintStream(new FileOutputStream(file, false))) {
            for (RaptureSheetScript script : scripts) {
                if (!script.getName().equals(scriptName)) out.println(JacksonUtil.jsonFromObject(script));
                else found = true;
            }
        } catch (IOException e) {
            log.debug("Can't read "+file.getName(), e);
        }
        return found;
    }

    public RaptureSheetScript putSheetScript(String name, String scriptName, RaptureSheetScript script) {
        script.setName(scriptName);
        File file = FileRepoUtils.makeGenericFile(parentDir, name+"_script");
        String data = JacksonUtil.jsonFromObject(script);
        try (PrintStream out = new PrintStream(new FileOutputStream(file, true))) {
            out.append(data);
            out.append("\n");
        } catch (IOException e) {
            log.debug("Can't read "+file.getName(), e);
        }
        return script;
    }

    public List<RaptureSheetRange> getSheetNamedSelections(final String name) {
        List<RaptureSheetRange> ranges = new ArrayList<>();
        File file = FileRepoUtils.makeGenericFile(parentDir, name+"_range");
        try {
            List<String> lines = FileUtils.readLines(file);
            for (String line : lines)
                ranges.add(JacksonUtil.objectFromJson(line, RaptureSheetRange.class));
        } catch (IOException e) {
            log.debug("Can't read "+file.getName(), e);

        }
        return ranges;
    }

    public Boolean deleteSheetNamedSelection(String name, String rangeName) {
        boolean found = false;
        List<RaptureSheetRange> ranges = getSheetNamedSelections(name);
        File file = FileRepoUtils.makeGenericFile(parentDir, name+"_range");
        try (PrintStream out = new PrintStream(new FileOutputStream(file, false))) {
            for (RaptureSheetRange range : ranges) {
                if (!range.getName().equals(rangeName)) out.println(JacksonUtil.jsonFromObject(range));
                else found = true;
            }
        } catch (IOException e) {
            log.debug("Can't read "+file.getName(), e);
        }
        return found;
    }

    public RaptureSheetRange putSheetNamedSelection(String name, String rangeName, RaptureSheetRange range) {
        range.setName(rangeName);
        File file = FileRepoUtils.makeGenericFile(parentDir, name+"_range");
        String data = JacksonUtil.jsonFromObject(range);
        try (PrintStream out = new PrintStream(new FileOutputStream(file, true))) {
            out.append(data);
            out.append("\n");
        } catch (IOException e) {
            log.debug("Can't read "+file.getName(), e);
        }
        return range;
    }

    public RaptureSheetScript getSheetScript(String name, String scriptName) {
        List<RaptureSheetScript> scripts = getSheetScripts(name);
        for (RaptureSheetScript script : scripts) {
            if (script.getName().equals(scriptName)) return script;
        }
        return null;
    }

    public RaptureSheetRange getSheetNamedSelection(String name, String rangeName) {
        List<RaptureSheetRange> ranges = getSheetNamedSelections(name);
        for (RaptureSheetRange range : ranges) {
            if (range.getName().equals(rangeName)) return range;
        }
        return null;
    }

    public List<RaptureSheetNote> getSheetNotes(final String name) {
        List<RaptureSheetNote> notes = new ArrayList<>();
        File file = FileRepoUtils.makeGenericFile(parentDir, name+"_note");
        try {
            List<String> lines = FileUtils.readLines(file);
            for (String line : lines) {
                notes.add(JacksonUtil.objectFromJson(line, RaptureSheetNote.class));
            }
        } catch (IOException e) {
            log.debug("Can't read "+file.getName(), e);
        }
        return notes;
    }

    public Boolean deleteSheetNote(String name, String noteId) {
        boolean found = false;
        List<RaptureSheetNote> notes = getSheetNotes(name);
        File file = FileRepoUtils.makeGenericFile(parentDir, name+"_note");
        try (PrintStream out = new PrintStream(new FileOutputStream(file, false))) {
            for (RaptureSheetNote note : notes) {
                if (!note.getId().equals(noteId)) {
                    out.println(JacksonUtil.jsonFromObject(note));
                }
                else found = true;
            }
        } catch (IOException e) {
            log.debug("Can't read "+file.getName(), e);
        }
        return found;
    }

    public RaptureSheetNote putSheetNote(String name, RaptureSheetNote note) {
        if (note.getId() == null || note.getId().isEmpty()) {
            note.setId(IDGenerator.getUUID());
        }
        File file = FileRepoUtils.makeGenericFile(parentDir, name+"_note");
        new File(file.getParent()).mkdirs();
        String data = JacksonUtil.jsonFromObject(note) + "\n";
        try {
            Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true)));
            writer.write(data);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            log.debug("Can't read "+file.getName(), e);
        }
        return note;
    }

}
