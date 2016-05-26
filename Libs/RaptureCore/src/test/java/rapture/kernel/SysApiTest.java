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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import rapture.common.ConnectionInfo;
import rapture.common.RaptureConstants;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.api.SysApi;
import rapture.common.connection.ConnectionType;

public class SysApiTest extends AbstractFileTest {

    private static final String auth = "test" + System.currentTimeMillis();
    private static final String REPO_USING_FILE = "REP {} USING FILE {prefix=\"/tmp/" + auth + "\"}";
    private static final File temp = new File("/tmp/" + auth);
    private static final String scriptAuthorityURI = "script://" + auth;
    private static final String scriptURI = scriptAuthorityURI + "/For/A/Jesters/Tear";

    private static ScriptApiImpl scriptImpl;

    String saveRaptureRepo;
    String saveInitSysConfig;

    @BeforeClass
    static public void setUp() {
        AbstractFileTest.setUp();
        config.RaptureRepo = REPO_USING_FILE;
        config.InitSysConfig = "NREP {} USING MEMORY { prefix=\"/tmp/" + auth + "/sys.config\"}";

        Kernel.initBootstrap();
        callingContext = ContextFactory.getKernelUser();

        Kernel.INSTANCE.clearRepoCache(false);
        Kernel.getAudit().createAuditLog(ContextFactory.getKernelUser(), new RaptureURI(RaptureConstants.DEFAULT_AUDIT_URI, Scheme.LOG).getAuthority(),
                "LOG {} using MEMORY {prefix=\"/tmp/" + auth + "\"}");
        Kernel.getLock().createLockManager(callingContext, "lock://kernel", "LOCKING USING DUMMY {}", "");
        scriptImpl = new ScriptApiImpl(Kernel.INSTANCE);
        Kernel.getIdGen().setupDefaultIdGens(callingContext, false);
    }

    private ConnectionInfo createInfo(String name, String host) {
        ConnectionInfo info = new ConnectionInfo();
        info.setHost(host);
        info.setUsername("rapture");
        info.setPassword("rapture");
        info.setDbName("RaptureDB");
        info.setInstanceName(name);
        return info;
    }

    @Test
    public void testConnInfo() {
        SysApi sys = Kernel.getSys();
        ConnectionInfo info = createInfo("foo", "localhost");
        Map<String, ConnectionInfo> map = sys.getConnectionInfo(callingContext, ConnectionType.MONGODB.toString());
        assertNotNull(map);
        assertNull(map.get(info.getInstanceName()));

        sys.setConnectionInfo(callingContext, ConnectionType.MONGODB.toString(), info.getInstanceName(), info);
        map = sys.getConnectionInfo(callingContext, ConnectionType.MONGODB.toString());
        assertEquals(info, map.get(info.getInstanceName()));

        ConnectionInfo info2 = createInfo("bar", "127.0.0.1");
        // For some reason equals method does not compare instance names
        // however it does compare hosts, which must match exactly.
        // So localhost != 127.0.0.1
        assertNotEquals(info, info2);

        sys.setConnectionInfo(callingContext, ConnectionType.MONGODB.toString(), info2.getInstanceName(), info2);
        map = sys.getConnectionInfo(callingContext, ConnectionType.MONGODB.toString());
        assertEquals(info2, map.get(info2.getInstanceName()));

        sys.deleteConnectionInfo(callingContext, ConnectionType.MONGODB.toString(), info.getInstanceName());
        map = sys.getConnectionInfo(callingContext, ConnectionType.MONGODB.toString());
        assertNull(map.get(info.getInstanceName()));
        assertEquals(info2, map.get(info2.getInstanceName()));

        sys.deleteConnectionInfo(callingContext, ConnectionType.MONGODB.toString(), info2.getInstanceName());

        map = sys.getConnectionInfo(callingContext, ConnectionType.MONGODB.toString());
        assertNull(map.get(info.getInstanceName()));
        assertNull(map.get(info2.getInstanceName()));

        // Already deleted - should do nothing
        sys.deleteConnectionInfo(callingContext, ConnectionType.MONGODB.toString(), info2.getInstanceName());

    }
}
