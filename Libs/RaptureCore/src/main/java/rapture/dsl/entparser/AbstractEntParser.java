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
package rapture.dsl.entparser;

import org.antlr.runtime.Parser;
import org.antlr.runtime.RecognizerSharedState;
import org.antlr.runtime.TokenStream;

import rapture.common.IEntitlementsContext;

public abstract class AbstractEntParser extends Parser {
    private IEntitlementsContext ectx;
    private StringBuilder path = new StringBuilder();

    public String getPath() {
        return path.toString();
    }

    protected AbstractEntParser(TokenStream input, RecognizerSharedState state) {
        super(input, state);
    }

    protected void addPath(String value) {
        if (path.length() != 0) {
            path.append("/");
        }
        path.append(value);
    }

    protected String getDocPath() {
        return ectx.getDocPath();
    }
    
    protected String getAuthority() {
        return ectx.getAuthority();
    }
    
    protected String getFullPath() {
        return ectx.getFullPath();
    }

    public IEntitlementsContext getEctx() {
        return ectx;
    }

    public void setEctx(IEntitlementsContext entCtx) {
        this.ectx = entCtx;
    }
}
