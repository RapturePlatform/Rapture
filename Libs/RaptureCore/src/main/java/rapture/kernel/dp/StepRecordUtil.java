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
package rapture.kernel.dp;

import rapture.common.CallingContext;
import rapture.common.dp.StepRecord;
import rapture.common.dp.StepRecordsWrapper;
import rapture.common.dp.StepRecordsWrapperStorage;
import rapture.common.dp.Worker;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import com.google.common.base.Optional;

/**
 * @author bardhi
 * @since 3/12/15.
 */
public class StepRecordUtil {

    /**
     * Get the step records for a worker
     *
     * @param worker
     * @return
     */
    public static List<StepRecord> getStepRecords(Worker worker) {
        StepRecordsWrapper stepsWrapper = StepRecordsWrapperStorage.readByFields(worker.getWorkOrderURI(), worker.getId());
        List<StepRecord> oldList = worker.getStepRecords(); //for backwards compatibility with Rapture pre 1.2.1. we should remove soon

        if (stepsWrapper == null) {
            return oldList;
        } else if (oldList.size() == 0) {
            return stepsWrapper.getStepRecords();
        } else { //stepsWrapper != null && oldList.size() > 0
            List<StepRecord> list = new LinkedList<>();
            list.addAll(oldList);
            list.addAll(stepsWrapper.getStepRecords());
            return list;
        }
    }

    /**
     * Store a step record for a worker. If this step record already exists for the worker, then it will get updated/overwritten. If it does not exist, a new
     * one is created. StepRecord equality is computed by checking if the stepURI and startTime fields in the StepRecord are the same
     * <p/>
     * This is NOT thread safe. However it should not matter, as a worker is by definition sequential, so this should never get called in parallel
     *
     * @param context
     * @param workOrderURI
     * @param workerId
     * @param stepRecord
     * @param comment
     */
    public static void writeStepRecord(CallingContext context, String workOrderURI, String workerId, StepRecord stepRecord, String comment) {
        StepRecordsWrapper stepsWrapper = StepRecordsWrapperStorage.readByFields(workOrderURI, workerId);
        if (stepsWrapper == null) {
            stepsWrapper = new StepRecordsWrapper();
            stepsWrapper.setWorkOrderURI(workOrderURI);
            stepsWrapper.setWorkerId(workerId);
        } else {
            List<StepRecord> existingList = stepsWrapper.getStepRecords();
            for (Iterator<StepRecord> iterator = existingList.iterator(); iterator.hasNext(); ) {
                StepRecord existing = iterator.next();
                if (areRecordsSame(existing, stepRecord)) {
                    iterator.remove();
                }
            }
        }
        stepsWrapper.getStepRecords().add(stepRecord);
        StepRecordsWrapperStorage.add(stepsWrapper, context.getUser(), comment);
    }

    public static boolean areRecordsSame(StepRecord r1, StepRecord r2) {
        // a Worker is sequential so only one step can start at a given time, so step start time is sufficient to determine equality
        return recordInitialized(r1) && recordInitialized(r2) && r1.getStartTime().equals(r2.getStartTime());
    }

    private static boolean recordInitialized(StepRecord record) {
        return record != null && record.getStartTime() != null;
    }

    public static Optional<StepRecord> getRecord(String workOrderURI, String workerId, Long stepStartTime) {
        StepRecordsWrapper stepsWrapper = StepRecordsWrapperStorage.readByFields(workOrderURI, workerId);
        if (stepsWrapper == null) {
            return Optional.absent();
        } else {
            for (StepRecord stepRecord : stepsWrapper.getStepRecords()) {
                if (stepRecord.getStartTime() != null && Objects.equals(stepRecord.getStartTime(), stepStartTime)) {
                    return Optional.of(stepRecord);
                }
            }
            return Optional.absent();
        }
    }
}
