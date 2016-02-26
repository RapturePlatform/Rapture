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

import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.antlr.stringtemplate.StringTemplateGroup;

public class TreeFactory {
    public static AbstractTTree createTreeWalker(CommonTreeNodeStream ns, StringTemplateGroup templates, String language) {
        ApiLanguage lang = ApiLanguage.valueOf(language.toUpperCase());
        AbstractTTree val = null;
        switch (lang) {
            case JAVA:
                val = createJavaTreeWalker(ns, templates);
                break;
            case DOTNET:
                val = new TTreeDotNet(ns);
                break;
            case CURTISDOC:
                val = new TTreeCurtisDoc(ns);
                break;
            case RUBY:
                val = new TTreeRuby(ns);
                break;
            case JAVASCRIPT:
            case JS:
                val = new TTreeJS(ns);
                break;
            case DOC:
                val = new TTreeDoc(ns);
                break;
            case SLATEMD:
                val = new TTreeSlateMd(ns);
                break;
            case VB:
                val = new TTreeVB(ns);
                break;
            case GO:
                val = new TTreeGo(ns);
                break;
            case PYTHON:
                val = new TTreePython(ns);
                break;
        }

        val.setTemplateLib(templates);
        return val;
    }

    public static TTree createJavaTreeWalker(CommonTreeNodeStream ns, StringTemplateGroup templates) {
        TTree tTree = new TTree(ns);
        tTree.setTemplateLib(templates);
        return tTree;
    }
}
