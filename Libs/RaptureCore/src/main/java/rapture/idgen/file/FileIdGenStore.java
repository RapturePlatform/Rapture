/**
 * The MIT License (MIT)
 *
 * Copyright (C) 2011-2016 Incapture Technologies LLC
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
package rapture.idgen.file;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import rapture.common.exception.RaptureExceptionFactory;
import rapture.dsl.idgen.IdGenStore;
import rapture.kernel.file.FileRepoUtils;

public class FileIdGenStore implements IdGenStore {

    private File parentDir;
    private String instanceName;
    
    @Override
    public Long getNextIdGen(Long interval) {
        File file = FileRepoUtils.makeGenericFile(parentDir, instanceName);
        try {
            if (!file.exists()) throw RaptureExceptionFactory.create("idgen has been deleted");
            String value = FileUtils.readFileToString(file);
            Long number = Long.parseLong(value);
            number += interval;
            FileUtils.writeStringToFile(file, "" + number);
            return number;
        } catch (NumberFormatException | IOException e) {
            throw RaptureExceptionFactory.create("Cannot read idgen storage " + file.getName(), e);
        }
    }

    @Override
    public void resetIdGen(Long number) {
        File file = FileRepoUtils.makeGenericFile(parentDir, instanceName);
        try {
            FileUtils.writeStringToFile(file, "" + number);
        } catch (NumberFormatException | IOException e) {
            throw RaptureExceptionFactory.create("Cannot update idgen storage " + file.getName(), e);
        }
    }

    @Override
    public void setConfig(Map<String, String> config) {
        // What happens if this is called twice?
        if (parentDir != null) throw RaptureExceptionFactory.create("Calling setConfig twice is currently not supported");

        String prefix = config.get(FileRepoUtils.PREFIX);
        if (StringUtils.trimToNull(prefix) == null) throw RaptureExceptionFactory.create("prefix must be specified");
        parentDir = FileRepoUtils.ensureDirectory(prefix + "_idgen");

        init();
    }

    @Override
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    @Override
    public void invalidate() {
        FileRepoUtils.makeGenericFile(parentDir, instanceName).delete();
    }

    @Override
    public void makeValid() {
        // do nothing
    }

    @Override
    public void init() {
        resetIdGen(0L);
    }
}
