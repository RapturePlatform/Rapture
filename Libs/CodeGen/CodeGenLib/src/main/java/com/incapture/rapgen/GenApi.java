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

import java.io.IOException;

import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.antlr.runtime.tree.Tree;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import rapture.common.exception.ExceptionToString;

import com.incapture.rapgen.TParser.hmxdef_return;
import com.incapture.rapgen.reader.ResourceBasedApiReader;

class GenApi {

    public static void main(String[] args) {
        Options options = new Options();
        options.addOption("l", true, "Language to generate");
        options.addOption("o", true, "Output root folder for kernel files");
        options.addOption("a", true, "Output root folder for api files");
        options.addOption("d", true, "Template dir to use (use either this or 't')");
        options.addOption("t", true, "Template file to use (use either this or 'd')");
        options.addOption("g", true, "The type of grammar to generate, current options are 'SDK' or 'API'");
        options.addOption("mainApiFile", true, "FileName specifying the api");
        options.addOption("codeSamplesJava", true, "A path to search for files that have Java code samples");
        options.addOption("codeSamplesPython", true, "A path to search for files that have Python code samples");

        CommandLineParser cparser = new PosixParser();
        try {
            CommandLine cmd = cparser.parse(options, args);
            String mainApiFile = cmd.getOptionValue("mainApiFile");
            String outputKernelFolder = cmd.getOptionValue('o');
            String outputApiFolder = cmd.getOptionValue('a');
            String codeSamplesJava = cmd.getOptionValue("codeSamplesJava");
            String codeSamplesPython = cmd.getOptionValue("codeSamplesPython");

            GenType genType = GenType.valueOf(cmd.getOptionValue('g'));

            // The language will ultimately choose the walker class
            String language = cmd.getOptionValue('l');
            if (cmd.hasOption('d') && cmd.hasOption('t')) {
                throw new IllegalArgumentException("Cannot define both a template folder ('d') and file ('t'). Please use one OR the other.");
            }

            // And off we go

            TLexer lexer = new TLexer();
            ResourceBasedApiReader apiReader = new ResourceBasedApiReader();
            lexer.setApiReader(apiReader);
            lexer.setCharStream(apiReader.read(mainApiFile));

            // Using the lexer as the token source, we create a token
            // stream to be consumed by the parser
            //
            CommonTokenStream tokens = new CommonTokenStream(lexer);

            // Now we need an instance of our parser
            //
            TParser parser = new TParser(tokens);

            hmxdef_return psrReturn = parser.hmxdef();

            // load in T.stg template group, put in templates variable
            StringTemplateGroup templates = null;

            if (!isSlateMd(language)) {
                templates = TemplateRepo.getTemplates(language, genType);
            }
            Tree t = psrReturn.getTree();

            CommonTreeNodeStream ns = new CommonTreeNodeStream(t);
            ns.setTokenStream(tokens);
            if (templates != null) {
                templates.registerRenderer(String.class, new UpCaseRenderer());
            }
            AbstractTTree walker = TreeFactory.createTreeWalker(ns, templates, language);
            System.out.println("Generating files with a " + walker.getClass().getName());
            if (walker instanceof TTree) {
                if (genType.equals(GenType.API)) {
                    ((TTree) walker).apiGen();
                } else {
                    ((TTree) walker).sdkGen();
                }
            } else if (walker instanceof TTreeRuby) {
                System.out.println("Running for Ruby");
                /* TTreeRuby.hmxdef_return out = */
                ((TTreeRuby) walker).hmxdef();
            } else if (walker instanceof TTreeJS) {
                System.out.println("Running for JavaScript");
                /* TTreeJS.hmxdef_return out = */
                ((TTreeJS) walker).hmxdef();
            } else if (walker instanceof TTreeDoc) {
                System.out.println("Running for Documentation");
                /* TTreeDoc.hmxdef_return out = */
                ((TTreeDoc) walker).hmxdef();
            } else if (walker instanceof TTreeVB) {
                System.out.println("Running for VB");
                /* TTreeVB.hmxdef_return out = */
                ((TTreeVB) walker).hmxdef();
            } else if (walker instanceof TTreeGo) {
                System.out.println("Running for Go");
                /* TTreeGo.hmxdef_return out = */
                ((TTreeGo) walker).hmxdef();
            } else if (walker instanceof TTreeCpp) {
                System.out.println("Running for Cpp");
                /* TTreeGo.hmxdef_return out = */
                ((TTreeCpp) walker).hmxdef();
            } else if (walker instanceof TTreePython) {
                System.out.println("Running for Python");
                /* TTreePython.hmxdef_return out = */
                ((TTreePython) walker).hmxdef();
            } else if (walker instanceof TTreeSlateMd) {
                System.out.println("Running for Slate Markdown");
                TTreeSlateMd slateMdWalker = (TTreeSlateMd) walker;
                slateMdWalker.setupCodeParser(codeSamplesJava, codeSamplesPython);
                slateMdWalker.hmxdef();
            } else if (walker instanceof TTreeDotNet) {
                System.out.println("Running for DotNet");
                ((TTreeDotNet) walker).apiGen();
            } else if (walker instanceof TTreeCurtisDoc) {
                System.out.println("Running for CurtisDoc");
                ((TTreeCurtisDoc) walker).apiGen();
            }
            // Now dump the files out
            System.out.println("Dumping files to " + outputKernelFolder + " and " + outputApiFolder);
            walker.dumpFiles(outputKernelFolder, outputApiFolder);

        } catch (ParseException e) {
            System.err.println("Error parsing command line - " + e.getMessage());
            System.out.println("Usage: " + options.toString());
        } catch (IOException | RecognitionException e) {
            System.err.println("Error running GenApi: " + ExceptionToString.format(e));
        }
    }

    private static boolean isSlateMd(String language) {
        return language.equals("SlateMd");
    }

}
