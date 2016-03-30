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
 *         <p>
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
