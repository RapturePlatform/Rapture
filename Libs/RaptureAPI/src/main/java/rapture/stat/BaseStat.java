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
package rapture.stat;

import java.util.Date;

/**
 * A base stat has a date - when it was entered or when it is recorded
 * 
 * @author amkimian
 * 
 */
public abstract class BaseStat {
    private String key;
    private Date when = new Date();

    /**
     * Has this expired - which we define as being when + ageInMinutes < now
     * 
     * @param ageInMinutes
     * @return
     */
    public boolean expiredInMinutes(long ageInMinutes) {
        return expiredInSeconds(ageInMinutes * 60);
    }

    public boolean expiredInSeconds(long expireSeconds) {
        Date now = new Date();
        long epoch = when.getTime() + expireSeconds * 1000;
        if (epoch < now.getTime()) {
            return true;
        }
        return false;
    }

    public String getKey() {
        return key;
    }

    public Date getWhen() {
        return when;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setWhen(Date when) {
        this.when = when;
    }

	public abstract boolean isStringResolving();
	public abstract boolean isNumberResolving();

	public Double asDouble() { return null; }
}
