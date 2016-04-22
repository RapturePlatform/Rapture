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
package rapture.dp.invocable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.text.SimpleDateFormat;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.RaptureConstants;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.SeriesPoint;
import rapture.common.api.BlobApi;
import rapture.common.api.DecisionApi;
import rapture.common.api.DocApi;
import rapture.common.api.ScriptApi;
import rapture.common.api.SeriesApi;
import rapture.config.RaptureConfig;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

public class CheckPrerequisiteStepTest {

    String prereqURI = "//datacapture/test/prerequisite";
    private static final int TIMEOUT = 5000;

    // / START GENERIC BOILERPLATE TEST STUFF

    DecisionApi dapi;
    BlobApi bapi;
    DocApi docapi = null;
    ScriptApi scrapi;
    SeriesApi serapi;

    private static final String BLOB_USING_MEMORY = "BLOB {} USING MEMORY {}";
    private static final String REPO_USING_MEMORY = "REP {} USING MEMORY {}";
    private static final String SERIES_USING_MEMORY = "SREP {} using MEMORY { }";
    private static final String blobAuthorityURI = "blob://foo/";
    private static final String blobURI = blobAuthorityURI + "bar";

    CallingContext context = null;

    @Before
    public void setUp() throws Exception {
        RaptureConfig.setLoadYaml(false);
        System.setProperty("LOGSTASH-ISENABLED", "false");

        dapi = Kernel.getDecision();
        bapi = Kernel.getBlob();
        docapi = Kernel.getDoc();
        scrapi = Kernel.getScript();
        serapi = Kernel.getSeries();

        context = ContextFactory.getKernelUser();
        Kernel.getAudit().createAuditLog(context, new RaptureURI(RaptureConstants.DEFAULT_AUDIT_URI, Scheme.LOG).getAuthority(), "LOG {} using MEMORY {}");
        Kernel.getLock().createLockManager(ContextFactory.getKernelUser(), "lock://kernel", "LOCKING USING DUMMY {}", "");

        if (!serapi.seriesRepoExists(context, "//monty")) serapi.createSeriesRepo(context, "//monty", SERIES_USING_MEMORY);

        if (!bapi.blobRepoExists(context, "//sys.blob")) bapi.createBlobRepo(context, "//sys.blob", BLOB_USING_MEMORY, REPO_USING_MEMORY);
        if (!bapi.blobRepoExists(context, blobAuthorityURI)) bapi.createBlobRepo(context, blobAuthorityURI, BLOB_USING_MEMORY, REPO_USING_MEMORY);
        bapi.putBlob(context, blobURI, "foo".getBytes(), "text/csv");

        // / END GENERIC BOILERPLATE TEST STUFF

    }

    @Test
    public void testCheckPrerequisiteStepInvokeTimeout() {
        long time = System.currentTimeMillis();
        final DateTime imminent = new DateTime().plusMillis(TIMEOUT);
        String prereq = "{" +
                "    \"requiredData\": [" +
                "        {" +
                "            \"uri\": \"blob://foo/<DATE>/bar\"," +
                "            \"timeNoEarlierThan\": \"23:59:59.000 " + imminent.getZone() + "\"," +
                "            \"keyFormat\": \"yyyyMMdd\"," +
                "            \"dateWithin\": \"5D\"" +
                "        }" +
                "    ]," +
                "    \"retryInMillis\": " + TIMEOUT / 10 + "," +
                "    \"cutoffTime\": \"" + new SimpleDateFormat("HH:mm:ss.000").format(imminent.toDate()) + " " + imminent.getZone() + "\"," +
                "    \"cutoffAction\": \"QUIT\"" +
                "}";

        docapi.putDoc(context, prereqURI, prereq);

        String workOrderURI = "workorder://test/prereq/" + System.currentTimeMillis();
        dapi.setContextLiteral(context, workOrderURI, "prerequisiteConfigUri", prereqURI);

        Kernel.getLock().createLockManager(ContextFactory.getKernelUser(), "lock://kernel", "LOCKING USING DUMMY {}", "");

        CheckPrerequisiteStep cps = new CheckPrerequisiteStep(workOrderURI, null);
        String nextStep = cps.invoke(context);
        long duration = System.currentTimeMillis() - time;
        System.out.println(nextStep + " " + duration);

        assertTrue(duration >= (TIMEOUT - 1000));
        assertTrue("Duration " + duration + " Timeout " + TIMEOUT + " (TIMEOUT + 2000) " + (TIMEOUT + 2000), duration < (TIMEOUT + 2000));
        assertEquals("quit", nextStep);
    }

