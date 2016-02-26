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
package com.incapture.rapgen;

import java.util.List;

import org.antlr.stringtemplate.StringTemplateGroup;
import org.antlr.stringtemplate.language.AngleBracketTemplateLexer;
import org.apache.log4j.Logger;

import rapture.util.ResourceLoader;

public class ResourceBasedStringTemplateGroup extends StringTemplateGroup {

    private static final Logger log = Logger.getLogger(ResourceBasedStringTemplateGroup.class);

    public ResourceBasedStringTemplateGroup(String name, String resourcePrefix) {
        this(name, resourcePrefix, null);
    }

    public ResourceBasedStringTemplateGroup(String name, String resourcePrefix, String fallbackResourcePrefix) {
        this(name, resourcePrefix, fallbackResourcePrefix, null);
    }

    // not worth generalizing to n levels.  Three should be enough for a long time.
    public ResourceBasedStringTemplateGroup(String name, String primary, String secondary, String tertiary) {
        super(name, AngleBracketTemplateLexer.class);
        log.info(String.format("Loading templates for %s from %s", name, primary));

        List<String> scripts = ResourceLoader.getScripts(this, primary);
        log.debug(String.format("Template names are %s", scripts.toString()));
        loadScripts(scripts, primary);

        if (secondary != null) {
            List<String> fallbackScripts = ResourceLoader.getScripts(this, secondary);
            fallbackScripts.removeAll(scripts);
            scripts.addAll(fallbackScripts);
            log.debug(String.format("Secondary template names are %s", fallbackScripts.toString()));
            loadScripts(fallbackScripts, secondary);
        }

        if (tertiary != null) {
            List<String> fallbackScripts = ResourceLoader.getScripts(this, tertiary);
            fallbackScripts.removeAll(scripts);
            log.debug(String.format("Tertiary template names are %s", fallbackScripts.toString()));
            loadScripts(fallbackScripts, tertiary);
        }

        log.info(String.format("Loaded [%s] templates", this.getTemplateNames().size()));
        log.debug("Loaded templates: " + this.getTemplateNames().toString());
    }

    private void loadScripts(List<String> scripts, String resourcePrefix) {
        for (String script : scripts) {
            if (script.length() > 4) {
                log.debug(String.format("Loading template %s from %s", script, resourcePrefix));
                String template = ResourceLoader.getResourceAsString(this, resourcePrefix + script);
                String templateName = script.substring(script.startsWith("/") ? 1 : 0, script.length() - 3);
                this.defineTemplate(templateName, template);
            }
        }
    }

}
