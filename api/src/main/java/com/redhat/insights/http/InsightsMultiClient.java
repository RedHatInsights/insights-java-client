/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.http;

import com.redhat.insights.InsightsException;
import com.redhat.insights.logging.InsightsLogger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * An implementation of the Insights client interface that can try multiple connection paths. This
 * includes the expected primary use case of HTTPS, then File.
 */
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
  public void sendInsightsReport(String filename, String report) {
    String previousExceptionMsg = "";
    for (InsightsHttpClient client : clients) {
      try {
        client.sendInsightsReport(filename + previousExceptionMsg, report);
        return;
      } catch (InsightsException x) {
        logger.debug("Client failed, trying next", x);
        byte[] msgBytes = x.getMessage().getBytes(StandardCharsets.UTF_8);
        previousExceptionMsg = "__" + Base64.getEncoder().encodeToString(msgBytes);
      }
    }
    throw new InsightsException("All clients failed: " + clients);
  }

  @Override
  public void sendInsightsReport(String filename, byte[] gzipReport) {
    throw new InsightsException("Multiclients do not support direct send of compressed data");
  }
}
