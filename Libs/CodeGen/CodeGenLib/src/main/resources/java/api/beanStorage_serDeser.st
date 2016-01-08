static {
    StorableObjectMapper.addSerDes(<type>.class, new <type>Serializer(), new <type>Deserializer());
}

<if (serDeserHelper)>
private static final <serDeserHelper> <serDeserHelper; format="uncapitalize"> = new <serDeserHelper>();
<endif>

private static class <type>Deserializer extends JsonDeserializer\<<type>\> {

    @Override
    public <type> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
<if (serDeserHelper)>
        <type>Extended extended = JacksonUtil.DEFAULT_MAPPER.readValue(p, <type>Extended.class);
        if (extended.get_raptureVersion() == null) {
            //if no version found in serialized form then it means this was serialized before we had versions
            extended.set_raptureVersion(OldestVersion.INSTANCE);
        }

        <type> <type; format="uncapitalize"> = new <type>();
        <serDeserHelper; format="uncapitalize">.onDeserialize(extended);
        <deserSetters; separator="\n">
<else>
        <type> <type; format="uncapitalize"> = JacksonUtil.DEFAULT_MAPPER.readValue(p, <type>.class);
<endif>


        <type; format="uncapitalize">.set_raptureVersion(ServerApiVersion.getApiVersion());
        return <type; format="uncapitalize">;
    }
}

private static class <type>Serializer extends com.fasterxml.jackson.databind.JsonSerializer\<<type>\> {
    @Override
    public void serialize(<type> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        value.set_raptureVersion(ServerApiVersion.getApiVersion());
<if (serDeserHelper)>

        <serDeserHelper; format="uncapitalize">.onSerialize(value);
<endif>

        gen.writeRaw(JacksonUtil.DEFAULT_MAPPER.writeValueAsString(value));
    }
}
