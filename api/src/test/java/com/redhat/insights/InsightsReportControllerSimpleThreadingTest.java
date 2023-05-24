/* Copyright (C) Red Hat 2023 */
package com.redhat.insights;

import static com.redhat.insights.http.InsightsHttpClient.gzipReport;
import static com.redhat.insights.jars.JarUtils.computeSha512;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

import com.redhat.insights.config.InsightsConfiguration;
import com.redhat.insights.doubles.DummyTopLevelReport;
import com.redhat.insights.doubles.MockInsightsConfiguration;
import com.redhat.insights.doubles.NoopInsightsHttpClient;
import com.redhat.insights.doubles.NoopInsightsLogger;
import com.redhat.insights.logging.InsightsLogger;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

public class InsightsReportControllerSimpleThreadingTest {

  @Test
  public void optedoutExitsWhenRunOnInsightsExecutor() throws InterruptedException {
    InsightsLogger logger = new NoopInsightsLogger(); // PrintLogger.STDOUT_LOGGER;
    InsightsConfiguration config = MockInsightsConfiguration.ofOptedOut("test_app");

    InsightsReportController controller =
        InsightsReportController.of(
            logger, config, DummyTopLevelReport.of(logger), () -> new NoopInsightsHttpClient());

    InsightsException thrown =
        assertThrows(
            InsightsException.class,
            () -> controller.generate(),
            "Expected generate() to throw, but it didn't");
    assertEquals("I4ASR0001: Opting out of the Red Hat Insights client", thrown.getMessage());

    InsightsScheduler tp = controller.getScheduler();
    await().atMost(Duration.ofSeconds(10)).untilAsserted(tp::isShutdown);
  }

  @Test
  public void testJarFingerprinting() throws NoSuchAlgorithmException, IOException {
    InsightsLogger logger = new NoopInsightsLogger();
    InsightsConfiguration config = MockInsightsConfiguration.ofOptedOut("test_app");

    DummyTopLevelReport report = DummyTopLevelReport.of(logger);
    InsightsReportController controller =
        InsightsReportController.of(logger, config, report, () -> new NoopInsightsHttpClient());
    report.generateReport(Filtering.DEFAULT);

    // First, compute the SHA 512 fingerprint without the id hash
    String initialReportJson = report.serialize();
    final byte[] initialGz = gzipReport(initialReportJson);
    final String hash = computeSha512(initialGz);

    // Now generate the hash
    controller.generateAndSetReportIdHash();

    // These are equal because the controllers identifying hash (the "sending hash") has to be
    // computed *before* the sending hash is in the report
    assertEquals(controller.getIdHash(), hash);

    // Reserialize with the hash
    final String reportJson = report.serialize();
    final byte[] finalGz = gzipReport(reportJson);

    assertNotEquals(initialReportJson, reportJson);

    // Sanity check
    String initialSha512 = computeSha512(initialGz);
    String finalSha512 = computeSha512(finalGz);
    assertNotEquals(initialSha512, finalSha512);
  }
}
