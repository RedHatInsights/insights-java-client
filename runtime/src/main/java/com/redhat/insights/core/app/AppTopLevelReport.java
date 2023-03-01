/* Copyright (C) Red Hat 2022-2023 */
package com.redhat.insights.core.app;

import com.redhat.insights.InsightsSubreport;
import com.redhat.insights.core.TopLevelReportBaseImpl;
import com.redhat.insights.jars.ClasspathJarInfoSubreport;
import com.redhat.insights.logging.InsightsLogger;
import java.util.Map;

/**
 * A generic Java application top-level report implementation with the application classpath
 * entries.
 */
public class AppTopLevelReport extends TopLevelReportBaseImpl {
  public AppTopLevelReport(InsightsLogger logger, Map<String, InsightsSubreport> subReports) {
    super(logger, subReports);
  }

  public static AppTopLevelReport of(InsightsLogger logger) {
    return new AppTopLevelReport(
        logger,
        Map.of(
            "jars", new ClasspathJarInfoSubreport(logger), "details", new AppInsightsSubreport()));
  }

  public Map<String, String> getNecessary() {
    return Map.of("name", "unknown_app");
  }
}
