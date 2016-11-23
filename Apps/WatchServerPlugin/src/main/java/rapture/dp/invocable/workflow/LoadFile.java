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

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.commons.io.FileUtils;

import com.google.common.net.MediaType;

import rapture.common.CallingContext;
import rapture.dp.AbstractStep;
import rapture.kernel.Kernel;

public class LoadFile extends AbstractStep {

    public LoadFile(String workerUri, String stepName) {
        super(workerUri, stepName);
    }

    @Override
    public String invoke(CallingContext ctx) {
        String archiveUriPrefix = "blob://archive/";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String dateTime = LocalDateTime.now().format(formatter);
        // create a unique uri path to store file in and for other steps use
        Kernel.getDecision().setContextLiteral(ctx, getWorkerURI(), "folderName", dateTime);
        // get the context variable passed into workflow
        String absFilePath = Kernel.getDecision().getContextValue(ctx, getWorkerURI(), "filetoupload");
        log.info("Loading File:" + absFilePath);

        try {
            File f = new File(absFilePath);
            byte[] data = FileUtils.readFileToByteArray(f);
            String uri = archiveUriPrefix + dateTime + "/" + f.getName();
            Kernel.getBlob().putBlob(ctx, uri, data, MediaType.MICROSOFT_EXCEL.toString());
            if (Kernel.getBlob().getBlob(ctx, uri) != null) {
                log.info("File written to " + uri + " with size " + Kernel.getBlob().getBlobSize(ctx, uri));
                Kernel.getDecision().setContextLiteral(ctx, getWorkerURI(), "blobUri", uri);
            } else {
                log.error("Problem writing file to " + uri);
                return "error";
            }
        } catch (IOException e) {
            log.error("Exception " + e.getMessage(), e);
            return "error";
        }

        return "ok";
    }
}
