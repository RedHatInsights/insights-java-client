/* Copyright (C) Red Hat 2022-2023 */
package com.redhat.insights;

import com.fasterxml.jackson.databind.JsonSerializer;
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
}
