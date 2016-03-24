package rapture.kernel.scripting;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

import org.antlr.runtime.RecognitionException;
import org.junit.Before;
import org.junit.Test;

import rapture.kernel.ContextFactory;
import rapture.script.ScriptFactory;
import rapture.script.reflex.ReflexRaptureScript;
import reflex.MetaScriptInfo;
import reflex.ReflexParser;

public class ScriptInterfaceTest {
    @Before
    public void setup() {
        ScriptFactory.init();
    }
    
    public static String getResourceAsString(Object context, String path) {
		InputStream is = (context == null ? ScriptInterfaceTest.class : context.getClass()).getResourceAsStream(path);

		// Can't open as a resource - try as a file
		if (is == null) {
			File f = new File("src/test/resources" + path);
			String s = f.getAbsolutePath();
			System.out.println(s);
			try {
				is = new FileInputStream(f);
			} catch (FileNotFoundException e) {
				// oops
			}
		}

		if (is != null) {
			Writer writer = new StringWriter();
			char[] buffer = new char[1024];
			try {
				Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
				int n;
				while ((n = reader.read(buffer)) != -1) {
					writer.write(buffer, 0, n);
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			} finally {
				try {
					is.close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
			return writer.toString();
		}
		// Can't open. Return null so that the caller doesn't assume we
		// succeeded.
		return null;
	}

    @Test
    public void testInputProgram() throws RecognitionException {
    	// We want to load Jonathan's script and put it through the same tests that the 
    	// ScriptApiImpl does.
    	
        //assertTrue(testScript("println('hello');println(_params['hello']);return 5;\n").isEmpty());
    	
        ReflexRaptureScript rrs = new ReflexRaptureScript();
        String test = getResourceAsString(this, "/scriptinterface.script");
        ReflexParser parser = rrs.getParser(ContextFactory.getKernelUser(), test);
        parser.parse();
        MetaScriptInfo info = parser.scriptInfo;
        if (info == null) {
        	System.out.println("No meta info");
        }
    }
}
