/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.doubles;

import com.redhat.insights.http.InsightsHttpClient;
import com.redhat.insights.reports.InsightsReport;

/**
 * Fake http client, which will store content of report sent. Designed to test @link
 * InsightsReportController
 */
public class StoringInsightsHttpClient implements InsightsHttpClient {
  private String reportFilename = null;
  private InsightsReport reportContent = null;
  private int reportsSent = 0;

  private boolean readyToSend = true;

  public StoringInsightsHttpClient() {}

  public StoringInsightsHttpClient(boolean readyToSend) {
    this.readyToSend = readyToSend;
  }

  @Override
  public void decorate(InsightsReport report) {}

  @Override
  public void sendInsightsReport(String filename, InsightsReport report) {
    reportFilename = filename + ".txt";
    reportContent = report;
    reportsSent++;
  }

  @Override
  public boolean isReadyToSend() {
    return readyToSend;
  }

  public void setReadyToSend(boolean readyToSend) {
    this.readyToSend = readyToSend;
  }

  public String getReportFilename() {
    return reportFilename;
  }

  public InsightsReport getReportContent() {
    return reportContent;
  }

  public int getReportsSent() {
    return reportsSent;
  }
}
