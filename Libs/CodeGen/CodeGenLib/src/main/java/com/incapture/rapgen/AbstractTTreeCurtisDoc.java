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
package com.incapture.rapgen;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.RecognizerSharedState;
import org.antlr.runtime.tree.TreeNodeStream;
import org.antlr.stringtemplate.StringTemplate;

public abstract class AbstractTTreeCurtisDoc extends AbstractTTree {

    /**
     * Here we store type information (name and package really)
     * 
     */

    private Map<String, Map<String, StringTemplate>> apiTemplates = new HashMap<String, Map<String, StringTemplate>>();

    protected void addApiTemplate(String fileName, String section, StringTemplate template) {
        if (!apiTemplates.containsKey(fileName)) {
            Map<String, StringTemplate> t = new HashMap<String, StringTemplate>();
            apiTemplates.put(fileName, t);
        }
        apiTemplates.get(fileName).put(section, template);
    }

    public void dumpFiles(String outputKernelFolder, String outputApiFolder) {
        System.out.println("Dumping into " + outputApiFolder);
        dumpFor(outputApiFolder, apiTemplates);
    }

    public void dumpFor(String outputFolder, Map<String, Map<String, StringTemplate>> templates) {
        // For each file, dump the templates
        for (Map.Entry<String, Map<String, StringTemplate>> file : templates.entrySet()) {
            String outputFile = outputFolder + "/" + file.getKey();
            File f = new File(outputFile);
            f.getParentFile().mkdirs();

            BufferedWriter bow = null;
            try {
                bow = new BufferedWriter(new FileWriter(f));
                Set<String> sections = file.getValue().keySet();
                SortedSet<String> sorted = new TreeSet<String>();
                sorted.addAll(sections);
                for (String sec : sorted) {
                    bow.write(file.getValue().get(sec).toString());
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
                    }
                }
            }
        }
    }

    protected String cleanDoc(String doc) {
        doc = doc.replace('\n', ' ');
        doc = doc.replace('"', '\'');
        return doc;
    }
    /**
     * Create a new parser instance, pre-supplying the input token stream.
     * 
     * @param input
     *            The stream of tokens that will be pulled from the lexer
     */
    protected AbstractTTreeCurtisDoc(TreeNodeStream input) {
        super(input);
    }

    /**
     * Create a new parser instance, pre-supplying the input token stream and
     * the shared state.
     * 
     * This is only used when a grammar is imported into another grammar, but we
     * must supply this constructor to satisfy the super class contract.
     * 
     * @param input
     *            The stream of tokesn that will be pulled from the lexer
     * @param state
     *            The shared state object created by an interconnectd grammar
     */
    protected AbstractTTreeCurtisDoc(TreeNodeStream input, RecognizerSharedState state) {
        super(input, state);
    }

    /**
     * Creates the error/warning message that we need to show users/IDEs when
     * ANTLR has found a parsing error, has recovered from it and is now telling
     * us that a parsing exception occurred.
     * 
     * @param tokenNames
     *            token names as known by ANTLR (which we ignore)
     * @param e
     *            The exception that was thrown
     */
    @Override
    public void displayRecognitionError(String[] tokenNames, RecognitionException e) {

        // This is just a place holder that shows how to override this method
        //
        super.displayRecognitionError(tokenNames, e);
    }
}
