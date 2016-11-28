package rapture.dsl.iqry;

import org.junit.Assert;
import org.junit.Test;

public class IndexQueryFactoryTest {

    @Test
    public void testParser1() {
        IndexQuery iq = IndexQueryFactory.parseQuery("Select x order by x limit 1 skip 2");
        Assert.assertEquals("Limit", 1, iq.getLimit());
        Assert.assertEquals("Skip", 2, iq.getSkip());
        Assert.assertEquals("Order", "x", iq.getOrderBy().getFieldList().get(0));
    }

    @Test
    public void testParser2() {
        IndexQuery iq = IndexQueryFactory.parseQuery("Select x order by x skip 2 limit 1");
        Assert.assertEquals("Limit", 1, iq.getLimit());
        Assert.assertEquals("Skip", 2, iq.getSkip());
        Assert.assertEquals("Order", "x", iq.getOrderBy().getFieldList().get(0));

    }

    @Test
    public void testParser3() {
        IndexQuery iq = IndexQueryFactory.parseQuery("Select x limit 1 skip 2 order by x");
        Assert.assertEquals("Limit", 1, iq.getLimit());
        Assert.assertEquals("Skip", 2, iq.getSkip());
        Assert.assertEquals("Order", "x", iq.getOrderBy().getFieldList().get(0));

    }

    @Test
    public void testParser4() {
        IndexQuery iq = IndexQueryFactory.parseQuery("Select x skip 2 limit 1 order by x");
        Assert.assertEquals("Limit", 1, iq.getLimit());
        Assert.assertEquals("Skip", 2, iq.getSkip());
        Assert.assertEquals("Order", "x", iq.getOrderBy().getFieldList().get(0));

    }

    @Test
    public void testParser5() {
        IndexQuery iq = IndexQueryFactory.parseQuery("Select x limit 1 order by x skip 2");
        Assert.assertEquals("Limit", 1, iq.getLimit());
        Assert.assertEquals("Skip", 2, iq.getSkip());
        Assert.assertEquals("Order", "x", iq.getOrderBy().getFieldList().get(0));

    }

    @Test
    public void testParser6() {
        IndexQuery iq = IndexQueryFactory.parseQuery("Select x skip 2 order by x limit 1");
        Assert.assertEquals("Limit", 1, iq.getLimit());
        Assert.assertEquals("Skip", 2, iq.getSkip());
        Assert.assertEquals("Order", "x", iq.getOrderBy().getFieldList().get(0));

    }

    @Test
    public void testParser7() {
        IndexQuery iq = IndexQueryFactory.parseQuery("Select x skip 3 skip 4 limit 976 order by x limit 666 limit 1 skip 2");
        Assert.assertEquals("Limit", 1, iq.getLimit());
        Assert.assertEquals("Skip", 2, iq.getSkip());
        Assert.assertEquals("Order", "x", iq.getOrderBy().getFieldList().get(0));
    }

}
