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
package rapture.common.version;

/**
 * Note that this class is referenced in types.api - any changes to this file should be reflected there.
 **/

public class ApiVersion implements Comparable<ApiVersion> {
    private Integer minor = 0;
    private Integer major = 0;
    private Integer micro = 0;

    public ApiVersion() {
    }

    public ApiVersion(int minor, int major, int micro) {
        this.minor = minor;
        this.major = major;
        this.micro = micro;
    }

    public int getMinor() {
        return minor;
    }

    public void setMinor(int minor) {
        this.minor = minor;
    }

    public int getMajor() {
        return major;
    }

    public void setMajor(int major) {
        this.major = major;
    }

    public int getMicro() {
        return micro;
    }

    public void setMicro(int micro) {
        this.micro = micro;

    }

    @Override
    public String toString() {
        return major + "." + minor + "." + micro;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ApiVersion that = (ApiVersion) o;

        return minor == that.minor && major == that.major && micro == that.micro;
    }

    @Override
    public int hashCode() {
        int result = minor;
        result = 31 * result + major;
        result = 31 * result + micro;
        return result;
    }

    @Override
    public int compareTo(ApiVersion other) {
        int majorDiff = this.major.compareTo(other.major);
        if (majorDiff == 0) {
            int minorDiff = this.minor.compareTo(other.minor);
            if (minorDiff == 0) {
                return this.micro.compareTo(other.micro);
            } else {
                return minorDiff;
            }
        } else {
            return majorDiff;
        }
    }
}
