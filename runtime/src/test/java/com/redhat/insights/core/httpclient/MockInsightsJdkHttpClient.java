/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.core.httpclient;

import com.redhat.insights.config.InsightsConfiguration;
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
  HttpClient getHttpClient() {
    return this.mock;
  }
}
