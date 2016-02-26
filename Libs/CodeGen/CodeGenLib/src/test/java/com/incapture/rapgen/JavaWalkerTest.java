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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenStream;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.antlr.runtime.tree.TreeNodeStream;
import org.junit.Before;
import org.junit.Test;

import com.incapture.rapgen.TParser.hmxdef_return;
import com.incapture.rapgen.TTree.apiGen_return;

public class JavaWalkerTest {

    private String outputApiFolder;
    private String outputKernelFolder;
    private File parentFolder;

    @Before
    public void setup() throws IOException {
//        parentFolder = File.createTempFile(prefix, "");
        parentFolder = new File("/tmp/bardhi");
        parentFolder.delete();
        parentFolder.mkdir();
        File api = new File(parentFolder, "api");
        api.mkdir();
        outputApiFolder = api.getAbsolutePath();
        
        File kernel = new File(parentFolder, "kernel");
        kernel.mkdir();
        outputKernelFolder = kernel.getAbsolutePath();
    }
    
    @Test
    public void test1() throws RecognitionException {
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
        TTree walker = new TTree(treeInput);
        walker.setTemplateLib(TemplateRepo.getApiTemplates("Java"));
        com.incapture.rapgen.TTree.hmxdef_return walkerResult = walker.hmxdef();
        System.out.println("Done, result=" + walkerResult.toString());
    }

    @Test
    public void test2() throws RecognitionException {
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
        TTree walker = new TTree(treeInput);
        walker.setTemplateLib(TemplateRepo.getApiTemplates("Java"));
        apiGen_return walkerResult = walker.apiGen();
        System.out.println("Done, result=" + walkerResult.toString());
    }

