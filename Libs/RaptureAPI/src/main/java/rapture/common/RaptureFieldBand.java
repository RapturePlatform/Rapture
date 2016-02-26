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

/**
 * One band when a field is defined by its bands
 * 
 * @author alan
 * 
 */
public class RaptureFieldBand implements RaptureTransferObject {
    private String bandName; // The return value of this band is called this

    private Double lowCheck;

    private Double highCheck;

    private Boolean hasLow = false;

    private Boolean hasHigh = false;

    public String getBandName() {
        return bandName;
    }

    public Boolean getHasHigh() {
        return hasHigh;
    }

    public Boolean getHasLow() {
        return hasLow;
    }

    public Double getHighCheck() {
        return highCheck;
    }

    public Double getLowCheck() {
        return lowCheck;
    }

    public void setBandName(String bandName) {
        this.bandName = bandName;
    }

    public void setHasHigh(Boolean hasHigh) {
        this.hasHigh = hasHigh;
    }

    public void setHasLow(Boolean hasLow) {
        this.hasLow = hasLow;
    }

    public void setHighCheck(Double highCheck) {
        this.highCheck = highCheck;
        setHasLow(!highCheck.isNaN());
    }

    public void setLowCheck(Double lowCheck) {
        this.lowCheck = lowCheck;
        setHasLow(!lowCheck.isNaN());
    }

    public boolean valueSatisfies(double value) {
        if (hasLow && value < lowCheck) {
            return false;
        }
        if (hasHigh && value > highCheck) {
            return false;
        }
        if (hasLow && value > lowCheck) {
            if (hasHigh) {
                if (value < highCheck) {
                    return true;
                }
            } else {
                return true;
            }
        }
        if (hasHigh && value < highCheck) {
            if (hasLow) {
                if (value > lowCheck) {
                    return true;
                }
            } else {
                return true;
            }
        }
        return false;
    }

}
