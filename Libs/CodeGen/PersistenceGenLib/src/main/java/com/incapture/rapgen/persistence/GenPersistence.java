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
package com.incapture.rapgen.persistence;

import rapture.common.exception.ExceptionToString;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.antlr.runtime.tree.Tree;
import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;

import com.incapture.rapgen.GenType;
import com.incapture.rapgen.TLexer;
import com.incapture.rapgen.TParser;
import com.incapture.rapgen.TParser.hmxdef_return;
import com.incapture.rapgen.TTree;
import com.incapture.rapgen.TemplateRepo;
import com.incapture.rapgen.TreeFactory;
import com.incapture.rapgen.UpCaseRenderer;
import com.incapture.rapgen.output.OutputWriter;
import com.incapture.rapgen.reader.ResourceBasedApiReader;
import com.incapture.rapgen.storable.StorableAttributes;

class GenPersistence {

    private static final Logger log = Logger.getLogger(GenPersistence.class);

    public static void main(String[] args) {

        Options options = new Options();
        options.addOption("o", true, "Output root folder for kernel files");

        options.addOption("g", true, "The type of grammar to generate, current options are 'SDK' or 'API'");

        options.addOption("mainApiFile", true, "FileName specifying the api");

        CommandLineParser cparser = new PosixParser();
        try {
            CommandLine cmd = cparser.parse(options, args);
            String mainApiFile = cmd.getOptionValue("mainApiFile");
            String outputFolder = cmd.getOptionValue('o');
            GenType genType = GenType.valueOf(cmd.getOptionValue('g'));
            StringTemplateGroup templateLib = loadTemplates(genType);

            List<StorableAttributes> storableAttributes = parseApiFiles(cmd, mainApiFile, templateLib, genType);
            StorableSerDeserRepo mappersRepo = StorableMappersLoader.loadSerDeserHelpers();
            log.info(String.format("Got %s storable mapper(s)", mappersRepo.getAll().size()));

            Generator generator = new Generator(templateLib);
            Map<String, StringTemplate> pathToTemplate = generator
                    .generatePersistenceFiles(storableAttributes, mappersRepo);

            log.info(String.format("Writing persistence files in [%s]", outputFolder));
            OutputWriter.writeTemplates(outputFolder, pathToTemplate);

        } catch (ParseException e) {
            System.err.println("Error parsing command line - " + e.getMessage());
            System.out.println("Usage: " + options.toString());
        } catch (IOException | RecognitionException e) {
            System.err.println("Error running GenApi: " + ExceptionToString.format(e));
        }
    }

    private static StringTemplateGroup loadTemplates(GenType genType) throws FileNotFoundException {
        StringTemplateGroup templates = TemplateRepo.getTemplates("Java", genType);

        if (templates != null) {
            templates.registerRenderer(String.class, new UpCaseRenderer());
        }
        return templates;
    }

    private static List<StorableAttributes> parseApiFiles(CommandLine cmd, String mainApiFile, StringTemplateGroup templateLib, GenType genType)
            throws IOException, RecognitionException {

        TLexer lexer = new TLexer();
        ResourceBasedApiReader apiReader = new ResourceBasedApiReader();
        lexer.setApiReader(apiReader);
        System.out.println(String.format("main api file is " + mainApiFile));
        lexer.setCharStream(apiReader.read(mainApiFile));

        // Using the lexer as the token source, we create a token
        // stream to be consumed by the parser
        //
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        // Now we need an instance of our parser
        //
        TParser parser = new TParser(tokens);

        hmxdef_return psrReturn = parser.hmxdef();
        Tree t = psrReturn.getTree();

        CommonTreeNodeStream ns = new CommonTreeNodeStream(t);
        ns.setTokenStream(tokens);
        TTree walker = TreeFactory.createJavaTreeWalker(ns, templateLib);
        System.out.println("Generating persistence files.");

        if (genType.equals(GenType.API)) {
            walker.apiGen();
        } else {
            walker.sdkGen();
        }

        return walker.getStorableAttributes();
    }

    private static boolean isSlateMd(String language) {
        return language.equals("SlateMd");
    }

}
