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
package rapture.repo.postgres;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import rapture.kernel.TransactionManager;
import rapture.repo.StructuredRepo;
import rapture.structured.StructuredFactory;
import rapture.util.IDGenerator;

/**
 * Created by yanwang on 4/15/15.
 */
public class PostgresTransactionTest {
    private static final String CONFIG = "STRUCTURED {} USING POSTGRES{}";
    private static final String AUTHORITY = "some.repo";
    private static final String TABLE_NAME = "some.table";
    private static final Map<String, String> COLUMNS = ImmutableMap.of("col1", "int", "col2", "varchar(30)");
    private static int TOTAL_ROWS = 10;

    private PostgresStructuredStore store;
    private StructuredRepo repo;
    private String txId;

    @Before
    public void setup() {
        store = (PostgresStructuredStore) StructuredFactory.getRepo(CONFIG, AUTHORITY);
        repo = new StructuredRepo(store);
        repo.createTable(TABLE_NAME, COLUMNS);
        txId = IDGenerator.getUUID();
    }

    @After
    public void tearDown() {
        store.dropTable(TABLE_NAME);
        store.drop();
    }

    private abstract class TransactionRunnable implements Runnable {
        protected abstract void doRun();

        @Override
        public void run() {
            TransactionManager.registerThread(txId);
            TransactionManager.registerRepo(txId, repo);
            doRun();
        }
    }

    @Test
    public void testCommitMultiStores() {
        final PostgresStructuredStore store2 = (PostgresStructuredStore) StructuredFactory.getRepo(CONFIG, "other.repo");

        // start transaction in main thread
        TransactionManager.begin(txId);
        // modify store in 2nd thread
        runInThread(new TransactionRunnable() {
            @Override
            protected void doRun() {
                store.dropTable(TABLE_NAME);
                assertFalse(store.tableExists(TABLE_NAME));
            }
        });
        assertTrue(store.tableExists(TABLE_NAME));
        // modify store2 in 3rd thread
        runInThread(new TransactionRunnable() {
            @Override
            protected void doRun() {
                store2.createTable(TABLE_NAME, COLUMNS);
                assertTrue(store2.tableExists(TABLE_NAME));
            }
        });
        assertFalse(store2.tableExists(TABLE_NAME));
        // commit transaction in 4th thread
        commitInThread(-1);
        assertFalse(store.tableExists(TABLE_NAME));
        assertTrue(store2.tableExists(TABLE_NAME));

        store2.drop();
    }

    @Test
    public void testRollbackMultiStores() {
        final PostgresStructuredStore store2 = (PostgresStructuredStore) StructuredFactory.getRepo(CONFIG, "other.repo");

        // start transaction in main thread
        TransactionManager.begin(txId);
        // modify store in 2nd thread
        runInThread(new TransactionRunnable() {
            @Override
            protected void doRun() {
                store.dropTable(TABLE_NAME);
                assertFalse(store.tableExists(TABLE_NAME));
            }
        });
        assertTrue(store.tableExists(TABLE_NAME));
        // modify store2 in 3rd thread
        runInThread(new TransactionRunnable() {
            @Override
            protected void doRun() {
                store2.createTable(TABLE_NAME, COLUMNS);
                assertTrue(store2.tableExists(TABLE_NAME));
            }
        });
        assertFalse(store2.tableExists(TABLE_NAME));
        // rollback transaction in 4th thread
        rollbackInThread(-1);
        assertTrue(store.tableExists(TABLE_NAME));
        assertFalse(store2.tableExists(TABLE_NAME));

        store2.drop();
    }

    @Test
    public void testCommitCreateTable() {
        final String newTableName = "newTableName";
        assertFalse(store.tableExists(newTableName));

        // start transaction in main thread
        TransactionManager.begin(txId);
        // create table in a new thread
        runInThread(new TransactionRunnable() {
            @Override
            protected void doRun() {
                store.createTable(newTableName, COLUMNS);
                assertTrue(store.tableExists(newTableName));
            }
        });
        assertFalse(store.tableExists(newTableName));

        // commit transaction from a third thread
        commitInThread(-1);
        assertTrue(store.tableExists(newTableName));
    }

    @Test
    public void testRollbackCreateTable() {
        final String newTableName = "newTableName";
        assertFalse(store.tableExists(newTableName));

        // start transaction in main thread
        TransactionManager.begin(txId);
        // create table in a new thread
        runInThread(new TransactionRunnable() {
            @Override
            protected void doRun() {
                store.createTable(newTableName, COLUMNS);
                assertTrue(store.tableExists(newTableName));
            }
        });
        assertFalse(store.tableExists(newTableName));

        // rollback transaction from a third thread
        rollbackInThread(-1);
        assertFalse(store.tableExists(newTableName));
    }

