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
package rapture.kernel.script;

import rapture.common.CallingContext;
import rapture.common.InstallableKernel;
import rapture.common.api.ScriptUserApi;
import rapture.common.api.ScriptingApi;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.kernel.Kernel;
import rapture.kernel.Login;
import rapture.script.IRaptureKernelScriptHelper;
import rapture.script.IRaptureScriptHelper;

public class KernelScript implements IRaptureScriptHelper, IRaptureKernelScriptHelper, ScriptingApi {
    private final Login login;
    private final ScriptActivity activity;
    private final ScriptAdmin admin;
    private final ScriptBootstrap bootstrap;
    private final ScriptEntitlement entitlement;
    private final ScriptIdGen idgen;
    private final ScriptIndex index;
    private final ScriptScript script;
    private final ScriptUser user;
    private final ScriptSchedule schedule;
    private final ScriptLock lock;
    private final ScriptEvent event;
    private final ScriptAudit audit;
    private final ScriptTransform transform;
    private final ScriptEntity entity;
    private final ScriptPlugin plugin;
    private final ScriptPipeline pipeline;
    private final ScriptAsync async;
    private final ScriptSys sys;
    private final ScriptRunner runner;
    private final ScriptNotification notification;
    private final ScriptSeries series;
    private final ScriptBlob blob;
    private final ScriptJar jar;
    private final ScriptDecision decision;
    private final ScriptDoc doc;
    private final ScriptEnvironment environment;
    private final ScriptStructured structured;
    private final ScriptSearch search;
    private final ScriptTag tag;
    private final ScriptOperation operation;

    public KernelScript() {
        login = Kernel.getLogin();
        activity = new ScriptActivity(Kernel.getActivity());
        admin = new ScriptAdmin(Kernel.getAdmin());
        bootstrap = new ScriptBootstrap(Kernel.getBootstrap());
        entitlement = new ScriptEntitlement(Kernel.getEntitlement());
        idgen = new ScriptIdGen(Kernel.getIdGen());
        index = new ScriptIndex(Kernel.getIndex());
        script = new ScriptScript(Kernel.getScript());
        user = new ScriptUser(Kernel.getUser());
        schedule = new ScriptSchedule(Kernel.getSchedule());
        lock = new ScriptLock(Kernel.getLock());
        event = new ScriptEvent(Kernel.getEvent());
        audit = new ScriptAudit(Kernel.getAudit());
        transform = new ScriptTransform(Kernel.getTransform());
        entity = new ScriptEntity(Kernel.getEntity());
        plugin = new ScriptPlugin(Kernel.getPlugin());
        pipeline = new ScriptPipeline(Kernel.getPipeline());
        async = new ScriptAsync(Kernel.getAsync());
        sys = new ScriptSys(Kernel.getSys());
        runner = new ScriptRunner(Kernel.getRunner());
        notification = new ScriptNotification(Kernel.getNotification());
        series = new ScriptSeries(Kernel.getSeries());
        blob = new ScriptBlob(Kernel.getBlob());
        jar = new ScriptJar(Kernel.getJar());
        decision = new ScriptDecision(Kernel.getDecision());
        doc = new ScriptDoc(Kernel.getDoc());
        environment = new ScriptEnvironment(Kernel.getEnvironment());
        structured = new ScriptStructured(Kernel.getStructured());
        search = new ScriptSearch(Kernel.getSearch());
        tag = new ScriptTag(Kernel.getTag());
        operation = new ScriptOperation(Kernel.getOperation());
    }

    public Login getLogin() {
        return login;
    }

    public ScriptAdmin getAdmin() {
        return admin;
    }

    public ScriptAudit getAudit() {
        return audit;
    }

    @Override
    public ScriptActivity getActivity() {
        return activity;
    }

    public ScriptBootstrap getBootstrap() {
        return bootstrap;
    }

    public ScriptEntitlement getEntitlement() {
        return entitlement;
    }

    public ScriptEvent getEvent() {
        return event;
    }

    public ScriptTransform getTransform() {
        return transform;
    }
    
    public ScriptEntity getEntity() {
    	return entity;
    }

    public ScriptIdGen getIdGen() {
        return idgen;
    }

    public ScriptIndex getIndex() {
        return index;
    }

    public ScriptLock getLock() {
        return lock;
    }

    public ScriptPlugin getPlugin() {
        return plugin;
    }

    @Override
    public String getName() {
        return "Rapture Kernel";
    }

    public ScriptSchedule getSchedule() {
        return schedule;
    }

    public ScriptScript getScript() {
        return script;
    }

    public ScriptUser getUser() {
        return user;
    }

    public ScriptPipeline getPipeline() {
        return pipeline;
    }

    public ScriptAsync getAsync() {
        return async;
    }

    public ScriptSys getSys() {
        return sys;
    }

    public ScriptRunner getRunner() {
        return runner;
    }

    public ScriptNotification getNotification() {
        return notification;
    }

    public ScriptEnvironment getEnvironment() {
        return environment;
    }

    public ScriptStructured getStructured() {
        return structured;
    }

    public ScriptTag getTag() {
        return tag;
    }
    
    public ScriptOperation getOperation() {
    	return operation;
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    public ScriptSeries getSeries() {
        return series;
    }

    private CallingContext ctx;

    @Override
    public void setCallingContext(CallingContext ctx) {
        this.ctx = ctx;
        getActivity().setCallingContext(ctx);
        getAdmin().setCallingContext(ctx);
        getBootstrap().setCallingContext(ctx);
        getEntitlement().setCallingContext(ctx);
        getIdGen().setCallingContext(ctx);
        getIndex().setCallingContext(ctx);
        getScript().setCallingContext(ctx);
        getUser().setCallingContext(ctx);
        getSchedule().setCallingContext(ctx);
        getLock().setCallingContext(ctx);
        getEvent().setCallingContext(ctx);
        getAudit().setCallingContext(ctx);
        getTransform().setCallingContext(ctx);
        getPipeline().setCallingContext(ctx);
        getAsync().setCallingContext(ctx);
        getSys().setCallingContext(ctx);
        getRunner().setCallingContext(ctx);
        getNotification().setCallingContext(ctx);
        getSeries().setCallingContext(ctx);
        getBlob().setCallingContext(ctx);
        getDecision().setCallingContext(ctx);
        getDoc().setCallingContext(ctx);
        getEnvironment().setCallingContext(ctx);
        getPlugin().setCallingContext(ctx);
        getStructured().setCallingContext(ctx);
        getSearch().setCallingContext(ctx);
        getTag().setCallingContext(ctx);
        getJar().setCallingContext(ctx);
        getOperation().setCallingContext(ctx);

        for (InstallableKernel installedKernel : Kernel.getInstalledKernels()) {
            installedKernel.getKernelScript().setCallingContext(ctx);
        }
    }

    public ScriptUserApi user() {
        return getUser();
    }

    public ScriptBlob getBlob() {
        return blob;
    }

    public ScriptJar getJar() {
        return jar;
    }

    @Override
    public void setKernelScriptHelper(IRaptureKernelScriptHelper helper) {
    }

    @Override
    public ScriptDecision getDecision() {
        return decision;
    }

    @Override
    public ScriptDoc getDoc() {
        return doc;
    }

    @Override
    public InstallableKernel getInstalledKernel(String name) {
        return Kernel.getInstalledKernel(name);
    }

    @Override
    public String getEndPoint() {
        return null;
    }

    @Override
    public String getSerializedContext() {
        return JacksonUtil.jsonFromObject(ctx);
    }

    @Override
    public ScriptSearch getSearch() {
        return search;
    }

}
