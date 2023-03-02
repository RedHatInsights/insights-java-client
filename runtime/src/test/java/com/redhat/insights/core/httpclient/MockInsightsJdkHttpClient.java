/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.core.httpclient;

import com.redhat.insights.InsightsReport;
import com.redhat.insights.config.InsightsConfiguration;
import com.redhat.insights.http.InsightsHttpClient;
import com.redhat.insights.logging.InsightsLogger;
import java.net.http.HttpClient;
import java.util.function.Supplier;
import javax.net.ssl.SSLContext;

public class MockInsightsJdkHttpClient extends InsightsJdkHttpClient {
  private final HttpClient mock;

  public MockInsightsJdkHttpClient(
      InsightsConfiguration configuration,
      Supplier<SSLContext> sslContextSupplier,
      InsightsLogger logger,
      HttpClient mock) {
    super(logger, configuration, sslContextSupplier);
    this.mock = mock;
  }

  @Override
  public void sendInsightsReport(String filename, InsightsReport report) {
    var gzipJson = InsightsHttpClient.gzipReport(report.serialize());
    sendInsightsReportWithClient(mock, filename, gzipJson);
  }
}
