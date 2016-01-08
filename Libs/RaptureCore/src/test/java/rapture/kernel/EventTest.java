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
package rapture.kernel;

import org.junit.Assert;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.api.EventApi;
import rapture.common.model.RaptureEvent;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

public class EventTest {

    EventApi event;
    CallingContext context;
    
    @Test
    public void testDoesEventExist() {
        event = Kernel.getEvent();
        context = ContextFactory.ADMIN;
        String eventURI = "event://bonzo.dog/doodah/band/UrbanSpaceman";
        Assert.assertFalse(eventURI, event.eventExists(context, eventURI));   
        
        RaptureEvent ev = new RaptureEvent();
        ev.setUriFullPath(eventURI);
        event.putEvent(context, ev);
        Assert.assertTrue(eventURI, event.eventExists(context, eventURI));
        event.deleteEvent(context, eventURI);
        Assert.assertFalse(eventURI, event.eventExists(context, eventURI));   
    }
}
