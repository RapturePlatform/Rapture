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
package rapture.kernel;

import static org.junit.Assert.assertEquals;

import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

import rapture.common.Messages;

public class MessagesTest {

    @Before
    public void setUp() throws Exception {
    }

    @SuppressWarnings("deprecation")
    @Test
    public void test() {
        
        // legacy method. No 18N support. Still works though.
        assertEquals("Locale DEFAULT test string {0} {1} {2}", Messages.getString("Test.Dummy"));                                             //$NON-NLS-1$ //$NON-NLS-2$
       
        Messages messageCatalog = new Messages("Test"); //$NON-NLS-1$
        
        // no args
        assertEquals("Locale DEFAULT test string {0} {1} {2}", messageCatalog.getMessage("Dummy").format());                                         //$NON-NLS-1$ //$NON-NLS-2$
        // Single arg
        assertEquals("Locale DEFAULT test string ARG0 {1} {2}", messageCatalog.getMessage("Dummy", "ARG0").format());                             //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        // args as array
        assertEquals("Locale DEFAULT test string ARG0 ARG1 {2}", messageCatalog.getMessage("Dummy", new String[] {"ARG0", "ARG1"}).format());               //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        assertEquals("Locale DEFAULT test string ARG0 ARG1 ARG2", messageCatalog.getMessage("Dummy", new String[] {"ARG0", "ARG1", "ARG2"}).format());  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        
        Messages uk = new Messages("Test", Locale.UK); //$NON-NLS-1$
        
        assertEquals("Locale en_GB for unit tests {0} {1} {2}", uk.getMessage("Dummy").format());                                         //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("Locale en_GB for unit tests ARG0 {1} {2}", uk.getMessage("Dummy", "ARG0").format());                             //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        assertEquals("Locale en_GB for unit tests ARG0 ARG1 {2}", uk.getMessage("Dummy", new String[] {"ARG0", "ARG1"}).format());               //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        assertEquals("Locale en_GB for unit tests ARG0 ARG1 ARG2", uk.getMessage("Dummy", new String[] {"ARG0", "ARG1", "ARG2"}).format());  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        
        // Static call
        assertEquals("Locale DEFAULT test string {0} {1} {2}", Messages.getMessage("Test", "Dummy", null, null).format());                                         //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        assertEquals("Locale en_GB for unit tests {0} {1} {2}", Messages.getMessage("Test", "Dummy", null, Locale.UK).format());                                         //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

    }

}