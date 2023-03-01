/* Copyright (C) Red Hat 2022-2023 */
package com.redhat.insights;

import com.fasterxml.jackson.databind.JsonSerializer;

/**
 * Runtime-dependent sub-report.
 *
 * @see TopLevelReportBaseImpl
 */
public interface InsightsSubreport {

  void generateReport();

  String getVersion();

  JsonSerializer<InsightsSubreport> getSerializer();

  // add filtering
}
