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
package rapture.dp.invocable.workflow;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.google.common.collect.ImmutableMap;

import rapture.common.CallingContext;
import rapture.common.RaptureFolderInfo;
import rapture.common.exception.RaptureException;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.util.InsertData;
import rapture.kernel.Kernel;

public class ProcessFile extends AbstractStep {

    public ProcessFile(String workerUri, String stepName) {
        super(workerUri, stepName);
    }

    @SuppressWarnings({ "rawtypes", "unchecked", "resource" })
    @Override
    public String invoke(CallingContext ctx) {
        final int BATCH_LOAD_SIZE = 200; // TODO: move to config
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        OPCPackage pkg;
        XSSFWorkbook wb;
        FileInputStream fis;
        List uris = new ArrayList<>();
        // stores all documents for insertion
        List<List<String>> allDocs = new ArrayList<List<String>>();

        String repo = "document://data/" + LocalDateTime.now().format(formatter);
        String docUri = repo + "#id";

        // TODO: move this to loadFile step and get from blob
        String file = Kernel.getDecision().getContextValue(ctx, getWorkerURI(), "filetoupload");
        try {
            fis = new FileInputStream(file);
        } catch (FileNotFoundException fnf) {
            log.error(fnf.getMessage() + " " + fnf.getStackTrace());
            return "error";
        }

        try {
            pkg = OPCPackage.open(fis);
            wb = new XSSFWorkbook(pkg);
            XSSFSheet sheet = wb.getSheetAt(0);

            log.info("Loading " + sheet.getPhysicalNumberOfRows() + " rows from " + file + ". Batch size is " + BATCH_LOAD_SIZE);

            int physicalNumberOfRows = sheet.getPhysicalNumberOfRows();
            int remainder = physicalNumberOfRows % BATCH_LOAD_SIZE;
            int div = physicalNumberOfRows / BATCH_LOAD_SIZE;

            // this only needs to be done once as the uris dont change
            for (int g = 1; g <= BATCH_LOAD_SIZE; g++) {
                uris.add(docUri);
            }
            log.info("created uris list " + uris.size());

            int j = 0;
            int count = 0;
            long startLoadTime = System.currentTimeMillis();
            for (int i = 1; i <= div; i++) {
                List docs = new ArrayList<>();
                // Create a list of documents with size of BATCH_LOAD_SIZE
                for (j = count; j < (BATCH_LOAD_SIZE * i); j++) {
                    Row row = sheet.getRow(j);
                    Map<String, Object> map = ImmutableMap.of("Row", row.getRowNum(), "DataPeriod", row.getCell(0).toString(), "Industry",
                            row.getCell(3).toString(), "Price", row.getCell(7).toString());
                    docs.add(JacksonUtil.jsonFromObject(map));
                }
                allDocs.add(docs);
                count = j;
            }
            long endLoadTime = System.currentTimeMillis();

            ExecutorService executorService = Executors.newCachedThreadPool();
            long startWriteTime = System.currentTimeMillis();
            for (List<String> docList : allDocs) {
                executorService.execute(new InsertData(ctx, docList, uris));
            }
            executorService.shutdown();

            try {
                // TODO: hardcoded timeout.ComparableFutures?
                // Helpful:
                // http://stackoverflow.com/questions/1250643/how-to-wait-for-all-threads-to-finish-using-executorservice
                executorService.awaitTermination(60000L, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                log.error(e.getStackTrace().toString(), e);
                return "error";
            }
            long endWriteTime = System.currentTimeMillis();
            log.info("Completed parallel load.");

            // handle the remaining rows
            if (remainder > 0) {
                long remStartTime = System.currentTimeMillis();
                for (int k = (count); k < (count + remainder); k++) {
                    Row row = sheet.getRow(k);
                    Map<String, Object> map = ImmutableMap.of("Row", row.getRowNum(), "DataPeriod", row.getCell(0).toString(), "Industry",
                            row.getCell(3).toString(), "Price", row.getCell(7).toString());
                    Kernel.getDoc().putDoc(ctx, docUri, JacksonUtil.jsonFromObject(map));
                }
                long remEndTime = System.currentTimeMillis();
                log.info("Remainders took " + (remEndTime - remStartTime) + "ms");
            }

            log.info("Populated uri " + repo + ". Took " + (endLoadTime - startLoadTime) + "ms. to load data. Took " + (endWriteTime - startWriteTime)
                    + "ms. to write data.");
            pkg.close();

            Map<String, RaptureFolderInfo> listDocsByUriPrefix = Kernel.getDoc().listDocsByUriPrefix(ctx, repo, 1);
            log.info("Count from repo is " + listDocsByUriPrefix.size());

            if (listDocsByUriPrefix.size() == sheet.getPhysicalNumberOfRows()) {
                return "ok";
            } else {
                return "error"; // TODO: add error step
            }
        } catch (InvalidFormatException | IOException |

                RaptureException e) {
            log.error(e.getStackTrace().toString() + e.getMessage().toString() + e.toString());
            return "error";
        }
    }
}
