package rapture.common;

import java.math.BigDecimal;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import rapture.common.impl.jackson.JacksonUtil;

public class RaptureJsonSerializerTest {

    class Bar {
        @JsonProperty
        @JsonSerialize(using = RaptureJsonSerializer.class)
        BigDecimal bdval = new BigDecimal("0.00000001");

        @JsonSerialize(using = RaptureJsonSerializer.class)
        @JsonProperty
        Double Dval = 0.00000001;

        @JsonProperty
        @JsonSerialize(using = RaptureJsonSerializer.class)
        Float Fval = 0.00000001f;

        @JsonSerialize(using = RaptureJsonSerializer.class)
        @JsonProperty
        Double Dlval = 1000000000D;

        @JsonProperty
        @JsonSerialize(using = RaptureJsonSerializer.class)
        Float Flval = 1000000000f;

        @JsonProperty
        @JsonSerialize(using = RaptureJsonSerializer.class)
        int ilval = 1000000000;

        @JsonProperty
        @JsonSerialize(using = RaptureJsonSerializer.class)
        Integer Iilval = 1000000000;

        @JsonSerialize(using = RaptureJsonSerializer.class)
        @JsonProperty
        long lilval = 1000000000l;

        @JsonSerialize(using = RaptureJsonSerializer.class)
        @JsonProperty
        Long Llval = 1000000000l;

        public Bar() {
            super();
        }

        @Override
        public String toString() {
            return "Bar [bdval=" + bdval + ", Dval=" + Dval + ", Fval=" + Fval + ", Dlval=" + Dlval + ", Flval=" + Flval + ", ilval=" + ilval + ", Iilval="
                    + Iilval + ", lilval=" + lilval + ", Llval=" + Llval + "]";
        }

    }

    class Foo {
        @JsonProperty
        Bar bar;

        public Foo() {
            super();
            bar = new Bar();
        }

        @Override
        public String toString() {
            return "Foo [bar=" + bar + "]";
        }

    }

    @Test
    public void test() {
        Foo foo = new Foo();
        System.out.println(foo.toString());
        String s = JacksonUtil.jsonFromObject(foo);
        Assert.assertEquals(
                "{\"bar\":{\"bdval\":0.00000001,\"Dval\":0.00000001,\"Fval\":0.00000001,\"Dlval\":1000000000,\"Flval\":1000000000,\"ilval\":1000000000,\"Iilval\":1000000000,\"lilval\":1000000000,\"Llval\":1000000000}}",
                s);
    }
}
