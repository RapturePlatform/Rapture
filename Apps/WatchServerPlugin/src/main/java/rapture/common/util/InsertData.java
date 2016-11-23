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
package rapture.common.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import rapture.common.CallingContext;
import rapture.kernel.Kernel;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class InsertData implements Runnable {

    private List docs = new ArrayList<>();
    private List uris = new ArrayList<>();
    private CallingContext ctx;
    private static Logger log = Logger.getLogger(InsertData.class);

    public InsertData(CallingContext ctx, List<String> docList, List uris2) {
        this.ctx = ctx;
        this.docs = docList;
        this.uris = uris2;
    }

    @Override
    public void run() {
        List putDocs = Kernel.getDoc().putDocs(ctx, uris, docs);
        log.info("Inserted " + putDocs.size() + " into repo " + uris.get(0));
    }
}
