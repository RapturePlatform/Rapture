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
package rapture.kernel;

import rapture.common.CallingContext;
import rapture.common.EntitlementConst;
import rapture.common.IEntitlementsContext;
import rapture.common.RaptureContextInfo;
import rapture.common.RaptureScript;
import rapture.common.exception.RaptureException;
import rapture.common.impl.jackson.JsonContent;
import rapture.common.model.RaptureViewResult;
import rapture.generated.DParseParser.displayname_return;
import rapture.repo.RepoVisitor;
import rapture.script.IRaptureScript;
import rapture.script.RaptureDataContext;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

public class UserViewVisitorApiImpl implements RepoVisitor {
    private static Logger log = Logger.getLogger(UserViewVisitorApiImpl.class);

    private RaptureContextInfo info;

    private RaptureScript mapScript;

    private RaptureScript filterScript;

    private IRaptureScript scriptHandler;

    private displayname_return disp;

    private RaptureViewResult result;

    private Map<String, Object> realParams;

    private String viewName;

    private CallingContext context;

    public RaptureViewResult getResult() {
        return result;
    }

    public String getViewName() {
        return viewName;
    }

    public void setContext(CallingContext context) {
        this.context = context;
    }

    public void setDisp(displayname_return disp) {
        this.disp = disp;
    }

    public void setFilterScript(RaptureScript filterScript) {
        this.filterScript = filterScript;
    }

    public void setInfo(RaptureContextInfo info) {
        this.info = info;
    }

    public void setMapScript(RaptureScript mapScript) {
        this.mapScript = mapScript;
    }

    public void setRealParams(Map<String, Object> realParams) {
        this.realParams = realParams;
    }

    public void setResult(RaptureViewResult result) {
        this.result = result;
    }

    public void setScriptHandler(IRaptureScript scriptHandler) {
        this.scriptHandler = scriptHandler;
    }

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    @Override
    public boolean visit(final String name, JsonContent content, boolean isFolder) {
        if (!isFolder) {
            // Load the document, convert to a
            // RaptureDataContext, pass it to the filterScript,
            // if that
            // returns true, run it through the Map function.

            // Check the entitlement for this document

            try {
                Kernel.getKernel().validateContext(context, EntitlementConst.DOC_DATABATCH, new IEntitlementsContext() {

                    @Override
                    public String getDocPath() {
                        return disp.type + "/" + name;
                    }

                    @Override
                    public String getAuthority() {
                        return "";
                    }

                    @Override
                    public String getFullPath() {
                        return info.getAuthority();
                    }

                });
            } catch (RaptureException e) {
                log.error(e.getMessage());
                log.info("Failed validation check for " + name + " so not using it in view");
            }

            RaptureDataContext dataContext = new RaptureDataContext();
            dataContext.setDisplayName(name);
            dataContext.putJsonContent(content);
            dataContext.setUser("FIXME");
            dataContext.setWhen(new Date());

            // With the results, add to the RaptureViewResult.
            try {
                if (scriptHandler.runFilter(context, filterScript, dataContext, realParams)) {
                    List<Object> values = scriptHandler.runMap(context, mapScript, dataContext, realParams);
                    // Merge values into RaptureViewResult

                    result.startNewRow();
                    if (values != null) {
                        for (Object x : values) {
                            result.addValue(x);
                        }
                    }
                }
            } catch (RaptureException e) {
                // /
            }
        }
        return true;
    }

}
