/* Copyright (C) Red Hat 2022-2023 */
package com.redhat.insights;

import static com.redhat.insights.InsightsErrorCode.ERROR_SERIALIZING_TO_JSON;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

/**
 * Top-level insights report.
 *
 * @see InsightsSubreport for runtime-specific sub-reports
 */
public interface InsightsReport {

  Map<String, InsightsSubreport> getSubreports();

  JsonSerializer<InsightsReport> getSerializer();

  /**
   * Filtering must be passed in as it may not be possible to determine the filtering level when
   * this object is created - the specific product may have to tell us.
   *
   * @param masking a function to filter some insight entries
   */
  void generateReport(Filtering masking);

  Map<String, Object> getBasic();

  String getVersion();

  void setIdHash(String hash);

  String getIdHash();

  void decorate(String key, String value);

  /**
   * Serializes this report to JSON for transport
   *
   * @return JSON serialized report
   */
  default String serialize() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());

    SimpleModule simpleModule =
        new SimpleModule(
            "SimpleModule", new Version(1, 0, 0, null, "com.redhat.insights", "runtimes-java"));
    simpleModule.addSerializer(InsightsReport.class, getSerializer());
    for (InsightsSubreport subreport : getSubreports().values()) {
      simpleModule.addSerializer(subreport.getClass(), subreport.getSerializer());
    }
    mapper.registerModule(simpleModule);

    StringWriter writer = new StringWriter();
    try {
      mapper.writerWithDefaultPrettyPrinter().writeValue(writer, this);
    } catch (IOException e) {
      throw new InsightsException(ERROR_SERIALIZING_TO_JSON, "JSON serialization exception", e);
    }
    return writer.toString();
  }
}
