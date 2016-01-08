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
package reflex.calendar;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * This calendar ensures that the date falls M-F.
 * 
 * @author amkimian
 * 
 */
public class BDForwardActual implements CalendarHandler {

    @Override
    public Date addDays(Date orig, int days) {
        Calendar gc = GregorianCalendar.getInstance();
        gc.setTime(orig);
        // TODO: This is not very efficient, there are probably better ways of
        // doing this.
        for (int i = 0; i < Math.abs(days); i++) {
            gc.add(Calendar.DATE, days >= 0 ? 1 : -1);
            if (gc.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                gc.add(Calendar.DATE, days >= 0 ? 2 : -1);
            } else if (gc.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                gc.add(Calendar.DATE, days >= 0 ? 1 : -2);
            }
        }

        return gc.getTime();
    }

}
