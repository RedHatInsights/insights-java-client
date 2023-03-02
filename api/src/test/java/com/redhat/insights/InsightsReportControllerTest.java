/* Copyright (C) Red Hat 2022-2023 */
package com.redhat.insights;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.redhat.insights.config.InsightsConfiguration;
import com.redhat.insights.doubles.DummyTopLevelReport;
import com.redhat.insights.doubles.MockInsightsConfiguration;
import com.redhat.insights.doubles.NoopInsightsHttpClient;
import com.redhat.insights.doubles.NoopInsightsLogger;
import com.redhat.insights.http.InsightsHttpClient;
import com.redhat.insights.logging.InsightsLogger;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * @author Emmanuel Hugonnet (c) 2022 Red Hat, Inc.
 */
public class InsightsReportControllerTest {

  public InsightsReportControllerTest() {}

  /** Test of generateAndSetReportIdHash method, of class InsightsReportController. */
  @Test
  public void testGenerateAndSetReportIdHash() {
    InsightsHttpClient httpClient = new NoopInsightsHttpClient();
    InsightsLogger logger = new NoopInsightsLogger();
    InsightsConfiguration config = MockInsightsConfiguration.ofOptedOut("test_app");
    AtomicInteger count = new AtomicInteger();
    InsightsReport report =
        new DummyTopLevelReport(logger, Collections.emptyMap()) {
          @Override
          public void setIdHash(String idHash) {
            count.incrementAndGet();
            super.setIdHash(idHash);
          }
        };
    // This should be done with a mock (or some sort of intercepting double), but I can't quite see
    // how
    //    InsightsReport report = mock(InsightsReport.class);
    //    when(report.getSerializer()).thenReturn(new InsightsReportSerializer());
    //    when(report.getSubreports()).thenReturn(Collections.emptyMap());
    //    when(report.getBasic()).thenReturn(Collections.singletonMap("test", "value"));
    InsightsReportController instance =
        InsightsReportController.of(logger, config, report, () -> httpClient);
    instance.generateAndSetReportIdHash();
    instance.generateAndSetReportIdHash();
    assertEquals(1, count.get());
    //    verify(report, times(1)).setIdHash(anyString());
  }
}
