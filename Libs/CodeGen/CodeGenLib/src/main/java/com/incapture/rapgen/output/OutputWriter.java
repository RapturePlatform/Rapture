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
package com.incapture.rapgen.output;

import rapture.common.exception.ExceptionToString;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.antlr.stringtemplate.StringTemplate;
import org.apache.commons.io.FileUtils;

import com.incapture.rapgen.AbstractTTree;

/**
 * @author bardhi
 * @since 11/17/14.
 */
public class OutputWriter {
    public static void writeList(String outputKernelFolder, List<String> list, String filePath) {
        String filepath = outputKernelFolder + "/" + AbstractTTree.GEN_PATH_PREFIX + filePath;
        File file = new File(filepath);
        try {
            FileUtils.writeLines(file, list);
        } catch (IOException e) {
            System.err.println(ExceptionToString.format(e));
        }
    }

    /**
     * Some files are composed of multiple templates. So the map passed in here is filename to template order to template.
     * E.g. "file.txt"->1->"some code" "file.txt"->2->"other code" and so on.
     *
     * @param rootFolder
     * @param pathToTemplate
     */
    public static void writeMultiPartTemplates(String rootFolder, Map<String, Map<String, StringTemplate>> pathToTemplate) {
        // For each file, dump the templates
        for (Map.Entry<String, Map<String, StringTemplate>> entry : pathToTemplate.entrySet()) {
            File file = new File(rootFolder, entry.getKey());
            file.getParentFile().mkdirs();

            BufferedWriter bow = null;
            try {
                bow = new BufferedWriter(new FileWriter(file));
                Set<String> sections = entry.getValue().keySet();
                SortedSet<String> sorted = new TreeSet<String>();
                sorted.addAll(sections);
                for (String sec : sorted) {
                    bow.write(entry.getValue().get(sec).toString());
                    bow.newLine();
                }
                bow.close();
            } catch (IOException e) {
                System.err.println(e.getMessage());
            } finally {
                if (bow != null) {
                    try {
                        bow.close();
                    } catch (IOException e) {
                        System.err.println("Error closing output stream: " + ExceptionToString.format(e));
                    }
                }
            }
        }
    }

    /**
     * Write templates to files. If you have files composed of multiple templates, look at {@link #writeMultiPartTemplates(String, Map)}
     *
     * @param rootFolder
     * @param pathToTemplate
     */
    public static void writeTemplates(String rootFolder, Map<String, StringTemplate> pathToTemplate) {
        for (Map.Entry<String, StringTemplate> entry : pathToTemplate.entrySet()) {
            String filename = entry.getKey();

            File file = new File(rootFolder, filename);
            file.getParentFile().mkdirs();

            StringTemplate template = entry.getValue();
            try {
                FileUtils.write(file, template.toString());
            } catch (IOException e) {
                System.err.println("Error writing template: " + ExceptionToString.format(e));
            }

        }
    }
}
