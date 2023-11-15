/* Copyright (C) Red Hat 2022-2023 */
package com.redhat.insights.reports;

import com.fasterxml.jackson.databind.JsonSerializer;

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
