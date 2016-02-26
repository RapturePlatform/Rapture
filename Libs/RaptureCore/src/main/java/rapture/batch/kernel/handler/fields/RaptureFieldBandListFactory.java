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
package rapture.batch.kernel.handler.fields;

import java.util.ArrayList;
import java.util.List;

import rapture.common.RaptureFieldBand;

public final class RaptureFieldBandListFactory {

    /**
     * Create a series of bands based on a string config
     * 
     * Config is name,low,high. E.g. short,,0.6;long,0.6,
     * 
     * If not present, the band captures that (i.e. it is the upper or lower
     * band)
     * 
     * @param string
     * @return
     */
    public static List<RaptureFieldBand> createFrom(String config) {
        List<RaptureFieldBand> ret = new ArrayList<RaptureFieldBand>();

        String[] bands = config.split(";");
        for (String bandConfig : bands) {
            String[] bandParts = bandConfig.split(",");
            RaptureFieldBand band = new RaptureFieldBand();
            band.setBandName(bandParts[0]);
            if (bandParts.length > 1) {
                band.setLowCheck(Double.valueOf(bandParts[1]));
            }
            if (bandParts.length > 2) {
                band.setHighCheck(Double.valueOf(bandParts[2]));
            }
            ret.add(band);
        }
        return ret;
    }

    private RaptureFieldBandListFactory() {

    }

}
