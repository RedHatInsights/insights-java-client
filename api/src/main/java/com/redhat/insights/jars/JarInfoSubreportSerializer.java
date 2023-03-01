/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.jars;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.redhat.insights.InsightsSubreport;
import java.io.IOException;

public class JarInfoSubreportSerializer extends JsonSerializer<InsightsSubreport> {
  @Override
  public void serialize(
      InsightsSubreport subreport, JsonGenerator generator, SerializerProvider serializerProvider)
      throws IOException {
    JarInfoSubreport insightsSubreport = (JarInfoSubreport) subreport;
    generator.writeStartObject();
    generator.writeStringField("version", insightsSubreport.getVersion());
    generator.writeFieldName("jars");
    generator.writeStartArray();
    for (JarInfo jarInfo : insightsSubreport.getJarInfos()) {
      generator.writeStartObject();
      generator.writeStringField("name", jarInfo.name());
      generator.writeStringField("version", jarInfo.version());
      generator.writeObjectField("attributes", jarInfo.attributes());
      generator.writeEndObject();
    }
    generator.writeEndArray();
    generator.writeEndObject();
    generator.flush();
  }
}
