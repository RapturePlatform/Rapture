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
package rapture.common.client;

import rapture.common.InstallableKernel;
import rapture.common.api.ScriptActivityApi;
import rapture.common.api.ScriptingApi;

/**
 * This class is intended to mirror the action of RaptureKernelScript in structure, so that we can potentially run scripts locally, knowing that locally they
 * will call this class, and when run on a RaptureServer they will invoke the RaptureKernelScript.
 * 
 * As they both have the same fundamental structure the script should work just fine either way.
 * 
 * @author amkimian
 * 
 */
public class ScriptClient implements ScriptingApi {
    private final HttpActivityApi activity;
    private final HttpAdminApi admin;
    private final HttpBootstrapApi bootstrap;
    private final HttpEntitlementApi entitlement;
    private final HttpIdGenApi idgen;
    private final HttpIndexApi index;
    private final HttpScriptApi script;
    private final HttpUserApi user;
    private final HttpScheduleApi schedule;
    private final HttpLockApi lock;
    private final HttpEventApi event;
    private final HttpAuditApi audit;
    private final HttpFieldsApi fields;
    private final HttpPluginApi plugin;
    private final HttpPipelineApi pipeline;
    private final HttpAsyncApi async;
    private final HttpSysApi sys;
    private final HttpRunnerApi runner;
    private final HttpNotificationApi notification;
    private final HttpBlobApi blob;
    private final HttpJarApi jar;
    private final HttpSeriesApi series;
    private final HttpDocApi doc;
    private final HttpDecisionApi decision;
    private final HttpEnvironmentApi environment;
    private final HttpLoginApi login;
    private final HttpStructuredApi structured;
    private final HttpSearchApi search;
    private final HttpTagApi tag;

    public ScriptClient(HttpLoginApi loginApi) {
        login = loginApi;
        admin = new HttpAdminApi(loginApi);
        activity = new HttpActivityApi(loginApi);
        bootstrap = new HttpBootstrapApi(loginApi);
        entitlement = new HttpEntitlementApi(loginApi);
        idgen = new HttpIdGenApi(loginApi);
        index = new HttpIndexApi(loginApi);
        script = new HttpScriptApi(loginApi);
        user = new HttpUserApi(loginApi);
        schedule = new HttpScheduleApi(loginApi);
        lock = new HttpLockApi(loginApi);
        event = new HttpEventApi(loginApi);
        audit = new HttpAuditApi(loginApi);
        fields = new HttpFieldsApi(loginApi);
        plugin = new HttpPluginApi(loginApi);
        pipeline = new HttpPipelineApi(loginApi);
        async = new HttpAsyncApi(loginApi);
        sys = new HttpSysApi(loginApi);
        runner = new HttpRunnerApi(loginApi);
        notification = new HttpNotificationApi(loginApi);
        blob = new HttpBlobApi(loginApi);
        jar = new HttpJarApi(loginApi);
        series = new HttpSeriesApi(loginApi);
        doc = new HttpDocApi(loginApi);
        decision = new HttpDecisionApi(loginApi);
        environment = new HttpEnvironmentApi(loginApi);
        structured = new HttpStructuredApi(loginApi);
        search = new HttpSearchApi(loginApi);
        tag = new HttpTagApi(loginApi);
    }

    public HttpLoginApi getLogin() {
        return login;
    }

    @Override
    public ScriptActivityApi getActivity() {
        return activity;
    }

    public HttpAdminApi getAdmin() {
        return admin;
    }

    public HttpAuditApi getAudit() {
        return audit;
    }

    public HttpBootstrapApi getBootstrap() {
        return bootstrap;
    }

    public HttpEntitlementApi getEntitlement() {
        return entitlement;
    }

    public HttpEventApi getEvent() {
        return event;
    }

    public HttpFieldsApi getFields() {
        return fields;
    }

    public HttpIdGenApi getIdGen() {
        return idgen;
    }

    public HttpPluginApi getPlugin() {
        return plugin;
    }

    public HttpIndexApi getIndex() {
        return index;
    }

    public HttpLockApi getLock() {
        return lock;
    }

    public HttpScheduleApi getSchedule() {
        return schedule;
    }

    public HttpScriptApi getScript() {
        return script;
    }

    public HttpUserApi getUser() {
        return user;
    }

    public HttpPipelineApi getPipeline() {
        return pipeline;
    }

    public HttpAsyncApi getAsync() {
        return async;
    }

    public HttpSysApi getSys() {
        return sys;
    }

    public HttpRunnerApi getRunner() {
        return runner;
    }

    public HttpNotificationApi getNotification() {
        return notification;
    }

    public HttpBlobApi getBlob() {
        return blob;
    }

    public HttpJarApi getJar() {
        return jar;
    }

    public HttpSeriesApi getSeries() {
        return series;
    }

    public HttpDocApi getDoc() {
        return doc;
    }

    public HttpEnvironmentApi getEnvironment() {
        return environment;
    }

    @Override
    public HttpDecisionApi getDecision() {
        return decision;
    }

    public HttpStructuredApi getStructured() {
        return structured;
    }

    @Override
    public InstallableKernel getInstalledKernel(String name) {
        return null;
    }

    @Override
    public String getEndPoint() {
        return login.currentUrl;
    }

    @Override
    public String getSerializedContext() {
        return login.getContext().getContext();
    }

    @Override
    public HttpSearchApi getSearch() {
        return search;
    }
    
    @Override
    public HttpTagApi getTag() {
    	return tag;
    }
}
