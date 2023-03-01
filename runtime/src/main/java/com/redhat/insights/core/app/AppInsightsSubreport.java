/* Copyright (C) Red Hat 2022-2023 */
package com.redhat.insights.core.app;

import com.fasterxml.jackson.databind.JsonSerializer;
import com.redhat.insights.InsightsSubreport;

/** A generic Java application {@link InsightsSubreport} implementation. */
public final class AppInsightsSubreport implements InsightsSubreport {

  @Override
  public void generateReport() {}

  @Override
  public String getVersion() {
    return "1.0.0";
  }

  @Override
  public JsonSerializer<InsightsSubreport> getSerializer() {
    return new AppInsightsReportSerializer();
  }
}
