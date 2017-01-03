package rapture.common;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class RaptureJsonSerializer extends JsonSerializer<Number> {

    @Override
    public void serialize(Number value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
        gen.writeNumber(new BigDecimal(value.doubleValue(), (value instanceof Float) ? MathContext.DECIMAL32 : MathContext.DECIMAL64).stripTrailingZeros()
                .toPlainString());
    }
}
