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
package rapture.dsl.serfun;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.List;

import com.google.common.base.Preconditions;

import rapture.common.Hose;
import rapture.common.SeriesValue;

public class FileHose extends SimpleHose {
    private final PrintStream out;
    
    public FileHose(HoseArg in, PrintStream out) {
        super(in);
        this.out = out;
    }
    
    @Override
    public String getName() {
        return "toFile(stream, filePath);";
    }

    @Override
    public void pushValue(SeriesValue v) {
        out.print(v.asString());
        downstream.pushValue(v);
    }

    @Override
    public SeriesValue pullValue() {
        SeriesValue sv = upstream.pullValue();
        if (sv != null) {
            out.print(sv.asString());
        }
        return sv;
    }
    
    public static class Factory implements HoseFactory {
        @Override
        public Hose make(List<HoseArg> args) {
            //TODO limit filter path according to entitlement
            Preconditions.checkArgument(args.size() == 2, "Wrong number of arguments to skipNaN()");
            HoseArg input = args.get(0);
            Preconditions.checkArgument(input.isSeries());
            HoseArg filePath = args.get(1);
            Preconditions.checkArgument(filePath.isString());
            File f = new File(filePath.asString());
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(f);
            } catch(FileNotFoundException ex) {
                throw new IllegalStateException("Unable to create file: "+filePath);
            }
            PrintStream ps = new PrintStream(fos);
            return new FileHose(input, ps);
        }
    }

    @Override
    public boolean asBoolean() {
        return false;
    }

    @Override
    public boolean isBoolean() {
        return false;
    }
}
