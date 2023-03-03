/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.core.agent;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.redhat.insights.InsightsCustomScheduledExecutor;
import com.redhat.insights.InsightsReport;
import com.redhat.insights.InsightsReportController;
import com.redhat.insights.core.app.AppTopLevelReport;
import com.redhat.insights.doubles.MockInsightsConfiguration;
import com.redhat.insights.doubles.NoopInsightsLogger;
import com.redhat.insights.http.InsightsHttpClient;
import com.redhat.insights.jars.JarInfo;
import java.lang.instrument.Instrumentation;
import java.time.Duration;
import java.util.concurrent.LinkedBlockingQueue;
import net.bytebuddy.agent.ByteBuddyAgent;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ClassNoticerTest {

  @Test
  public void testNoticer() throws InterruptedException {
    // Setup Byte Buddy agent
    var instrumentation = ByteBuddyAgent.install();
    assertInstanceOf(Instrumentation.class, instrumentation);

    var logger = new NoopInsightsLogger();
    var jarsToSend = new LinkedBlockingQueue<JarInfo>();
    var noticer = new ClassNoticer(logger, jarsToSend);
    instrumentation.addTransformer(noticer);

    var mockConfig =
        MockInsightsConfiguration.of("test_app", false, Duration.ofDays(1), Duration.ofSeconds(5));
    var mockHttpClient = Mockito.mock(InsightsHttpClient.class);
    when(mockHttpClient.isReadyToSend()).thenReturn(true);
    var report = AppTopLevelReport.of(logger, mockConfig);
    var scheduler = InsightsCustomScheduledExecutor.of(logger, mockConfig);

    var controller =
        InsightsReportController.of(
            logger, mockConfig, report, () -> mockHttpClient, scheduler, jarsToSend);
    controller.generate();

    await()
        .atMost(Duration.ofSeconds(20))
        .untilAsserted(
            () ->
                Mockito.verify(mockHttpClient, times(2))
                    .sendInsightsReport(any(), (InsightsReport) any()));
  }
}
