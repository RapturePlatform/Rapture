package rapture.common;

import static org.junit.Assert.*;

import org.junit.Test;

public class RaptureFolderInfoTest {

	@Test
	public void testGetParent() {
		assertEquals("e", new RaptureFolderInfo("a/b/c/d/e", false).trimName().getName());
		assertEquals("ae", new RaptureFolderInfo("ae", false).trimName().getName());
		assertEquals("", new RaptureFolderInfo("", false).trimName().getName());
		assertEquals("", new RaptureFolderInfo("/", false).trimName().getName());
		assertEquals(null, new RaptureFolderInfo(null, false).trimName().getName());
	}

}
