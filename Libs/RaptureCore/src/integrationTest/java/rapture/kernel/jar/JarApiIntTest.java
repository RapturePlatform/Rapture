package rapture.kernel.jar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import rapture.common.RaptureScript;
import rapture.common.RaptureScriptLanguage;
import rapture.common.RaptureScriptPurpose;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.client.HttpBlobApi;
import rapture.common.client.HttpDecisionApi;
import rapture.common.client.HttpDocApi;
import rapture.common.client.HttpJarApi;
import rapture.common.client.HttpLoginApi;
import rapture.common.client.HttpScriptApi;
import rapture.common.client.HttpTagApi;
import rapture.common.client.SimpleCredentialsProvider;

public class JarApiIntTest {

    @Test
    public void testS() {
        String s = "'";
        System.out.println(s.startsWith("'"));
        System.out.println(s.endsWith("'"));

        if (s.length() > 1) {
            System.out.println("[" + s.substring(1, s.length() - 1) + "]");
        }
        List<String> x = new ArrayList<>();
        x.add(null);
        x.add(null);
        System.out.println(x.get(0));
    }

    @Test
    public void testBasic() throws IOException {
        String host = "http://localhost:8665/rapture";
        SimpleCredentialsProvider creds = new SimpleCredentialsProvider("rapture", "rapture");
        HttpLoginApi login = new HttpLoginApi(host, creds);
        login.login();
        HttpJarApi jar = new HttpJarApi(login);
        byte[] jarBytes = IOUtils.toByteArray(this.getClass().getResourceAsStream("/dp/c24-io-api-4.8.3.jar"));
        jar.putJar("jar://c24/4.8.3/c24-io-api-4.8.3.jar", jarBytes);

    }

    @Test
    public void testRunWorkflow() {
        String host = "http://localhost:8665/rapture";
        SimpleCredentialsProvider creds = new SimpleCredentialsProvider("rapture", "rapture");
        HttpLoginApi login = new HttpLoginApi(host, creds);
        login.login();
        HttpDecisionApi workflow = new HttpDecisionApi(login);
        workflow.createWorkOrder("//workflows/dynamic", new HashMap<>());
    }

    @Test
    public void testDelBlobs() {
        String host = "http://192.168.99.100:8665/rapture";
        SimpleCredentialsProvider creds = new SimpleCredentialsProvider("rapture", "rapture");
        HttpLoginApi login = new HttpLoginApi(host, creds);
        login.login();
        HttpBlobApi blob = new HttpBlobApi(login);
        blob.deleteBlobsByUriPrefix("blob://matrix.archive/tradeweb/2016/05/24");
    }

    @Test
    public void testPutBlobs() {
        String host = "http://192.168.99.100:8665/rapture";
        SimpleCredentialsProvider creds = new SimpleCredentialsProvider("rapture", "rapture");
        HttpLoginApi login = new HttpLoginApi(host, creds);
        login.login();
        HttpBlobApi blob = new HttpBlobApi(login);
        blob.putBlob("blob://matrix.archive/tradweb/test", "8=FIX.4.49=5935=034=77649=TRADEWEB52=20160525-17:00:02.52056=SSSNYC10=116".getBytes(), "text/fix");
    }

    @Test
    public void testRunWorkflowMatrix() {
        String host = "http://localhost:8665/rapture";
        SimpleCredentialsProvider creds = new SimpleCredentialsProvider("rapture", "rapture");
        HttpLoginApi login = new HttpLoginApi(host, creds);
        login.login();
        HttpDecisionApi workflow = new HttpDecisionApi(login);
        workflow.createWorkOrder("//matrix.workflows/eurobrokers", new HashMap<>());
    }

    @Test
    public void testRunScript() {
        String host = "http://localhost:8665/rapture";
        SimpleCredentialsProvider creds = new SimpleCredentialsProvider("rapture", "rapture");
        HttpLoginApi login = new HttpLoginApi(host, creds);
        login.login();
        HttpScriptApi script = new HttpScriptApi(login);
        script.runScript("script://scriptExample/dynamic", new HashMap<>());
    }

    @Test
    public void testPutScript() {
        String host = "http://localhost:8665/rapture";
        SimpleCredentialsProvider creds = new SimpleCredentialsProvider("rapture", "rapture");
        HttpLoginApi login = new HttpLoginApi(host, creds);
        login.login();
        HttpScriptApi script = new HttpScriptApi(login);
        RaptureScript s = new RaptureScript();
        s.setName("s1");
        s.setAuthority("testscripts");
        s.setLanguage(RaptureScriptLanguage.REFLEX);
        s.setPurpose(RaptureScriptPurpose.PROGRAM);
        s.setScript("import dynamicModule as dm from ('jar://reflexModules/DynamicModule-1.0.0', 'jar://com.google.code.gson/gson/2.6.2/gson-2.6.2');\n" +
                "return $dm.prettyPrintJson('{\"data1\":100,\"data2\":\"hello\",\"list\":[\"String 1\",\"String 2\",\"String 3\"]}');");
        script.putScript("//testscripts/s1", s);
        // System.out.println(script.runScript("//testscripts/s1", new HashMap<>()));
    }

    @Test
    public void testUri() {
        System.out.println(new RaptureURI("blob://test/x", Scheme.DOCUMENT).toString());
    }

    @Test
    public void testPutScript2() {
        String host = "http://localhost:8665/rapture";
        SimpleCredentialsProvider creds = new SimpleCredentialsProvider("rapture", "rapture");
        HttpLoginApi login = new HttpLoginApi(host, creds);
        login.login();
        HttpScriptApi script = new HttpScriptApi(login);
        String authority = "ts5";
        RaptureScript s = new RaptureScript();
        s.setName("s1");
        s.setAuthority(authority);
        s.setLanguage(RaptureScriptLanguage.REFLEX);
        s.setPurpose(RaptureScriptPurpose.PROGRAM);
        s.setScript("println('hello');");
        script.putScript("//" + authority + "/s1", s);
        System.out.println(script.runScript("//" + authority + "/s1", new HashMap<>()));
    }

    @Test
    public void testApplyTag() {
        String host = "http://localhost:8665/rapture";
        SimpleCredentialsProvider creds = new SimpleCredentialsProvider("rapture", "rapture");
        HttpLoginApi login = new HttpLoginApi(host, creds);
        login.login();
        HttpTagApi tag = new HttpTagApi(login);
        tag.applyTag("script://testscripts/s1", "testTag", "testValue");
    }

    @Test
    public void testGetDoc() {
        String host = "http://localhost:8665/rapture";
        SimpleCredentialsProvider creds = new SimpleCredentialsProvider("rapture", "rapture");
        HttpLoginApi login = new HttpLoginApi(host, creds);
        login.login();
        HttpDocApi doc = new HttpDocApi(login);
        System.out.println(doc.getDoc("document://sys.config/script/testscripts/s1"));
    }
}
