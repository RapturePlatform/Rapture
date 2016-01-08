/**
 * Copyright (C) 2011-2015 Incapture Technologies LLC
 *
 * This is an autogenerated license statement. When copyright notices appear below
 * this one that copyright supercedes this statement.
 *
 * Unless required by applicable law or agreed to in writing, software is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 *
 * Unless explicit permission obtained in writing this software cannot be distributed.
 */
package rapture.repo;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import org.h2.tools.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;

/**
 * @author bardhi
 *         <p/>
 *         unfortunately this doesn't work, because h2 does not understand the json data types. if they add support in the future, this can hopefully
 *         be re-enabled...
 * @since 4/2/15.
 */
@Ignore
public class PgUnversionedRepoTest extends UnversionedRepoTest {

    private static Server server;

    @BeforeClass
    public static void beforeThisClass() throws Exception {
        setupH2Server();
    }

    private static void setupH2Server() throws IOException, SQLException {
        File baseDir = File.createTempFile("h2server", "pgserver");
        baseDir.delete();
        baseDir.mkdir();
        server = Server.createPgServer("-pgPort", "12345", "-pgAllowOthers", "-baseDir", baseDir.getAbsolutePath()).start();
    }

    @Before
    public void setUp() throws Exception {
        setupH2Server();
    }

    @After
    public void tearDown() throws Exception {
        server.stop();
    }

    @Override
    protected String getConfig(String authority) {
        return "REP {} USING POSTGRES { prefix=\"rapture.bootstrap\" }";
    }

}
