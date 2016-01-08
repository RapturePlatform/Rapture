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
package com.incapture.slate;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.incapture.slate.model.FunctionNode;
import com.incapture.slate.model.node.ApiNode;
import com.incapture.slate.model.node.AsideNode;
import com.incapture.slate.model.node.AsideNodeType;
import com.incapture.slate.model.node.EmptyLineNode;
import com.incapture.slate.model.node.HrefNode;
import com.incapture.slate.model.node.IndexNode;
import com.incapture.slate.model.node.LanguageNode;
import com.incapture.slate.model.node.TextNode;
import com.incapture.slate.model.node.WrapperNode;
import com.incapture.slate.model.node.code.CodeAnnotationNode;
import com.incapture.slate.model.node.code.CodeLanguageNode;
import com.incapture.slate.model.node.description.HeaderNode;
import com.incapture.slate.model.node.description.table.TableHeaderNode;
import com.incapture.slate.model.node.description.table.TableNode;
import com.incapture.slate.model.node.description.table.TableRowNode;

/**
 * @author bardhi
 * @since 6/2/15.
 */
public class DocGeneratorTest {

    private File rootDir;

    @Before
    public void setUp() throws Exception {
        rootDir = File.createTempFile("rapture.test", "slate");
        rootDir.delete();
        rootDir.mkdir();
    }

    @After
    public void tearDown() throws Exception {
        //        FileUtils.deleteDirectory(rootDir);
    }

    private final static Set<String> FILE_NAMES = ImmutableSet.<String>builder().add("_authentication.md").add("_doc.md").add("_introduction.md").build();

    private static final Logger log = Logger.getLogger(DocGeneratorTest.class);

    @Test
    public void testGenerateOutput() throws Exception {

        DocGenerator generator = new DocGenerator();
        IndexNode indexNode = new IndexNode();
        indexNode.setTitle("Rapture API Docs");
        indexNode.getLanguages().add(new LanguageNode("java"));
        indexNode.getLanguages().add(new LanguageNode("python"));
        indexNode.getLanguages().add(new LanguageNode("javascript"));
        indexNode.getLanguages().add(new LanguageNode("shell", "cURL"));
        indexNode.getTocFooters().add(new TextNode("by Incapture Technologies"));
        indexNode.getTocFooters().add(new HrefNode("Documentation Powered by Slate", "http://github.com/tripit/slate"));
        indexNode.setSearchable(true);

        ApiNode introApi = createIntroApi();
        ApiNode authApi = createAuthApi();
        ApiNode docApi = createDocApi();

        generator.generateOutput(rootDir, indexNode, Arrays.asList(introApi, authApi, docApi));

        Collection<File> files = FileUtils.listFiles(rootDir, null, true);
        assertEquals(4, files.size());
        for (File file : files) {
            if (file.getName().equals("index.md")) {
                String content = readResource("/output/expected/index.md");
                assertEquals(content, FileUtils.readFileToString(file));
            } else {
                String name = file.getName();
                assertTrue("filename is " + name, FILE_NAMES.contains(name));
                assertTrue(file.getAbsolutePath().endsWith("includes/" + name));
                String content = readResource("/output/expected/includes/" + name);
                assertEquals(String.format("content for file %s does not match", file.getAbsolutePath()), content, FileUtils.readFileToString(file));
            }
        }
    }

    private ApiNode createDocApi() throws IOException {
        ApiNode api = new ApiNode("Doc");

        FunctionNode getDocDoc = createGetDocDoc();
        api.getFunctions().add(getDocDoc);

        FunctionNode putDocDoc = createPutDocDoc();
        api.getFunctions().add(putDocDoc);

        return api;
    }

    private FunctionNode createPutDocDoc() throws IOException {
        FunctionNode putDocDoc = new FunctionNode("putDoc");
        WrapperNode descriptionNode = new WrapperNode();
        descriptionNode.getNodes().add(new TextNode("This endpoint writes a document."));
        descriptionNode.getNodes().add(new EmptyLineNode());
        descriptionNode.getNodes().add(new HeaderNode("HTTP Request"));
        descriptionNode.getNodes().add(new EmptyLineNode());
        descriptionNode.getNodes().add(new TextNode("`PUT http://example.com:8665/rapture/doc/putDoc`"));
        descriptionNode.getNodes().add(new EmptyLineNode());
        descriptionNode.getNodes().add(new HeaderNode("Query Parameters"));
        descriptionNode.getNodes().add(new EmptyLineNode());
        TableNode tableNode = new TableNode(new TableHeaderNode("Parameter", "Type", "Description"));
        tableNode.getRows().add(new TableRowNode("documentURI", "DocumentURI", "The document URI to write"));
        tableNode.getRows().add(new TableRowNode("content", "JSON", "The JSON to write"));
        descriptionNode.getNodes().add(tableNode);
        putDocDoc.setDescriptionNode(descriptionNode);

        WrapperNode codeWrapperNode = new WrapperNode();
        CodeLanguageNode javaNode = new CodeLanguageNode(readResource("/code/doc/putDoc/java.txt"), "java");
        codeWrapperNode.getNodes().add(javaNode);
        CodeLanguageNode pythonNode = new CodeLanguageNode(readResource("/code/doc/putDoc/python.txt"), "python");
        codeWrapperNode.getNodes().add(pythonNode);
        CodeLanguageNode jsNode = new CodeLanguageNode(readResource("/code/doc/putDoc/js.txt"), "js");
        codeWrapperNode.getNodes().add(jsNode);
        CodeLanguageNode shellNode = new CodeLanguageNode(readResource("/code/doc/putDoc/shell.txt"), "shell");
        codeWrapperNode.getNodes().add(shellNode);
        putDocDoc.setCodeWrapperNode(codeWrapperNode);

        return putDocDoc;
    }

