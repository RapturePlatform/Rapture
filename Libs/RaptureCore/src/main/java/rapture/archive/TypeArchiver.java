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
package rapture.archive;

import org.apache.log4j.Logger;

import rapture.common.TypeArchiveConfig;
import rapture.common.storable.helpers.TypeArchiveConfigHelper;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

/**
 * This class is used to archive a type. The config may be stored in the
 * settings repo through an admin api call but such a config may
 * not exist and therefore some defaults are needed.
 * 
 * Archive settings have two components - the maximum number of versions to keep
 * and the amount of time to keep all versions for.
 * 
 * So for instance if "all versions" was 1 month and
 * "max versions to keep was 4" then we could actually retain more than 4
 * versions if there was a flurry of activity in the last month.
 * 
 * The config really defines constraints. - do we want to use a Reflex
 * script for this as an alternate? - it gets given an array of maps, with each
 * map containing version number and a date It is up to the script to return the
 * version numbers to *remove*.
 * 
 * @author amkimian
 * 
 */
public class TypeArchiver {
    private static Logger log = Logger.getLogger(TypeArchiver.class);
    private String authority;
    private String type;
    private TypeArchiveConfig config;

    public TypeArchiver(String authority, String type) {
        this.authority = authority;
        this.type = type;
        config = Kernel.getAdmin().getArchiveConfig(ContextFactory.getKernelUser(), "//" + authority + "/" + type);
        if (config == null) {
            config = TypeArchiveConfigHelper.getDefault();
        }
    }

    /**
     * Archive this type.
     * 
     * @param targetFolder
     *            - the root folder to put archived documents
     * @param testMode
     *            - whether to look and log but don't change anything
     * @param noArchive
     *            - whether to actually archive (store) or just delete
     */
    public void archive(String targetFolder, boolean testMode, boolean noArchive) {
        log.info(String.format("Archiving for %s in authority %s", type, authority));
    }
}
