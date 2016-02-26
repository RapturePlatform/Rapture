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
package rapture.common;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Simple class to display memory usage
 * 
 * @author <a href="mailto:dave.tong@incapturetechnologies.com">David Tong</a>
 * 
 */
public class MemUse {

    MemoryUsage heap = null;
    MemoryUsage nonheap = null;
    String marker;
    Date date;
    SimpleDateFormat simple = new SimpleDateFormat("M/dd h:mm:ss");
    boolean verbose = false;

    /**
     * Store current memory use
     * 
     * @param marker
     *            A name to identify this particular snapshot
     */
    public MemUse(String marker) {
        heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        nonheap = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();
        this.marker = marker;
        date = new Date();
    }

    /**
     * Format this object
     * 
     * @return
     */
    public String getMemUse() {
        // Request garbage collection first
        System.gc();
        return getMemUse(null);
    }

    /**
     * Format this object and compare current use to a previous snapshot
     * 
     * @param last
     *            Previous snapshot to compare to
     * @return
     */
    public String getMemUse(MemUse last) {
        StringBuilder sb = new StringBuilder();

        if (last != null) sb.append(last.marker).append(": ").append(simple.format(last.date)).append(" - ");

        sb.append(marker).append(": ");
        sb.append(simple.format(date)).append("\n");
        sb.append("Heap: ").append(heap.toString()).append("\n");
        sb.append("NonHeap: ").append(nonheap.toString()).append("\n");

        if (verbose) {
            List<MemoryPoolMXBean> beans = ManagementFactory.getMemoryPoolMXBeans();
            for (MemoryPoolMXBean bean : beans) {
                sb.append(bean.getName()).append(": ").append(bean.getUsage()).append("\n");
            }

            for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
                sb.append(bean.getName()).append(": ").append(bean.getCollectionCount()).append(": ").append(bean.getCollectionTime()).append("\n");
            }
        }

        if (last != null) {
            long heapUse = heap.getUsed() - last.heap.getUsed();
            long nonHeapUse = nonheap.getUsed() - last.nonheap.getUsed();
            sb.append("total heap used: ").append(toReadable(heapUse)).append("\n");
            sb.append("total non-heap used: ").append(toReadable(nonHeapUse)).append("\n");
        }
        return sb.toString();
    }

    /**
     * Control how much detail is displayed.
     * 
     * @param verbose
     *            True for more detail, false for less (default).
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    private static final String[] suffix = { "bytes", "kb", "Mb", "Gb", "Tb", "Pb", "Eb" };
    private static final double KILOBYTE = 1024.0;

    /**
     * Helper function to convert bytes to Kilo/Meg/Gig
     * 
     * @param bytes
     * @return
     */
    public static final String toReadable(double bytes) {
        int suff = 0;
        while (bytes > KILOBYTE) {
            bytes = bytes / KILOBYTE;
            suff++;
        }
        return ("" + bytes + "   ").substring(0, 3) + suffix[suff];
    }

}
