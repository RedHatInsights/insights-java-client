/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.agent;

import com.redhat.insights.InsightsReport;
import com.redhat.insights.http.InsightsHttpClient;
import com.redhat.insights.logging.InsightsLogger;
import java.util.function.Supplier;
import javax.net.ssl.SSLContext;

public class InsightsAgentHttpClient implements InsightsHttpClient {
  private final InsightsLogger logger;
  private final AgentConfiguration configuration;
  private final Supplier<SSLContext> sslContextSupplier;
  private final boolean useMTLS;

  public InsightsAgentHttpClient(
      InsightsLogger logger, AgentConfiguration configuration, Supplier<SSLContext> supplier) {
    this.logger = logger;
    this.configuration = configuration;
    this.sslContextSupplier = supplier;
    this.useMTLS = !configuration.getMaybeAuthToken().isPresent();
  }

  @Override
  public void decorate(InsightsReport report) {}

  @Override
  public void sendInsightsReport(String filename, InsightsReport report) {}

  @Override
  public boolean isReadyToSend() {
    return InsightsHttpClient.super.isReadyToSend();
  }
}
