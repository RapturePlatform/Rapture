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

import static org.junit.Assert.fail;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenStream;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.antlr.runtime.tree.TreeNodeStream;
import org.junit.Test;

import com.incapture.rapgen.TParser.typeAnnotation_return;
import com.incapture.rapgen.TParser.typeExpr_return;

public class AnnotationsTest {

    @Test
    public void test1() throws RecognitionException {
        System.out.println("one");
        CharStream input = new ANTLRStringStream("@Storable");
        TLexer lexer = new TLexer(input);
        TokenStream tokenInputStream = new CommonTokenStream(lexer);
        TParser parser = new TParser(tokenInputStream);
        try {
            parser.typeAnnotation();
            fail("This should have failed");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void test2() throws RecognitionException {
        System.out.println("two");

        CharStream input = new ANTLRStringStream("@Storable ( storagePath : {\"a\"})");
        TLexer lexer = new TLexer(input);
        TokenStream tokenInputStream = new CommonTokenStream(lexer);
        TParser parser = new TParser(tokenInputStream);
        typeAnnotation_return returnVal = parser.typeAnnotation();
        System.out.println("Done " + returnVal.getTree().toStringTree());
    }

    @Test
    public void test3() throws RecognitionException {
        System.out.println("three");
        CharStream input = new ANTLRStringStream("@Addressable(scheme = MAILBOX)");
        TLexer lexer = new TLexer(input);
        TokenStream tokenInputStream = new CommonTokenStream(lexer);
        TParser parser = new TParser(tokenInputStream);
        typeAnnotation_return returnVal = parser.typeAnnotation();
        System.out.println("Done " + returnVal.getTree().toStringTree());
    }

    @Test
    public void test4() throws RecognitionException {
        System.out.println("four");
        CharStream input = new ANTLRStringStream("@Storable(storagePath : {authority, documentPath, id} )");
        TLexer lexer = new TLexer(input);
        TokenStream tokenInputStream = new CommonTokenStream(lexer);
        TParser parser = new TParser(tokenInputStream);
        typeAnnotation_return returnVal = parser.typeAnnotation();
        System.out.println("Done " + returnVal.getTree().toStringTree());
    }

    @Test
    public void test5() throws RecognitionException {
        System.out.println("five");
        CharStream input = new ANTLRStringStream("[ This is a mailbox message, usually posted by an external user]\n" + "@Addressable(scheme = MAILBOX)\n"
                + "@Storable(storagePath : {authority, documentPath, id})\n" + "type RaptureMailMessage(@package=rapture.common.model) {\n" + "   String id;\n"
                + "   String authority;\n" + "   String category;\n" + "   String content;\n" + "   Date when;\n" + "   String who;\n" + "}\n" + "");
        TLexer lexer = new TLexer(input);
        TokenStream tokenInputStream = new CommonTokenStream(lexer);
        TParser parser = new TParser(tokenInputStream);
        typeExpr_return returnVal = parser.typeExpr();
        System.out.println("Done " + returnVal.getTree().toStringTree());
    }

    @Test
    public void test6() throws RecognitionException {
        System.out.println("6");
        CharStream input = new ANTLRStringStream("[ This is a mailbox message, usually posted by an external user]\n" + "@Addressable(scheme = MAILBOX)\n"
                + "@Storable(storagePath : {authority , documentPath , id} )\n" + "type RaptureMailMessage(@package=rapture.common.model) {\n"
                + "   String id;\n" + "   String authority;\n" + "   String category;\n" + "   String content;\n" + "   Date when;\n" + "   String who;\n"
                + "}\n" + "");
        TLexer lexer = new TLexer(input);
        TokenStream tokenInputStream = new CommonTokenStream(lexer);
        TParser parser = new TParser(tokenInputStream);
        typeExpr_return returnVal = parser.typeExpr();
        System.out.println("Done " + returnVal.getTree().toStringTree());

        TreeNodeStream treeInput = new CommonTreeNodeStream(returnVal.getTree());
        TTree walker = new TTree(treeInput);
        walker.setTemplateLib(TemplateRepo.getApiTemplates("Java"));
        com.incapture.rapgen.TTree.typeExpr_return walkerResult = walker.typeExpr();
        System.out.println("Done, result=" + walkerResult.toString());
    }

    @Test
    public void test7() throws RecognitionException {
        System.out.println("seven");
        CharStream input = new ANTLRStringStream("@Storable(storagePath : {authority, documentPath, id} , separator = \".\")");
        TLexer lexer = new TLexer(input);
        TokenStream tokenInputStream = new CommonTokenStream(lexer);
        TParser parser = new TParser(tokenInputStream);
        typeAnnotation_return returnVal = parser.typeAnnotation();
        System.out.println("Done " + returnVal.getTree().toStringTree());
    }

    @Test
    public void test8() throws RecognitionException {
        System.out.println("eight");
        CharStream input = new ANTLRStringStream("@Storable(storagePath : {authority, documentPath, id})");
        TLexer lexer = new TLexer(input);
        TokenStream tokenInputStream = new CommonTokenStream(lexer);
        TParser parser = new TParser(tokenInputStream);
        typeAnnotation_return returnVal = parser.typeAnnotation();
        System.out.println("Done " + returnVal.getTree().toStringTree());
    }

    @Test
    public void test9() throws RecognitionException {
        System.out.println("nine -- extend");
        CharStream input = new ANTLRStringStream("@Extends(rapture.common RaptureJobExec)");
        TLexer lexer = new TLexer(input);
        TokenStream tokenInputStream = new CommonTokenStream(lexer);
        TParser parser = new TParser(tokenInputStream);
        typeAnnotation_return returnVal = parser.typeAnnotation();
        System.out.println("Done " + returnVal.getTree().toStringTree());
    }

    @Test
    public void test10() throws RecognitionException {
        System.out.println("ten");

        CharStream input = new ANTLRStringStream("@Storable ( storagePath : {\"a\"}, encoding=\"RaptureLightweightCoder\")");
        TLexer lexer = new TLexer(input);
        TokenStream tokenInputStream = new CommonTokenStream(lexer);
        TParser parser = new TParser(tokenInputStream);
        typeAnnotation_return returnVal = parser.typeAnnotation();
        System.out.println("Done " + returnVal.getTree().toStringTree());
    }


    @Test
    public void test11() throws RecognitionException {
        System.out.println("11");

        CharStream input = new ANTLRStringStream("@Storable ( storagePath : {\"a\"}, encoding=\"RaptureLightweightCoder\", ttlDays=90)");
        TLexer lexer = new TLexer(input);
        TokenStream tokenInputStream = new CommonTokenStream(lexer);
        TParser parser = new TParser(tokenInputStream);
        typeAnnotation_return returnVal = parser.typeAnnotation();
        System.out.println("Done " + returnVal.getTree().toStringTree());
    }


    @Test
    public void test12() throws RecognitionException {
        System.out.println("12");

        CharStream input = new ANTLRStringStream("@Storable ( storagePath : {\"a\"}, ttlDays=90)");
        TLexer lexer = new TLexer(input);
        TokenStream tokenInputStream = new CommonTokenStream(lexer);
        TParser parser = new TParser(tokenInputStream);
        typeAnnotation_return returnVal = parser.typeAnnotation();
        System.out.println("Done " + returnVal.getTree().toStringTree());
    }
}