    @Test
    public void testCheckPrerequisiteStepInvokeSuccess() {
        long time = System.currentTimeMillis();
        final DateTime imminent = new DateTime().plusMillis(TIMEOUT);
        String prereq = "{" +
                "    \"requiredData\": [" +
                "        {" +
                "            \"uri\": \"blob://foo/<DATE>/bar\"," +
                "            \"timeNoEarlierThan\": \"23:59:59.000 " + imminent.getZone() + "\"," +
                "            \"keyFormat\": \"yyyyMMdd\"," +
                "            \"dateWithin\": \"5D\"" +
                "        }" +
                "    ]," +
                "    \"retryInMillis\": " + TIMEOUT / 10 + "," +
                "    \"cutoffTime\": \"" + new SimpleDateFormat("HH:mm:ss.000").format(imminent.toDate()) + " " + imminent.getZone() + "\"," +
                "    \"cutoffAction\": \"QUIT\"" +
                "}";

        docapi.putDoc(context, prereqURI, prereq);

        String workOrderURI = "workorder://test/prereq/" + System.currentTimeMillis();
        dapi.setContextLiteral(context, workOrderURI, "prerequisiteConfigUri", prereqURI);

        Kernel.getLock().createLockManager(ContextFactory.getKernelUser(), "lock://kernel", "LOCKING USING DUMMY {}", "");

        final String uri = "blob://foo/" + new SimpleDateFormat("yyyyMMdd").format(imminent.minusDays(3).toDate()) + "/bar";
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    sleep(TIMEOUT / 3);
                } catch (InterruptedException e) {
                }
                bapi.putBlob(context, uri, "foo".getBytes(), "text/csv");
            }
        };
        thread.start();

        CheckPrerequisiteStep cps = new CheckPrerequisiteStep(workOrderURI, null);
        String nextStep = cps.invoke(context);
        long duration = System.currentTimeMillis() - time;
        System.out.println(nextStep + " " + duration);

        assertTrue(duration < TIMEOUT);
        assertEquals("next", nextStep);
        bapi.deleteBlob(context, uri);
    }

    @Test
    public void testCheckMultiplePrerequisiteSteps() {
        long time = System.currentTimeMillis();
        final DateTime imminent = new DateTime().plusMillis(TIMEOUT);

        final String montyUri = "series://monty/pythons/flying/circus"; // It was a very good series.

        String prereq = "{" +
                "    \"requiredData\": [" +
                "        {" +
                "            \"uri\": \"blob://foo/<DATE>/bar\"," +
                "            \"timeNoEarlierThan\": \"23:59:59.000 " + imminent.getZone() + "\"," +
                "            \"keyFormat\": \"yyyyMMdd\"," +
                "            \"dateWithin\": \"5D\"" +
                "        }," +
                "        {" +
                "            \"uri\": \"" + montyUri + "\"," +
                "            \"timeNoEarlierThan\": \"23:59:59.000 " + imminent.getZone() + "\"," +
                "            \"keyFormat\": \"yyyyMMdd\"," +
                "            \"dateWithin\": \"5D\"" +
                "        }," +
                "        {" +
                "            \"uri\": \"blob://foo/<DATE>/brian\"," +
                "            \"timeNoEarlierThan\": \"23:59:59.000 " + imminent.getZone() + "\"," +
                "            \"keyFormat\": \"yyyyMMdd\"," +
                "            \"dateWithin\": \"5D\"" +
                "        }" +
                "    ]," +
                "    \"retryInMillis\": " + TIMEOUT / 10 + "," +
                "    \"cutoffTime\": \"" + new SimpleDateFormat("HH:mm:ss.000").format(imminent.toDate()) + " " + imminent.getZone() + "\"," +
                "    \"cutoffAction\": \"QUIT\"" +
                "}";

        docapi.putDoc(context, prereqURI, prereq);

        String workOrderURI = "workorder://test/prereq/" + System.currentTimeMillis();
        dapi.setContextLiteral(context, workOrderURI, "prerequisiteConfigUri", prereqURI);

        Kernel.getLock().createLockManager(ContextFactory.getKernelUser(), "lock://kernel", "LOCKING USING DUMMY {}", "");

        final String dateStr = new SimpleDateFormat("yyyyMMdd").format(imminent.minusDays(3).toDate());
        final String barUri = "blob://foo/" + dateStr + "/bar";
        final String brianUri = "blob://foo/" + dateStr + "/brian";

        Thread thread1 = new Thread() {
            @Override
            public void run() {
                try {
                    sleep(TIMEOUT / 7);
                } catch (InterruptedException e) {
                }
                bapi.putBlob(context, barUri, "foo".getBytes(), "text/csv");
            }
        };
        thread1.start();

        Thread jeffThread = new Thread() {
            @Override
            public void run() {
                try {
                    sleep(TIMEOUT / 5);
                } catch (InterruptedException e) {
                }
                bapi.putBlob(context, brianUri, "jeff".getBytes(), "text/csv");
            }
        };
        jeffThread.start();

        Thread montyThread = new Thread() {
            @Override
            public void run() {
                try {
                    sleep(TIMEOUT / 2);
                } catch (InterruptedException e) {
                }
                serapi.addStringToSeries(context, montyUri, dateStr, dateStr);
                SeriesPoint lastDataPoint = Kernel.getSeries().getLastPoint(context, montyUri);

            }
        };
        montyThread.start();

        CheckPrerequisiteStep cps = new CheckPrerequisiteStep(workOrderURI, null);
        String nextStep = cps.invoke(context);
        long duration = System.currentTimeMillis() - time;
        System.out.println(nextStep + " " + duration);

        assertTrue(duration > (TIMEOUT / 2));
        assertTrue(duration < TIMEOUT);
        assertEquals("next", nextStep);
        bapi.deleteBlob(context, barUri);
        bapi.deleteBlob(context, brianUri);
    }

    @Test
    public void testCheckPrereqSpecificDateSuccess() {
        long time = System.currentTimeMillis();
        final DateTime imminent = new DateTime().plusMillis(TIMEOUT);
        String prereq = "{" +
                "    \"requiredData\": [" +
                "        {" +
                "            \"uri\": \"blob://foo/<DATE>/bar\"," +
                "            \"keyFormat\": \"yyyyMMdd\"," +
                "            \"specificDate\": \"$DATE\"" +
                "        }" +
                "    ]," +
                "    \"retryInMillis\": " + TIMEOUT / 10 + "," +
                "    \"cutoffTime\": \"" + new SimpleDateFormat("HH:mm:ss.000").format(imminent.toDate()) + " " + imminent.getZone() + "\"," +
                "    \"cutoffAction\": \"QUIT\"" +
                "}";

        docapi.putDoc(context, prereqURI, prereq);

        String workOrderURI = "workorder://test/prereq/" + System.currentTimeMillis();
        dapi.setContextLiteral(context, workOrderURI, "prerequisiteConfigUri", prereqURI);
        dapi.setContextLiteral(context, workOrderURI, "DATE", "20140102");

        Kernel.getLock().createLockManager(ContextFactory.getKernelUser(), "lock://kernel", "LOCKING USING DUMMY {}", "");

        Thread thread = new Thread() {
            @Override
            public void run() {
                bapi.putBlob(context, "blob://foo/20140101/bar", "foo".getBytes(), "text/csv");
                bapi.putBlob(context, "blob://foo/20140103/bar", "foo".getBytes(), "text/csv");
                try {
                    sleep(TIMEOUT / 3);
                } catch (InterruptedException e) {
                }
                bapi.putBlob(context, "blob://foo/20140102/bar", "foo".getBytes(), "text/csv");
                System.out.println("Created blob://foo/20140102/bar");
            }
        };
        thread.start();

        CheckPrerequisiteStep cps = new CheckPrerequisiteStep(workOrderURI, null);
        String nextStep = cps.invoke(context);
        long duration = System.currentTimeMillis() - time;
        System.out.println(nextStep + " " + duration);

        assertTrue(duration < TIMEOUT);
        assertEquals("next", nextStep);
        bapi.deleteBlob(context, "blob://foo/20140101/bar");
        bapi.deleteBlob(context, "blob://foo/20140102/bar");
        bapi.deleteBlob(context, "blob://foo/20140103/bar");
    }

    @Test
    public void testCheckPrereqSpecificDateFail() {
        long time = System.currentTimeMillis();
        final DateTime imminent = new DateTime().plusMillis(TIMEOUT);
        String prereq = "{" +
                "    \"requiredData\": [" +
                "        {" +
                "            \"uri\": \"blob://foo/<DATE>/bar\"," +
                "            \"keyFormat\": \"yyyyMMdd\"," +
                "            \"specificDate\": \"$DATE\"" +
                "        }" +
                "    ]," +
                "    \"retryInMillis\": " + TIMEOUT / 10 + "," +
                "    \"cutoffTime\": \"" + new SimpleDateFormat("HH:mm:ss.000").format(imminent.toDate()) + " " + imminent.getZone() + "\"," +
                "    \"cutoffAction\": \"QUIT\"" +
                "}";

        docapi.putDoc(context, prereqURI, prereq);

        String workOrderURI = "workorder://test/prereq/" + System.currentTimeMillis();
        dapi.setContextLiteral(context, workOrderURI, "prerequisiteConfigUri", prereqURI);
        dapi.setContextLiteral(context, workOrderURI, "DATE", "20140102");

        Kernel.getLock().createLockManager(ContextFactory.getKernelUser(), "lock://kernel", "LOCKING USING DUMMY {}", "");

        Thread thread = new Thread() {
            @Override
            public void run() {
                bapi.putBlob(context, "blob://foo/20140101/bar", "foo".getBytes(), "text/csv");
                bapi.putBlob(context, "blob://foo/20140103/bar", "foo".getBytes(), "text/csv");
                try {
                    sleep(TIMEOUT / 3);
                } catch (InterruptedException e) {
                }
            }
        };
        thread.start();

        CheckPrerequisiteStep cps = new CheckPrerequisiteStep(workOrderURI, null);
        String nextStep = cps.invoke(context);
        long duration = System.currentTimeMillis() - time;
        System.out.println(nextStep + " " + duration);

        assertTrue(duration < TIMEOUT);
        assertEquals("quit", nextStep);
        bapi.deleteBlob(context, "blob://foo/20140101/bar");
        bapi.deleteBlob(context, "blob://foo/20140103/bar");
    }

}
