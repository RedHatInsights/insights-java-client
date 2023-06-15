/* Copyright (C) Red Hat 2022-2023 */
package com.redhat.insights;

import static com.redhat.insights.InsightsErrorCode.ERROR_SERIALIZING_TO_JSON;
import static com.redhat.insights.InsightsErrorCode.ERROR_WRITING_FILE;

import com.fasterxml.jackson.databind.JsonSerializer;
import java.io.*;
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

  /** Serializes this report to JSON on the given {@code file}. */
  default void serialize(File out) {
    // It uses RandomAccessFile on purpose vs Files::write
    // to avoid any hidden pooled off-heap allocations
    // at the cost of additional allocation/free and copy
    try (RandomAccessFile raf = new RandomAccessFile(out, "rw")) {
      // truncate it
      raf.setLength(0);
      ObjectMappers.createFor(this).writerWithDefaultPrettyPrinter().writeValue(raf, this);
    } catch (IOException e) {
      throw new InsightsException(ERROR_WRITING_FILE, "Could not serialize JSON to: " + out, e);
    }
  }

  /**
   * Serializes this report to JSON for transport
   *
   * @return JSON serialized report as a stream of UTF-8 encoded bytes
   */
  default byte[] serialize() {
    try {
      return ObjectMappers.createFor(this).writerWithDefaultPrettyPrinter().writeValueAsBytes(this);
    } catch (IOException e) {
      throw new InsightsException(ERROR_SERIALIZING_TO_JSON, "JSON serialization exception", e);
    }
  }
}
