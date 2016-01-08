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
package rapture.common.uri;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.exception.RaptureException;

public class URIParserTest {

    private static ArrayList<String> goodList;
    private static ArrayList<String> badList;

    @Before
    public void before() {
        int size = 100000;
        goodList = new ArrayList<String>(size);
        goodList.add("job://jobauthority/abc");
        goodList.add("job://jobauthority/abc");
        goodList.add("job://jobauthority/abc#bookmark");
        goodList.add("job://jobauthority/abc@timeSpec");
        goodList.add("job://jobauthority/abc$attr");
        goodList.add("//jobauthority/abc$attr");
        goodList.add("job://jobauthority/$attr");
        goodList.add("job://jobauthority/#$attr");
        goodList.add("job://jobauthority/#$@timespec");
        goodList.add("job://jobauthority/#$@");
        for (int i = 0; i < size; i++) {
            String toParse = String.format("job://job.au.t_ho-rity%s/path-has_mixed.chars.-%s/some/sub/path#bookmark@timespec$attr", 10 - i, i);
            goodList.add(toParse);
        }

        badList = new ArrayList<String>(size);
        badList.add("jobz://jobauthority/abc#bookmark@timespec$attr");
        badList.add("job://jobauthority/abc#bookmark#timespec$attr");
        badList.add("job://jobauthority/abc@bookmark@timespec$attr");
        badList.add("job://jobauthority/abc$bookmark@timespec$attr");
        // removed temporarily
        // badList.add("jobauthority/abc#bookmark@timespec$attr");
        badList.add("job://abc#bookmark@timespec$attr");
        badList.add("job://#bookmark@timespec$attr");
        badList.add("job://#bookmark@timespec$attr/abc");
        badList.add("://jobauthority/abc$attr");

    }

    @Test
    public void testGood() throws URISyntaxException {
        int index = 0;
        long totalSize = 0;

        for (String toParse : goodList) {
            index++;
            long after;
            long before = getTime();
            before = getTime();
            RaptureURI result = new RaptureURI(toParse, Scheme.DOCUMENT);
            after = getTime();
            if (index % 1000 == 0) {
                System.out.println("Result is " + result.toString());
            }
            totalSize = totalSize + (after - before);

        }
        System.out.println("Difference for good list is " + totalSize);
    }

    @Test
    public void testBad() {
        long totalSize = 0;
        for (String toParse : badList) {
            long after;
            long before = getTime();
            try {
                before = getTime();
                RaptureURI result = new RaptureURI(toParse, Scheme.DOCUMENT);
                after = getTime();
                fail("Should fail on bad list entry " + toParse + " but instead got " + result);
            } catch (RaptureException e) {
                System.out.println(e.getFormattedMessage());
                if (e.getCause() != null) {
                    System.out.println(e.getCause().getMessage());
                    assertTrue("Expected URISyntaxException - " + e.getFormattedMessage() + " : " + e.getCause().getMessage(),
                            e.getCause() instanceof URISyntaxException);
                } else {
                    fail("Expected URISyntaxException for " + toParse + " - got " + e.getFormattedMessage());
                }
                after = getTime();
            }
            totalSize = totalSize + (after - before);

        }
        System.out.println("Difference for bad list is " + totalSize);
    }

    // private static final ThreadMXBean bean = ManagementFactory.getThreadMXBean();

    static long getTime() {
        // return bean.getCurrentThreadCpuTime() / 1000000;
        return System.currentTimeMillis();
    }
}
