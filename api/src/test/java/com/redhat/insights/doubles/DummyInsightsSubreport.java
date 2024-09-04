/* Copyright (C) Red Hat 2022-2024 */
package com.redhat.insights.doubles;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.redhat.insights.reports.InsightsSubreport;
import java.io.IOException;

/** A generic Java application {@link InsightsSubreport} implementation. */
public final class DummyInsightsSubreport implements InsightsSubreport {

  @Override
  public void generateReport() {}

  @Override
  public String getVersion() {
    return "1.0.0";
  }

  @Override
  public JsonSerializer<InsightsSubreport> getSerializer() {
    return new JsonSerializer<InsightsSubreport>() {
      @Override
      public void serialize(
          InsightsSubreport value, JsonGenerator gen, SerializerProvider serializers)
          throws IOException {}
    };
  }
}
