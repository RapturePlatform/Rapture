package rapture.common.mime;

import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.net.MediaType;

import rapture.common.BlobUpdateObject;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.model.DocumentMetadata;
import rapture.common.model.DocumentWithMeta;
import rapture.common.version.ApiVersion;

public class MimeSearchUpdateObjectTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void testBlobWithDAndWithouLegacyDoc() {
        MimeSearchUpdateObject msuo = new MimeSearchUpdateObject();
        DocumentWithMeta doc = new DocumentWithMeta();
        doc.set_raptureVersion(new ApiVersion(10,5,38));
        doc.setContent("Fire On High");
        doc.setDisplayName("ELO");
        DocumentMetadata met = new DocumentMetadata();
        met.set_raptureVersion(new ApiVersion(10,5,38));
        met.setComment("The music is reversible");
        met.setCreatedTimestamp(10538L);
        met.setDeleted(false);
        met.setUser("Jeff Lynne");
        doc.setMetaData(met);
        msuo.setDoc(doc); // ignored
        
        msuo.setRepo("El Dorado");
        msuo.setSearchRepo("Mr Blue Sky");
        msuo.setUri(RaptureURI.builder(Scheme.BLOB, "Foo").docPath("bar/baz").build());
        
        msuo.setUpdateObject(new BlobUpdateObject(msuo.getUri(), "Foo".getBytes(), MediaType.ANY_TEXT_TYPE.toString()));

        String json = JacksonUtil.jsonFromObject(msuo);
        
        MimeSearchUpdateObject msuo2 = JacksonUtil.objectFromJson(json, MimeSearchUpdateObject.class);
        assertTrue(msuo.equals(msuo2));

        String jsonWithDoc = "{" + " \"doc\": {" + " \"displayName\": \"ELO\"," + " \"metaData\": {" + " \"createdTimestamp\": 10538,"
                + " \"user\": \"Jeff Lynne\"," + " \"comment\": \"The music is reversible\"," + " \"deleted\": false," + " \"tags\": {},"
                + " \"semanticUri\": \"\"," + " \"_raptureVersion\": {" + " \"minor\": 10," + " \"major\": 5," + " \"micro\": 38" + " }" + " },"
                + " \"content\": \"Fire On High\"," + " \"_raptureVersion\": {" + " \"minor\": 10," + " \"major\": 5," + " \"micro\": 38" + " }" + " },"
                + " \"updateObject\": {" + " \"blobUpdateObject\": {" + " \"payload\": {" + " \"headers\": {}," + " \"content\": \"Rm9v\"" + " },"
                + " \"uri\": \"blob://Foo/bar/baz\"," + " \"mimeType\": \"text/*\"" + " }" + " }," + " \"uri\": \"blob://Foo/bar/baz\","
                + " \"repo\": \"El Dorado\"," + " \"searchRepo\": \"Mr Blue Sky\"" + "}";
        
        MimeSearchUpdateObject msuo3 = JacksonUtil.objectFromJson(jsonWithDoc, MimeSearchUpdateObject.class);
        assertTrue(msuo.equals(msuo3));
    }

}
