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
package rapture.common;

/**
 * Note that this class is referenced in types.api - any changes to this file should be reflected there.
**/

public class PluginVersion {
    private int major;

    private int minor;

    private int release;
    
    private long timestamp;

    public PluginVersion() {
        this(0, 0, 0, 0);
    }
    public PluginVersion(int major, int minor, int release, long timestamp) {
        this.major = major;
        this.minor = minor;
        this.release = release;
        this.timestamp = timestamp;
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getRelease() {
        return release;
    }
    
    public long getTimestamp() {
        return timestamp;
    }

    public void setMajor(int major) {
        this.major = major;
    }

    public void setMinor(int minor) {
        this.minor = minor;
    }

    public void setRelease(int release) {
        this.release = release;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean sameAs(PluginVersion version) {
        return version.major == major && version.minor == minor && version.release == release && version.timestamp == timestamp;
    }

    public static PluginVersion makeVersion(int major, int minor, int release, long timestamp) {
        PluginVersion version = new PluginVersion();
        version.major = major;
        version.minor = minor;
        version.release = release;
        version.timestamp = timestamp;
        return version;
    }

    public boolean earlierThan(PluginVersion version) {
        if (major < version.major) {
            return true;
        } else if (major > version.major) {
            return false;
        }
        if (minor < version.minor) {
            return true;
        } else if (minor > version.minor) {
            return false;
        }
        if (release < version.release) {
            return true;
        } else if (release > version.release) {
            return false;
        }
        if (timestamp < version.timestamp) {
            return true;
        } else if (timestamp > version.timestamp) {
            return false;
        }
        // Same = no
        return false;
    }
    
    public String toString() {
        return "" + major + "." + minor + "." + release + "." + timestamp;
    }
}
