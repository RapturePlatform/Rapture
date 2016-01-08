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

import java.io.IOException;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenStream;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.antlr.runtime.tree.TreeNodeStream;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.junit.Test;

import com.incapture.rapgen.TParser.hmxdef_return;

public class JSWalkerTest {

    @Test
    public void test1() throws RecognitionException, IOException {
        CharStream input = new ANTLRStringStream("version(1.1.0)\n" + "minVer(1.1.0)\n"
                + "[ The Admin API is used to manipulate and access the low level entities in Rapture. Typically the methods in this API\n"
                + "are only used during significant setup events in a Rapture environment.]\n" + "api(Admin) {\n"
                + "    [This method restores a user that has been deleted]\n" + "    @entitle=/admin/main\n"
                + "    @public Boolean restoreUser(String userName);\n" + "}\n" + "[A return value from a native query]\n"
                + "type RaptureQueryResult(@package=rapture.common) {\n" + "    List(JsonContent) rows;\n" + "}\n");
        TLexer lexer = new TLexer(input);
        TokenStream tokenInputStream = new CommonTokenStream(lexer);
        TParser parser = new TParser(tokenInputStream);
        hmxdef_return returnVal = parser.hmxdef();
        System.out.println("Done " + returnVal.getTree().toStringTree());

        TreeNodeStream treeInput = new CommonTreeNodeStream(returnVal.getTree());
        TTreeJS walker = new TTreeJS(treeInput);
        walker.setTemplateLib(getJsTemplate());
    }

    private StringTemplateGroup getJsTemplate() throws IOException {
        String templateFile = "template/JS.stg";
        return TemplateRepo.getTemplatesFromFile(templateFile);
    }

}
