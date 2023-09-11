/* Copyright (C) Red Hat 2022-2023 */
package com.redhat.insights;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** JSON serializer for an {@link InsightsReport} object. */
public class InsightsReportSerializer extends JsonSerializer<InsightsReport> {

  @Override
  public void serialize(
      InsightsReport insightsReport, JsonGenerator generator, SerializerProvider serializerProvider)
      throws IOException {
    generator.writeStartObject();
    generator.writeStringField("version", insightsReport.getVersion());
    String hash = insightsReport.getIdHash();
    if (hash != null && !hash.equals("")) {
      generator.writeStringField("idHash", hash);
    }
    if (!insightsReport.getBasic().isEmpty()) {
      generator.writeObjectField("basic", insightsReport.getBasic());
    }
    byte[] subReport = insightsReport.getSubModulesReport();
    generator.writeRaw(new String(subReport, StandardCharsets.UTF_8));
    generator.writeEndObject();
    generator.flush();
  }
}
