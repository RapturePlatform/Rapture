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
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.simple.JSONObject;

import rapture.common.CallingContext;
import rapture.common.RaptureFolderInfo;
import rapture.common.exception.RaptureException;
import rapture.kernel.Kernel;

public class ProcessFile extends AbstractStep {

    public ProcessFile(String workerUri, String stepName) {
        super(workerUri, stepName);
    }

    @Override
    public String invoke(CallingContext ctx) {
        final int BATCH_LOAD_SIZE = 25; // TODO: move to config
        OPCPackage pkg;
        XSSFWorkbook wb;
        FileInputStream fis = null;
        List<String> uris = new ArrayList<String>(); // lists for doc.putDocs()
        List<String> docs = new ArrayList<String>();
        HashMap<Object, Object> data = new HashMap<Object, Object>();
        JSONObject obj = new JSONObject();

        SimpleDateFormat ft = new SimpleDateFormat("yyyyMMdd_hhmmss");
        String repo = "document://data/" + ft.format(new Date());
        String docUri = repo + "#id";

        try {
            // TODO: move this to loadFile step and get from blob
            String file = Kernel.getDecision().getContextValue(ctx, getWorkerURI(), "filetoupload");
            fis = new FileInputStream(file);

            // pkg = OPCPackage.open(new File(file));
            pkg = OPCPackage.open(fis);
            wb = new XSSFWorkbook(pkg);
            XSSFSheet sheet = wb.getSheetAt(0);

            log.info("Loading " + sheet.getPhysicalNumberOfRows() + " rows from " + file + ". Batch size is " + BATCH_LOAD_SIZE);

            int physicalNumberOfRows = sheet.getPhysicalNumberOfRows();
            int remainder = physicalNumberOfRows % BATCH_LOAD_SIZE;
            int div = physicalNumberOfRows / BATCH_LOAD_SIZE;

            // preload the uri list as they are all end with "#id"
            for (int g = 1; g <= BATCH_LOAD_SIZE; g++) {
                uris.add(docUri);
            }
            log.info("created uris list " + uris.size());

            int j = 0;
            int count = 0;
            long start = System.currentTimeMillis();
            for (int i = 1; i <= div; i++) {
                for (j = count; j < (BATCH_LOAD_SIZE * i); j++) {
                    Row row = sheet.getRow(j);
                    data.put("Row", row.getRowNum());
                    data.put("DataPeriod", row.getCell(0).toString());
                    data.put("Industry", row.getCell(3).toString());
                    data.put("Price", row.getCell(7).toString());
                    obj.putAll(data);
                    docs.add(obj.toJSONString());
                    obj.clear();
                    data.clear();
                }
                Kernel.getDoc().putDocs(ctx, uris, docs);
                count = j;
                docs.clear();
            }
            // get the remaining rows
            if (remainder > 0) {
                for (int k = (count); k < (count + remainder); k++) {
                    Row row = sheet.getRow(k);
                    data.put("Row", row.getRowNum());
                    data.put("DataPeriod", row.getCell(0).toString());
                    data.put("Industry", row.getCell(3).toString());
                    data.put("Price", row.getCell(7).toString());
                    obj.putAll(data);
                    Kernel.getDoc().putDoc(ctx, docUri, obj.toJSONString());
                }
            }

            long end = System.currentTimeMillis();
            log.info("Populated uri " + repo + ". Took " + (end - start) + "ms.");
            pkg.close();

            Map<String, RaptureFolderInfo> listDocsByUriPrefix = Kernel.getDoc().listDocsByUriPrefix(ctx, repo, 1);
            log.info("Count from repo is " + listDocsByUriPrefix.size());

            if (listDocsByUriPrefix.size() == sheet.getPhysicalNumberOfRows()) {
                return "ok";
            } else {
                return "error"; // TODO: add error step
            }
        } catch (InvalidFormatException | IOException | RaptureException e) {
            log.error(e.getMessage() + " " + e.getStackTrace());
            return "error";
        }
    }
}
