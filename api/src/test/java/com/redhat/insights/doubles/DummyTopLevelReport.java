/* Copyright (C) Red Hat 2022-2023 */
package com.redhat.insights.doubles;

import com.redhat.insights.AbstractTopLevelReportBase;
import com.redhat.insights.InsightsSubreport;
import com.redhat.insights.logging.InsightsLogger;
import java.util.Collections;
import java.util.Map;

/**
 * A generic Java application top-level report implementation with the application classpath
 * entries.
 */
public class DummyTopLevelReport extends AbstractTopLevelReportBase {

  private Package[] packages = new Package[0];

  // if set to true, will generate empty "basic" body of report
  private boolean generateEmpty = false;

  public DummyTopLevelReport(InsightsLogger logger, Map<String, InsightsSubreport> subReports) {
    super(logger, subReports);
  }

  @Override
  public long getProcessPID() {
    return 0;
  }

  @Override
  public Package[] getPackages() {
    return packages;
  }

  @Override
  public Map<String, Object> getBasic() {
    if (generateEmpty) {
      return Collections.emptyMap();
    }
    return super.getBasic();
  }

  @Override
  public void decorate(String key, String value) {}

  public static DummyTopLevelReport of(InsightsLogger logger) {
    return new DummyTopLevelReport(logger, Collections.emptyMap());
  }

  public Map<String, String> getNecessary() {
    return Collections.singletonMap("name", "unknown_app");
  }

  public void setGenerateEmpty(boolean generateEmpty) {
    this.generateEmpty = generateEmpty;
  }

  public void setPackages(Package[] packages) {
    this.packages = packages;
  }
}
