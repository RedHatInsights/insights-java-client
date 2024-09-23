/* Copyright (C) Red Hat 2023-2024 */
package com.redhat.insights.http;

import static com.redhat.insights.InsightsErrorCode.ERROR_CLIENT_FAILED;

import com.redhat.insights.InsightsException;
import com.redhat.insights.logging.InsightsLogger;
import com.redhat.insights.reports.InsightsReport;
import java.util.Arrays;
import java.util.List;
import org.jspecify.annotations.NullMarked;

/**
 * An implementation of the Insights client interface that can try multiple connection paths. This
 * includes the expected primary use case of HTTPS, then File.
 */
@NullMarked
public class InsightsMultiClient implements InsightsHttpClient {
  private final InsightsLogger logger;
  private final List<InsightsHttpClient> clients;

  public InsightsMultiClient(InsightsLogger logger, List<InsightsHttpClient> clients) {
    this.logger = logger;
    this.clients = clients;
  }

  public InsightsMultiClient(InsightsLogger logger, InsightsHttpClient... clients) {
    this.logger = logger;
    this.clients = Arrays.asList(clients);
  }

  @Override
  public void decorate(InsightsReport report) {
    logger.warning("Multiclients do not support direct decoration of reports");
  }

  @Override
  public void sendInsightsReport(String filename, InsightsReport report) {
    String previousExceptionMsg = "";
    for (InsightsHttpClient client : clients) {
      try {
        if (!"".equals(previousExceptionMsg)) {
          report.decorate("app.client.exception", previousExceptionMsg);
        }
        client.sendInsightsReport(filename, report);
        return;
      } catch (InsightsException x) {
        logger.debug("Client failed, trying next", x);
        previousExceptionMsg = x.getMessage();
      }
    }
    throw new InsightsException(ERROR_CLIENT_FAILED, "All clients failed: " + clients);
  }

  @Override
  public boolean isReadyToSend() {
    boolean isReady = false;
    for (InsightsHttpClient client : clients) {
      isReady = isReady || client.isReadyToSend();
    }
    return isReady;
  }
}
