/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.it;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.redhat.insights.InsightsReportController;
import com.redhat.insights.config.EnvAndSysPropsInsightsConfiguration;
import com.redhat.insights.core.app.AppInsightsSubreport;
import com.redhat.insights.core.app.AppTopLevelReport;
import com.redhat.insights.core.httpclient.InsightsJdkHttpClient;
import com.redhat.insights.jars.ClasspathJarInfoSubreport;
import com.redhat.insights.logging.TestLogger;
import com.redhat.insights.tls.PEMSupport;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Run integration tests against an ephemeral environment")
class EphemeralEnvironmentTest {

  @Test
  @DisplayName("Send a sample application insights")
  void sendSampleApplicationInsights() throws Exception {
    assumeTrue(
        System.getenv("RHT_INSIGHTS_JAVA_AUTH_TOKEN") != null,
        "RHT_INSIGHTS_JAVA_AUTH_TOKEN is defined for ephemeral environments");
    assumeTrue(
        System.getenv("RHT_INSIGHTS_JAVA_UPLOAD_BASE_URL") != null,
        "RHT_INSIGHTS_JAVA_UPLOAD_BASE_URL is defined for ephemeral environments");

    var logger = new TestLogger();
    var report =
        new AppTopLevelReport(
            logger,
            Map.of(
                "jars",
                new ClasspathJarInfoSubreport(logger),
                "details",
                new AppInsightsSubreport()));
    var config = new EnvAndSysPropsInsightsConfiguration();
    var pem = new PEMSupport(logger, config);

    var controller =
        InsightsReportController.of(
            logger,
            config,
            report,
            () ->
                new InsightsJdkHttpClient(
                    logger,
                    config,
                    () ->
                        pem.createTLSContext(
                            Path.of(
                                "..",
                                "api",
                                "src/test/resources/com/redhat/insights/tls/dummy.cert"),
                            Path.of(
                                "..",
                                "api",
                                "src/test/resources/com/redhat/insights/tls/dummy.key"))));

    controller.generate();

    await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(() -> assertFalse(findSuccessLogEntry(logger).isEmpty()));
  }

  private static Optional<TestLogger.TestLogRecord> findSuccessLogEntry(TestLogger logger) {
    List<TestLogger.TestLogRecord> copy = new ArrayList<>(logger.getLogs());
    return copy.stream()
        .filter(
            record ->
                "Red Hat Insights - Payload was accepted for processing"
                    .equals(record.getMessage()))
        .findFirst();
  }
}
