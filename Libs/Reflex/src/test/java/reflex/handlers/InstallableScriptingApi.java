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
package reflex.handlers;

import rapture.common.InstallableKernel;
import rapture.common.api.ScriptActivityApi;
import rapture.common.api.ScriptAdminApi;
import rapture.common.api.ScriptAsyncApi;
import rapture.common.api.ScriptAuditApi;
import rapture.common.api.ScriptBlobApi;
import rapture.common.api.ScriptJarApi;
import rapture.common.api.ScriptBootstrapApi;
import rapture.common.api.ScriptDecisionApi;
import rapture.common.api.ScriptDocApi;
import rapture.common.api.ScriptEntitlementApi;
import rapture.common.api.ScriptEnvironmentApi;
import rapture.common.api.ScriptEventApi;
import rapture.common.api.ScriptPluginApi;
import rapture.common.api.ScriptFieldsApi;
import rapture.common.api.ScriptIdGenApi;
import rapture.common.api.ScriptIndexApi;
import rapture.common.api.ScriptLockApi;
import rapture.common.api.ScriptMailboxApi;
import rapture.common.api.ScriptNotificationApi;
import rapture.common.api.ScriptPipelineApi;
import rapture.common.api.ScriptQuestionApi;
import rapture.common.api.ScriptRelationshipApi;
import rapture.common.api.ScriptRepoApi;
import rapture.common.api.ScriptRunnerApi;
import rapture.common.api.ScriptScheduleApi;
import rapture.common.api.ScriptScriptApi;
import rapture.common.api.ScriptSeriesApi;
import rapture.common.api.ScriptSheetApi;
import rapture.common.api.ScriptStructuredApi;
import rapture.common.api.ScriptSysApi;
import rapture.common.api.ScriptTableApi;
import rapture.common.api.ScriptUserApi;
import rapture.common.api.ScriptingApi;

public class InstallableScriptingApi implements ScriptingApi {

    @Override
    public ScriptActivityApi getActivity() {
        return null;
    }

    @Override
    public ScriptBootstrapApi getBootstrap() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ScriptScriptApi getScript() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ScriptLockApi getLock() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ScriptNotificationApi getNotification() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ScriptIndexApi getIndex() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ScriptAdminApi getAdmin() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ScriptIdGenApi getIdGen() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ScriptEntitlementApi getEntitlement() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ScriptUserApi getUser() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ScriptScheduleApi getSchedule() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ScriptEventApi getEvent() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ScriptAuditApi getAudit() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ScriptMailboxApi getMailbox() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ScriptFieldsApi getFields() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ScriptBlobApi getBlob() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ScriptJarApi getJar() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ScriptSheetApi getSheet() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ScriptPluginApi getPlugin() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ScriptPipelineApi getPipeline() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ScriptAsyncApi getAsync() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ScriptSysApi getSys() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ScriptRunnerApi getRunner() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ScriptSeriesApi getSeries() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ScriptDecisionApi getDecision() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ScriptRepoApi getRepo() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ScriptDocApi getDoc() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ScriptRelationshipApi getRelationship() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public InstallableKernel getInstalledKernel(String name) {
        return new TestSDK();
    }

	@Override
	public ScriptTableApi getTable() {
		// TODO Auto-generated method stub
		return null;
	}

    @Override
    public ScriptEnvironmentApi getEnvironment() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ScriptQuestionApi getQuestion() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getEndPoint() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getSerializedContext() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ScriptStructuredApi getStructured() {
        // TODO Auto-generated method stub
        return null;
    }
}
