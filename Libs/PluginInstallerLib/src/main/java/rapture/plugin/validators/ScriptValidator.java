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
package rapture.plugin.validators;

import java.util.List;

import rapture.common.RaptureScript;
import rapture.common.RaptureURI;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.plugin.validators.Note.Level;

/**
 * This validator is made more complicated by a hack in the ScriptInstaller which allows raw reflex code to be placed in a .script file without using the .rfx
 * prefix for a raw reflex file.
 * 
 * @author mel
 */
public class ScriptValidator implements Validator {
    public static final ScriptValidator singleton = new ScriptValidator();

    public static final Validator getValidator() {
        return singleton;
    }

    public ScriptValidator() {
    }

    @Override
    public void validate(String content, RaptureURI uri, List<Note> errors) {
        if (content.startsWith("{")) {
            RaptureScript script = null;
            try {
                script = JacksonUtil.objectFromJson(content, RaptureScript.class);
            } catch (Exception ex) {
                errors.add(new Note(Level.ERROR, "Failed to parse Json for " + uri.toString()));
                return;
            }
            if (script == null) validateReflex(content);
            else validateReflex(script.getScript());
        }
    }

    public void validateReflex(String script) {

    }
}
