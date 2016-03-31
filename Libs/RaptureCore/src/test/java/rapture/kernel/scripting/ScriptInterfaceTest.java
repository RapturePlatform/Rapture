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
import java.util.List;
import java.util.Map;

import org.antlr.runtime.RecognitionException;
import org.junit.Before;
import org.junit.Test;
import org.testng.Assert;

import rapture.common.impl.jackson.JacksonUtil;
import rapture.kernel.ContextFactory;
import rapture.script.ScriptFactory;
import rapture.script.reflex.ReflexRaptureScript;
import reflex.MetaParam;
import reflex.MetaReturn;
import reflex.MetaScriptInfo;
import reflex.ReflexParser;

public class ScriptInterfaceTest {
    String scriptName = "scriptInterface.script";
    
    @Before
    public void setup() {
        ScriptFactory.init();
    }
    
    public static String getResourceAsString(Object context, String path) throws FileNotFoundException,RuntimeException {
        System.out.println("looking for src/test/resources" + path);
        InputStream is = (context == null ? ScriptInterfaceTest.class : context.getClass()).getResourceAsStream(path);

		// Can't open as a resource - try as a file
		if (is == null) {
		    System.out.println("loading file using File");
		    File f = new File("src/test/resources" + path);
			String s = f.getAbsolutePath();
			System.out.println("File path is " + s);
			is = new FileInputStream(f);
		}

		if (is != null) {
			System.out.println("loading file using Stream ");
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
        
        ReflexRaptureScript rrs = new ReflexRaptureScript();
        String test = "";
        try{
            test = getResourceAsString(this, "/" + scriptName);
        } catch (FileNotFoundException fnf){
            System.out.println("file not found. exception is " + fnf.getMessage());
        } catch (RuntimeException rte){
            System.out.println("Runtime exception " + rte.getMessage());
        }
        
        Assert.assertFalse(test == null); //fail test if script is not loaded.
        
        System.out.println("test script file: " + test);
        
        ReflexParser parser = rrs.getParser(ContextFactory.getKernelUser(), test);
        parser.parse();
        MetaScriptInfo info = parser.scriptInfo;
        if (info == null) {
        	System.out.println("No meta info");
        } else {
        	System.out.println(JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(info)));
        }
        
        List<MetaParam> parameters = info.getParameters();
        MetaReturn returnInfo = info.getReturnInfo();
        Map<String, String> properties = info.getProperties();
        
        Assert.assertEquals(parameters.size(),5);
        Assert.assertEquals(parameters.get(0).getParameterName(),"a");
        Assert.assertEquals(parameters.get(0).getDescription(),"The a parameter");
        Assert.assertEquals(parameters.get(1).getParameterName(),"b");
        Assert.assertEquals(parameters.get(2).getParameterName(),"c");
        Assert.assertEquals(parameters.get(3).getParameterName(),"d");
        Assert.assertEquals(parameters.get(4).getParameterName(),"e");
        Assert.assertEquals(parameters.get(0).getParameterType(),"string");
        Assert.assertEquals(parameters.get(1).getParameterType(),"string");
        Assert.assertEquals(parameters.get(2).getParameterType(),"string");
        Assert.assertEquals(parameters.get(3).getParameterType(),"string");
        Assert.assertEquals(parameters.get(4).getParameterType(),"number");
        
        Assert.assertEquals(returnInfo.getType(),"string");
        Assert.assertEquals(returnInfo.getDescription(),"Just the parameters put together");
        
        String color = properties.get("color");
        Assert.assertEquals(color, "blue");
    }
}
