package rapture.mongodb;

import java.math.BigDecimal;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

public class BigDecimalCodec implements Codec<BigDecimal> {
    @Override
    public void encode(BsonWriter writer, BigDecimal t, EncoderContext ec) {
        writer.writeDouble(t.doubleValue());
    }

    @Override
    public Class<BigDecimal> getEncoderClass() {
        return BigDecimal.class;
    }

    @Override
    public BigDecimal decode(BsonReader reader, DecoderContext dc) {
        return new BigDecimal(reader.readDouble());
    }
}

