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

import java.util.List;

public class PrerequisiteConfig {
    private List<RequiredData> requiredData;
    private int retryInMillis;              // retry interval in milliseconds
    private String cutoffTime;              // hh:mm:ss timezone, eg, 18:00:00 America/New_York
    private CutoffAction cutoffAction;

    public List<RequiredData> getRequiredData() {
        return requiredData;
    }

    public void setRequiredData(List<RequiredData> requiredData) {
        this.requiredData = requiredData;
    }

    public int getRetryInMillis() {
        return retryInMillis;
    }

    public void setRetryInMillis(int retryInMillis) {
        this.retryInMillis = retryInMillis;
    }    

    public String getCutoffTime() {
        return cutoffTime;
    }

    public void setCutoffTime(String cutoffTime) {
        this.cutoffTime = cutoffTime;
    }

    public CutoffAction getCutoffAction() {
        return cutoffAction;
    }

    public void setCutoffAction(CutoffAction cutoffAction) {
        this.cutoffAction = cutoffAction;
    }

    // public classes

    public static enum CutoffAction {
        START,
        QUIT;
    }

    public static class RequiredData {

        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "RequiredData [uri=" + uri + ", keyFormat=" + keyFormat + ", dateWithin=" + dateWithin + ", specificDate=" + specificDate
                    + ", timeNoEarlierThan=" + timeNoEarlierThan + "]";
        }

        // data uri
        // eg. "series://datacapture/HIST/MKIT/PMI_BR_MAN_OP"
        // eg. "blob://datacapture/foo/<DATE>/blob"
        private String uri;

        // series data key format, defaults to "yyyyMMdd"
        // eg. "yyyy/MM/dd", or "yyyy-MM-dd"
        private String keyFormat;

        // defaults to today if not specified. Format should be "#unit". Unit can be month, week, day, or business_day.
        // eg. "1D" (1 calendar day) "1B" (1 business day) "1W" (1 week) "1M" (1 month)
        private String dateWithin;

        // If you need to get data for a specific date range (useful for testing) - see RFS-1213
        private String specificDate;

        // defaults to start of day if not specified. Format should be "hh:mm:ss timezone"
        // eg. "15:00:00 America/New_York"
        private String timeNoEarlierThan;

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public String getKeyFormat() {
            return keyFormat;
        }

        public void setKeyFormat(String keyFormat) {
            this.keyFormat = keyFormat;
        }

        public String getDateWithin() {
            return dateWithin;
        }

        public void setDateWithin(String dateWithin) {
            this.dateWithin = dateWithin;
        }

        public String getSpecificDate() {
            return specificDate;
        }

        public void setSpecificDate(String specificDate) {
            this.specificDate = specificDate;
        }

        public String getTimeNoEarlierThan() {
            return timeNoEarlierThan;
        }

        public void setTimeNoEarlierThan(String timeNoEarlierThan) {
            this.timeNoEarlierThan = timeNoEarlierThan;
        }
    }
}
