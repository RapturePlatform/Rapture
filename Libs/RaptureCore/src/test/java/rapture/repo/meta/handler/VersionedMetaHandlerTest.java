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
package rapture.repo.meta.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by zanniealvarez on 11/12/15.
 */

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import rapture.common.exception.RaptureException;
import rapture.repo.KeyStore;
import rapture.repo.mem.MemKeyStore;

//TODO: rework this class.  Fails too often
@Ignore
public class VersionedMetaHandlerTest {
    private VersionedMetaHandler handler;

    @Before
    public void setup() {
        Map<String, String> config = new HashMap<String, String>();

        KeyStore store = new MemKeyStore();
        store.setConfig(config);

        KeyStore version = new MemKeyStore();
        version.setConfig(config);

        KeyStore meta = new MemKeyStore();
        meta.setConfig(config);

        KeyStore attribute = new MemKeyStore();
        attribute.setConfig(config);

        handler = new VersionedMetaHandler(store, version, meta, attribute);
    }

    @Test
    public void testGetVersionNumberAsOfTime() throws InterruptedException {
        String docName = "testDoc";
        int numVersions = 3;
        long[] versionTimes = putVersionsOfDocInRepo(docName, numVersions, 2);

        for (int i = 0; i < numVersions; i++) {
            doTestAsOfTime(docName, versionTimes[i], i + 1, "Exact time version was submitted should return that version");
        }

        doTestAsOfTime(docName, versionTimes[numVersions - 1] + 1, numVersions, "With AsOfTime later than last version, get last version");
        doTestAsOfTime(docName, versionTimes[numVersions - 1] - 1, numVersions - 1, "With AsOfTime between two versions, get earlier version");

        long timestamp = versionTimes[0] - 1;
        assertNull("Should get null if document didn't exist yet", handler.getVersionNumberAsOfTime(docName, "t" + timestamp));

        try {
            handler.deleteOldVersions(docName, 1);
            timestamp = versionTimes[0] + 1;
            assertNull("Should get an exception if document had existed but has since been deleted",
                    handler.getVersionNumberAsOfTime(docName, "t" + timestamp));
        } catch (RaptureException e) {
        }
    }

    protected void doTestAsOfTime(String docName, long milliseconds, int expectedVersion, String testMessage) {
        assertEquals(testMessage, expectedVersion, (long) handler.getVersionNumberAsOfTime(docName, "t" + milliseconds));
    }

    private long[] putVersionsOfDocInRepo(String docName, int numVersions, int microsecondsBetween) throws InterruptedException {
        long[] versionTimes = new long[numVersions];
        for (int i = 0; i < numVersions; i++) {
            versionTimes[i] = System.currentTimeMillis();
            String jsonContent = "{ \"version\" : \"" + (i + 1) + "\" }";

            handler.addDocument(docName, jsonContent, "rapture", "test comment", null);

            Thread.sleep(microsecondsBetween);
        }
        return versionTimes;
    }
}
