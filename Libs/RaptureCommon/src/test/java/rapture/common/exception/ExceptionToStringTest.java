package rapture.common.exception;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ExceptionToStringTest {

    @Test
    public void testSummary() {
        Throwable t1 = new RuntimeException("This is where it starts");
        Throwable t2 = new RuntimeException("This is in the middle", t1);
        Throwable t3 = new RuntimeException("This is where it ends", t2);

        String s = ExceptionToString.summary(t3);
        assertEquals("java.lang.RuntimeException: This is where it ends\n" + "Caused by: java.lang.RuntimeException: This is in the middle\n"
                + "Caused by: java.lang.RuntimeException: This is where it starts\n", s);
    }

    @Test
    public void testCause() {
        Throwable t1 = new RuntimeException("This is where it starts");
        Throwable t2 = new RuntimeException("This is in the middle", t1);
        Throwable t3 = new RuntimeException("This is where it ends", t2);

        Throwable t = ExceptionToString.getRootCause(t3);
        assertEquals(t1, t);
    }
}