    @Test
    public void test3() throws RecognitionException {
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
        TTree walker = new TTree(treeInput);
        walker.setTemplateLib(TemplateRepo.getSdkTemplates("Java"));
        try {
            walker.sdkGen();
            fail("This should have failed, sdk name is required but not present in code");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("SDK Name"));
        }
    }

    @Test
    public void test4() throws RecognitionException {
        CharStream input = new ANTLRStringStream("sdk(alan)\n" + "version(0.0.1)\n" + "minVer(0.0.1)\n"
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
        TTree walker = new TTree(treeInput);
        walker.setTemplateLib(TemplateRepo.getSdkTemplates("Java"));
        walker.sdkGen();
    }

    @Test
    public void test5() throws RecognitionException {
        CharStream input = new ANTLRStringStream("sdk(alan)\n" + "version(0.0.1)\n" + "minVer(0.0.1)\n"
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
        TTree walker = new TTree(treeInput);
        walker.setTemplateLib(TemplateRepo.getApiTemplates("Java"));
        try {
            walker.apiGen();
            fail("This should have failed, sdk name is not expected but found");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("SDK Name"));
        }
    }
    
    
    @Test
    public void test6() throws RecognitionException {
        CharStream input = new ANTLRStringStream("sdk(alan)\n" + 
                "version(0.0.1)\n" + 
                "minVer(0.0.1)\n" +
                "\n" + 
                "[Information about a folder or a file in a repo.]\n" + 
                "type RaptureFolderInfo(@package=rapture.common) {\n" + 
                "    String name;\n" + 
                "    Boolean isFolder;\n" + 
                "}\n" + 
                "\n" + 
                "[ This is a test api of the SDK.]\n" + 
                "api(TestSDK) {\n" + 
                "    [Log a message to standard out]\n" + 
                "    @entitle=/admin/main\n" + 
                "    @public Boolean logMessage(String msg);\n" + 
                "}\n" + 
                "\n" + 
                "//include \"testApi/crudSamp.api\"\n" + 
                "\n" + 
                "[Alan Info type]\n" + 
                "\n" + 
                "crud AlanInfo( region name ) { \n" + 
                "   String region;\n" + 
                "   String name;\n" + 
                "   String value;\n" + 
                "}\n" + 
                "\n" + 
                "");
        TLexer lexer = new TLexer(input);
        TokenStream tokenInputStream = new CommonTokenStream(lexer);
        TParser parser = new TParser(tokenInputStream);
        hmxdef_return returnVal = parser.hmxdef();
        System.out.println("Done " + returnVal.getTree().toStringTree());

        TreeNodeStream treeInput = new CommonTreeNodeStream(returnVal.getTree());
        TTree walker = new TTree(treeInput);
        walker.setTemplateLib(TemplateRepo.getSdkTemplates("Java"));
        walker.sdkGen();
    }


    @Test
    public void test7() throws RecognitionException {
        CharStream input = new ANTLRStringStream("version(1.1.0)\n" + "minVer(1.1.0)\n"
                + "[ The Admin API is used to manipulate and access the low level entities in Rapture. Typically the methods in this API\n"
                + "are only used during significant setup events in a Rapture environment.]\n" + "api(Admin) {\n"
                + "    [This method restores a user that has been deleted]\n" + "    @entitle=/admin/main\n"
                + "    @public Boolean restoreUser(String userName);\n" + "}\n" + "[A return value from a native query]\n"
                + "@Storable(storagePath : {\"sys\", name}, separator=\".\")\n" + 
                "type RepoConfig (@package=rapture.common.model) {\n" + 
                "   String name = \"test\";\n" + 
                "   String config;\n" + 
                "}\n" + 
                "");
        TLexer lexer = new TLexer(input);
        TokenStream tokenInputStream = new CommonTokenStream(lexer);
        TParser parser = new TParser(tokenInputStream);
        hmxdef_return returnVal = parser.hmxdef();
        System.out.println("Done " + returnVal.getTree().toStringTree());

        TreeNodeStream treeInput = new CommonTreeNodeStream(returnVal.getTree());
        TTree walker = new TTree(treeInput);
        walker.setTemplateLib(TemplateRepo.getApiTemplates("Java"));
        apiGen_return walkerResult = walker.apiGen();
        System.out.println("Done, result=" + walkerResult.toString());
    }
    
    @Test
    public void testBean() throws RecognitionException {
        CharStream input = new ANTLRStringStream("version(1.1.0)\n" + "minVer(1.1.0)\n"
                + "[ The Admin API is used to manipulate and access the low level entities in Rapture. Typically the methods in this API\n"
                + "are only used during significant setup events in a Rapture environment.]\n" + "api(Admin) {\n"
                + "    [This method restores a user that has been deleted]\n" + "    @entitle=/admin/main\n"
                + "    @public Boolean restoreUser(String userName);\n" + "}\n" + 
                "[A Graph node]\n" + 
                "@Bean\n" + 
                "type Node(@package=rapture.common.dp) {\n" + 
                "    String nodeId; //this is not a URI, just a String id\n" + 
                "    List<XFer> xferValues;\n" + 
                "}\n" + 
                ""
                );
        TLexer lexer = new TLexer(input);
        TokenStream tokenInputStream = new CommonTokenStream(lexer);
        TParser parser = new TParser(tokenInputStream);
        hmxdef_return returnVal = parser.hmxdef();
        System.out.println("Done " + returnVal.getTree().toStringTree());

        TreeNodeStream treeInput = new CommonTreeNodeStream(returnVal.getTree());
        TTree walker = new TTree(treeInput);
        walker.setTemplateLib(TemplateRepo.getApiTemplates("Java"));
        apiGen_return walkerResult = walker.apiGen();
        System.out.println("Done, result=" + walkerResult.toString());
    }

    @Test
    public void testTypeGenericURI() throws RecognitionException {
        CharStream input = new ANTLRStringStream("version(1.1.0)\n" + "minVer(1.1.0)\n"
                + "[ The Admin API is used to manipulate and access the low level entities in Rapture. Typically the methods in this API\n"
                + "are only used during significant setup events in a Rapture environment.]\n" + "api(Admin) {\n"
                + "    [This method restores a user that has been deleted]\n" + "    @entitle=/admin/main\n"
                + "    @public Boolean restoreUser(String userName);\n" + "}\n" + 
                "[A Graph node]\n" + 
                "@Bean\n" + 
                "type Node(@package=rapture.common.dp) {\n" + 
                "    String nodeId; //this is not a URI, just a String id\n" + 
                "    List<String> stringValues;\n" + 
                "    List<JobURI> jobUriValues;\n" + 
                "    JobURI myJobURI;\n" + 
                "}\n" + 
                ""
                );
        TLexer lexer = new TLexer(input);
        TokenStream tokenInputStream = new CommonTokenStream(lexer);
        TParser parser = new TParser(tokenInputStream);
        hmxdef_return returnVal = parser.hmxdef();
        System.out.println("Done " + returnVal.getTree().toStringTree());

        TreeNodeStream treeInput = new CommonTreeNodeStream(returnVal.getTree());
        TTree walker = new TTree(treeInput);
        walker.setTemplateLib(TemplateRepo.getApiTemplates("Java"));
        walker.apiGen();
        walker.dumpFiles(outputKernelFolder, outputApiFolder);
        System.out.println("Done, folder=" + parentFolder.getAbsolutePath());
    }

}
