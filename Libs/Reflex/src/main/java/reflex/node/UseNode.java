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
package reflex.node;

import java.util.List;

import rapture.common.RaptureRemote;
import rapture.common.client.CredentialsProvider;
import rapture.common.client.HttpLoginApi;
import rapture.common.client.ScriptClient;
import reflex.IReflexHandler;
import reflex.ReflexException;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexVoidValue;

public class UseNode extends BaseNode {

    private String remoteEnv;

    public UseNode(int lineNumber, IReflexHandler handler, Scope scope, String remoteEnvironment) {
        super(lineNumber, handler, scope);
        this.remoteEnv = remoteEnvironment;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        debugger.stepStart(this, scope);
        ReflexValue ret = new ReflexVoidValue(lineNumber);
        // If remoteEnv is null, reset the api
        // otherwise this should be the name of a RemoteEnv - create a
        // ScriptingApi instance
        // connecting to that environment and switch to that.
        if (remoteEnv == null) {
            handler.resetApi();
        } else {
            List<RaptureRemote> remotes = handler.getApi().getAdmin().getRemotes();
            boolean switched = false;
            for (RaptureRemote r : remotes) {
                if (r.getName().equals(remoteEnv)) {
                    switchTo(r);
                    switched = true;
                    break;
                }
            }
            if (!switched) {
                throw new ReflexException(lineNumber, "No remote known as " + remoteEnv);
            }
        }
        debugger.stepEnd(this, ret, scope);
        return ret;

    }

    private void switchTo(final RaptureRemote r) {
        // Connect to remote
        try {
            HttpLoginApi loginApi = new HttpLoginApi(r.getUrl(), new CredentialsProvider() {

                @Override
                public String getUserName() {
                    return r.getApiKey();
                }

                @Override
                public String getPassword() {
                    return r.getOptionalPass();
                }
            });

            loginApi.login();
            ScriptClient sc = new ScriptClient(loginApi);
            handler.switchApi(sc);
        } catch (Exception e) {
            throw new ReflexException(lineNumber, "Could not connect to remote - " + e.getMessage());
        }
    }
}
