/* Copyright (C) Red Hat 2022-2023 */
package com.redhat.insights.reports;

import com.fasterxml.jackson.databind.JsonSerializer;

/**
 * Runtime-dependent sub-report.
 *
 * @see AbstractTopLevelReportBase
 */
public interface InsightsSubreport {

  void generateReport();

  String getVersion();

  JsonSerializer<InsightsSubreport> getSerializer();

  // add filtering
}
