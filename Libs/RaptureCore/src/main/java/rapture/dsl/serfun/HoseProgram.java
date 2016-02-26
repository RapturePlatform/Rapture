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
package rapture.dsl.serfun;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;

import rapture.common.Hose;
import rapture.common.SeriesValue;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.generated.HoseLexer;
import rapture.generated.HoseParser;
import rapture.generated.HoseWalker;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class HoseProgram {
    private final Map<String, HoseArg> links = Maps.newHashMap();
    private static final HoseLexer lexer = new HoseLexer();
    private String name;
    private List<HoseParm> inParms, outParms;
    private List<String> inNames, outNames;
    Function<HoseParm, String> parmNameFunction = new Function<HoseParm, String>() {
        public String apply(HoseParm parm) {
            return parm.getName();
        }
    };

    public void addLink(String varname, HoseArg value) {
        if (links.put(varname, value) != null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "Error: duplicate variable declaration " + varname);
        }
    }

    public HoseArg getLink(String varname) {
        HoseArg result = links.get(varname);
        if (result == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "Error: unknown variable " + varname);
        }
        return result;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setInParms(List<HoseParm> in) {
        this.inParms = in;
        this.inNames = Lists.transform(in, parmNameFunction);

        for (String name : inNames) {
            addLink(name, new HoseArg());
        }
    }

    public void setOutParms(List<HoseParm> out) {
        this.outParms = out;
        this.outNames = Lists.transform(out, parmNameFunction);
    }

    public int getInputCount() {
        return inParms.size();
    }

    public int getOutputCount() {
        return outParms.size();
    }

    public List<String> getInputKeys() {
        return inNames;
    }

    public List<String> getOutputKeys() {
        return outNames;
    }

    public String getName() {
        return name;
    }

    public static HoseFactory compile(String program) throws RecognitionException {
        lexer.setCharStream(new ANTLRStringStream(program));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        HoseParser parser = new HoseParser(tokens);
        CommonTree tree = (CommonTree) parser.program().getTree();

        CommonTreeNodeStream nodes = new CommonTreeNodeStream(tree);
        HoseWalker walker = new HoseWalker(nodes);
        walker.program();
        return walker.program.new Factory();
    }

    public class Factory implements HoseFactory {
        @Override
        public Hose make(List<HoseArg> args) {
            return new Runner(args, HoseProgram.this);
        }
    }

    public static class Runner extends ComplexHose {
        private final HoseProgram program;

        public Runner(List<HoseArg> args, HoseProgram program) {
            super(program.getInputCount(), program.getOutputCount());
            this.program = program;
            for (int i = 0; i < args.size(); i++) {
                // TODO MEL refactor this loop to avoid args.get(i)
                HoseArg inPort = (HoseArg) program.getLink(program.getInputKeys().get(i));
                upstream[i] = inPort;
                args.get(i).bind(inPort, 0, 0);
            }
            int i = 0;
            for (String key : getOutputKeys()) {
                downstream[i] = program.getLink(key);
                downstreamIndex[i] = 0;
                i++;
            }
        }

        @Override
        public void pushValue(SeriesValue v, int index) {
            downstream[index].pushValue(v);
        }

        @Override
        public void terminateStream(int index) {
            downstream[index].terminateStream();
        }

        @Override
        public void terminateStream() {
            terminateStream(0);
        }

        @Override
        public SeriesValue pullValue(int index) {
            return downstream[index].pullValue();
        }

        @Override
        public final String getName() {
            return program.getName();
        }

        @Override
        public final List<String> getInputKeys() {
            return program.getInputKeys();
        }

        @Override
        public final List<String> getOutputKeys() {
            return program.getOutputKeys();
        }
    }
}