    private FunctionNode createGetDocDoc() throws IOException {
        FunctionNode getDocDoc = new FunctionNode("getDoc");
        WrapperNode descriptionNode = new WrapperNode();
        descriptionNode.getNodes().add(new TextNode("This endpoint gets a document."));
        descriptionNode.getNodes().add(new EmptyLineNode());
        descriptionNode.getNodes().add(new HeaderNode("HTTP Request"));
        descriptionNode.getNodes().add(new EmptyLineNode());
        descriptionNode.getNodes().add(new TextNode("`GET http://example.com:8665/rapture/doc/getDoc`"));
        descriptionNode.getNodes().add(new EmptyLineNode());
        descriptionNode.getNodes().add(new HeaderNode("Query Parameters"));
        descriptionNode.getNodes().add(new EmptyLineNode());
        TableNode tableNode = new TableNode(new TableHeaderNode("Parameter", "Type", "Description"));
        tableNode.getRows().add(new TableRowNode("documentURI", "DocumentURI", "The document URI to get"));
        descriptionNode.getNodes().add(tableNode);
        getDocDoc.setDescriptionNode(descriptionNode);

        WrapperNode codeWrapperNode = new WrapperNode();
        CodeLanguageNode javaNode = new CodeLanguageNode(readResource("/code/doc/getDoc/java.txt"), "java");
        codeWrapperNode.getNodes().add(javaNode);
        CodeLanguageNode pythonNode = new CodeLanguageNode(readResource("/code/doc/getDoc/python.txt"), "python");
        codeWrapperNode.getNodes().add(pythonNode);
        CodeLanguageNode jsNode = new CodeLanguageNode(readResource("/code/doc/getDoc/js.txt"), "js");
        codeWrapperNode.getNodes().add(jsNode);
        CodeLanguageNode shellNode = new CodeLanguageNode(readResource("/code/doc/getDoc/shell.txt"), "shell");
        codeWrapperNode.getNodes().add(shellNode);
        getDocDoc.setCodeWrapperNode(codeWrapperNode);
        return getDocDoc;
    }

    private ApiNode createAuthApi() throws IOException {
        ApiNode api = new ApiNode("Authentication");

        WrapperNode descriptionNode = new WrapperNode();
        descriptionNode.getNodes().add(new TextNode("Rapture uses username and password to log in. Look at the code samples on the right for a how-to."));
        descriptionNode.getNodes().add(new EmptyLineNode());
        descriptionNode.getNodes().add(new TextNode("`Host: localhost`"));
        descriptionNode.getNodes().add(new TextNode("`Username: rapture`"));
        descriptionNode.getNodes().add(new TextNode("`Password: password`"));
        descriptionNode.getNodes().add(new EmptyLineNode());
        descriptionNode.getNodes()
                .add(new AsideNode("You must replace <code>localhost</code> as well as username and password with your own values.", AsideNodeType.NOTICE));
        api.setDescriptionNode(descriptionNode);

        WrapperNode codeWrapperNode = new WrapperNode();
        codeWrapperNode.getNodes().add(new CodeAnnotationNode("To log in and get authenticated, use this code:"));
        codeWrapperNode.getNodes().add(new EmptyLineNode());
        CodeLanguageNode javaNode = new CodeLanguageNode(readResource("/code/auth/java.txt"), "java");
        codeWrapperNode.getNodes().add(javaNode);
        CodeLanguageNode pythonNode = new CodeLanguageNode(readResource("/code/auth/python.txt"), "python");
        codeWrapperNode.getNodes().add(pythonNode);
        CodeLanguageNode jsNode = new CodeLanguageNode(readResource("/code/auth/js.txt"), "js");
        codeWrapperNode.getNodes().add(jsNode);
        CodeLanguageNode shellNode = new CodeLanguageNode(readResource("/code/auth/shell.txt"), "shell");
        codeWrapperNode.getNodes().add(shellNode);
        codeWrapperNode.getNodes().add(new CodeAnnotationNode("You must replace <code>localhost</code> with your URI."));
        api.setCodeWrapperNode(codeWrapperNode);

        return api;
    }

    private String readResource(String resource) throws IOException {
        InputStream stream = getClass().getResourceAsStream(resource);
        String data = StringUtils.strip(IOUtils.toString(stream));
        stream.close();
        return data;
    }

    private ApiNode createIntroApi() throws IOException {
        ApiNode api = new ApiNode("Introduction");
        WrapperNode descriptionNode = new WrapperNode();
        descriptionNode.getNodes().add(new TextNode(readResource("/description/intro.txt")));
        api.setDescriptionNode(descriptionNode);
        return api;
    }
}
