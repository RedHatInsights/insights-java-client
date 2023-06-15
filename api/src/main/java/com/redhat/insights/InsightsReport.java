/* Copyright (C) Red Hat 2022-2023 */
package com.redhat.insights;

import static com.redhat.insights.InsightsErrorCode.ERROR_SERIALIZING_TO_JSON;

import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
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
   * @return JSON serialized report as a stream of UTF-8 encoded bytes
   */
  default byte[] serializeRaw() {
    ObjectMapper mapper = ObjectMappers.createFor(this);

    try {
      return mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(this);
    } catch (IOException e) {
      throw new InsightsException(ERROR_SERIALIZING_TO_JSON, "JSON serialization exception", e);
    }
  }

  /**
   * Serializes this report to JSON for transport
   *
   * @return JSON serialized report
   */
  default String serialize() {
    ObjectMapper mapper = ObjectMappers.createFor(this);

    try {
      return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
    } catch (IOException e) {
      throw new InsightsException(ERROR_SERIALIZING_TO_JSON, "JSON serialization exception", e);
    }
  }
}
