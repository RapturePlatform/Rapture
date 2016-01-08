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
package reflex.file;

import java.util.ArrayList;
import java.util.List;

import rapture.common.impl.jackson.JacksonUtil;
import reflex.IReflexIOHandler;
import reflex.IReflexLineCallback;
import reflex.node.io.FileReadAdapter;
import reflex.value.ReflexStreamValue;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexVoidValue;

public class JSONArrayReadAdapter implements FileReadAdapter {

    public JSONArrayReadAdapter(String param1, String param2) {
    }

    @Override
    public ReflexValue readContent(ReflexStreamValue file, IReflexIOHandler ioHandler) {
        // Each line is json string document - convert to a map
        // The file is a series of CSV records
        final List<ReflexValue> valArray = new ArrayList<ReflexValue>();

        ioHandler.forEachLine(file, new IReflexLineCallback() {

            @Override
            public ReflexValue callback(String line) {
                if (!line.isEmpty()) {
                    try {
                        Object x = JacksonUtil.getMapFromJson(line);
                        valArray.add(new ReflexValue(x));
                    } catch (Exception e) {
                        e.printStackTrace(); // for now
                    }
                }
                return new ReflexVoidValue();
            }

        });

        return new ReflexValue(valArray);
    }

}
