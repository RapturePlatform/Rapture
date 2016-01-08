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

import com.incapture.rapgen.docString.DocumentationParser;
import org.antlr.runtime.RecognizerSharedState;
import org.antlr.runtime.tree.TreeNodeStream;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

public abstract class AbstractPythonTTree extends AbstractTTree {

    protected AbstractPythonTTree(TreeNodeStream input) {
        super(input);
    }

    protected AbstractPythonTTree(TreeNodeStream input, RecognizerSharedState state) {
        super(input, state);
    }

    private static String[] reservedWords = {"and", "del", "from", "not", "while", "as", "elif", "global", "or", "with", "assert", "else", "if", "pass",
            "yield", "break", "except", "import", "print", "class", "exec", "in", "raise", "continue", "finally", "is", "return", "def", "for", "lambda", "try"};

    protected String ensureNotReserved(String name) {
        for (String res : reservedWords) {
            if (name.equals(res)) {
                return "p_" + res;
            }
        }
        return name;
    }

    @Override
    public String formatDoc(String raw) {
        StringBuilder docStringBuilder = new StringBuilder(DocumentationParser.retrieveDescription(raw));

        LinkedHashMap<String, String> params = DocumentationParser.retrieveParams(raw);
        if(params.size() > 0) {
            docStringBuilder.append("\nArguments: \n");
            for (Entry<String, String> entry : params.entrySet()) {
                docStringBuilder.append(entry.getKey()).append(" -- ").append(entry.getValue()).append("\n");
            }
        }

        if (docStringBuilder.charAt(docStringBuilder.length() - 1) == '\n') {
            docStringBuilder = new StringBuilder(docStringBuilder.substring(0, docStringBuilder.length()-1));
        }

        return docStringBuilder.toString();
    }

}
