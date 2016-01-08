import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import rapture.common.impl.jackson.JacksonUtil;
import java.io.IOException;

<if (serDeserFullName)>
import <serDeserFullName>;
<endif>
<if (extendedName)>
import <extendedName>;
<endif>

import <apiVersionName>;

import rapture.common.version.OldestVersion;
