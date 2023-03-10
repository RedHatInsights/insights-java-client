/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.agent;

import com.fasterxml.jackson.databind.JsonSerializer;
import com.redhat.insights.Filtering;
import com.redhat.insights.InsightsReport;
import com.redhat.insights.InsightsSubreport;
import com.redhat.insights.config.InsightsConfiguration;
import com.redhat.insights.logging.InsightsLogger;
import java.util.Map;

public class AgentBasicReport implements InsightsReport {
  public static AgentBasicReport of(InsightsLogger logger, InsightsConfiguration configuration) {
    return null;
  }

  @Override
  public Map<String, InsightsSubreport> getSubreports() {
    return null;
  }

  @Override
  public JsonSerializer<InsightsReport> getSerializer() {
    return null;
  }

  @Override
  public void generateReport(Filtering masking) {}

  @Override
  public Map<String, Object> getBasic() {
    return null;
  }

  @Override
  public String getVersion() {
    return null;
  }

  @Override
  public void setIdHash(String hash) {}

  @Override
  public String getIdHash() {
    return null;
  }

  @Override
  public void decorate(String key, String value) {}
}