    @Test
    public void testCommitAddTableColumns() {
        final String newColumnName = "col3";
        final String newColumnType = "int";
        assertFalse(store.describeTable(TABLE_NAME).getRows().containsKey(newColumnName));

        // start transaction in main thread
        TransactionManager.begin(txId);
        // create table in a new thread
        runInThread(new TransactionRunnable() {
            @Override
            protected void doRun() {
                store.addTableColumns(TABLE_NAME, ImmutableMap.of(newColumnName, newColumnType));
                assertTrue(store.describeTable(TABLE_NAME).getRows().containsKey(newColumnName));
            }
        });
        assertFalse(store.describeTable(TABLE_NAME).getRows().containsKey(newColumnName));

        // commit transaction from a third thread
        commitInThread(-1);
        assertTrue(store.describeTable(TABLE_NAME).getRows().containsKey(newColumnName));
    }

    @Test
    public void testRollbackAddTableColumns() {
        final String newColumnName = "col3";
        final String newColumnType = "int";
        assertFalse(store.describeTable(TABLE_NAME).getRows().containsKey(newColumnName));

        // start transaction in main thread
        TransactionManager.begin(txId);
        // create table in a new thread
        runInThread(new TransactionRunnable() {
            @Override
            protected void doRun() {
                store.addTableColumns(TABLE_NAME, ImmutableMap.of(newColumnName, newColumnType));
                assertTrue(store.describeTable(TABLE_NAME).getRows().containsKey(newColumnName));
            }
        });
        assertFalse(store.describeTable(TABLE_NAME).getRows().containsKey(newColumnName));

        // rollback transaction from a third thread
        rollbackInThread(-1);
        assertFalse(store.describeTable(TABLE_NAME).getRows().containsKey(newColumnName));
    }

    @Test
    public void testCommitInsertRow() {
        checkContentSize(0);

        // start transaction from main thread
        TransactionManager.begin(txId);
        // add content from a new thread
        runInThread(new TransactionRunnable() {
            @Override
            protected void doRun() {
                addContent(TOTAL_ROWS);
                checkContentSize(TOTAL_ROWS);
            }
        });
        checkContentSize(0);

        // commit transaction from a third thread
        commitInThread(TOTAL_ROWS);
        checkContentSize(TOTAL_ROWS);
    }

    @Test
    public void testRollbackInsertRow() {
        checkContentSize(0);

        // start transaction from main thread
        TransactionManager.begin(txId);
        // add content from a new thread
        runInThread(new TransactionRunnable() {
            @Override
            protected void doRun() {
                addContent(TOTAL_ROWS);
                checkContentSize(TOTAL_ROWS);
            }
        });
        checkContentSize(0);

        // rollback transaction from a third thread
        rollbackInThread(0);
        checkContentSize(0);
    }

    @Test
    public void testCommitDeleteRows() {
        addContent(TOTAL_ROWS);
        checkContentSize(TOTAL_ROWS);

        // start transaction from main thread
        TransactionManager.begin(txId);
        // delete content in new thread
        runInThread(new TransactionRunnable() {
            @Override
            protected void doRun() {
                deleteHalfContent();
                checkContentSize(TOTAL_ROWS/2);
            }
        });
        checkContentSize(TOTAL_ROWS);

        // commit transaction from a third thread
        commitInThread(TOTAL_ROWS / 2);
        checkContentSize(TOTAL_ROWS/2);
    }

    @Test
    public void testRollBackDeleteRows() {
        addContent(TOTAL_ROWS);
        checkContentSize(TOTAL_ROWS);

        // start transaction from main thread
        TransactionManager.begin(txId);

        // delete content in new thread
        runInThread(new TransactionRunnable() {
            @Override
            protected void doRun() {
                deleteHalfContent();
                checkContentSize(TOTAL_ROWS / 2);
            }
        });
        checkContentSize(TOTAL_ROWS);

        // rollback transaction from a third thread
        rollbackInThread(TOTAL_ROWS);
        checkContentSize(TOTAL_ROWS);
    }

    private void addContent(int total) {
        for(int i = 1; i <= total; i++) {
            store.insertRow(TABLE_NAME, ImmutableMap.of("col1", i, "col2", "value " + i));
        }
    }

    private void deleteHalfContent() {
        store.deleteRows(TABLE_NAME, "col1>" + TOTAL_ROWS / 2);
    }

    private void checkContentSize(int expectedSize) {
        assertEquals(expectedSize, store.selectRows(TABLE_NAME, null, null, null, true, -1).size());
    }

    private void runInThread(Runnable runnable)  {
        try {
            Thread thread = new Thread(runnable);
            thread.start();
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void commitInThread(final int expectedSize) {
        runInThread(new Runnable() {
            @Override
            public void run() {
                TransactionManager.commit(txId);
                if(expectedSize != -1) {
                    checkContentSize(expectedSize);
                }

            }
        });
    }

    private void rollbackInThread(final int expectedSize) {
        runInThread(new Runnable() {
            @Override
            public void run() {
                TransactionManager.rollback(txId);
                if(expectedSize != -1) {
                    checkContentSize(expectedSize);
                }
            }
        });
    }
}
