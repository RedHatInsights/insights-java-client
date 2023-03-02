/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.doubles;

import com.redhat.insights.InsightsReport;
import com.redhat.insights.http.InsightsHttpClient;

public final class NoopInsightsHttpClient implements InsightsHttpClient {

  @Override
  public void decorate(InsightsReport report) {}

  @Override
  public void sendInsightsReport(String filename, InsightsReport report) {
    return;
  }
}
