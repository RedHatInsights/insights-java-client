/* Copyright (C) Red Hat 2022-2023 */
package com.redhat.insights.reports;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;

/** A generic Java application {@link InsightsSubreport} JSON serializer implementation. */
public class AppInsightsReportSerializer extends JsonSerializer<InsightsSubreport> {

  @Override
  public void serialize(
      InsightsSubreport appInsightsReportAdapter,
      JsonGenerator generator,
      SerializerProvider serializerProvider)
      throws IOException {
    generator.writeStartObject();
    // Put some identifying text here
    generator.writeStringField("type", "General Java app");
    generator.writeEndObject();
    generator.flush();
  }
}
