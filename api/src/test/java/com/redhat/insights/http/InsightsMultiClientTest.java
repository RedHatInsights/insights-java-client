/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.http;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.redhat.insights.InsightsException;
import com.redhat.insights.InsightsReport;
import com.redhat.insights.config.InsightsConfiguration;
import com.redhat.insights.doubles.DummyTopLevelReport;
import com.redhat.insights.doubles.NoopInsightsLogger;
import com.redhat.insights.doubles.StoringInsightsHttpClient;
import com.redhat.insights.logging.InsightsLogger;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class InsightsMultiClientTest {

  @Test
  public void testBothClientsOperational() {
    StoringInsightsHttpClient client1 = new StoringInsightsHttpClient();
    StoringInsightsHttpClient client2 = new StoringInsightsHttpClient();
    InsightsLogger logger = new NoopInsightsLogger();
    InsightsReport report = DummyTopLevelReport.of(logger);
    InsightsMultiClient multiClient = new InsightsMultiClient(logger, client1, client2);

    multiClient.sendInsightsReport("", report);

    assertEquals(1, client1.getReportsSent(), "First client should send the report");
    assertEquals(0, client2.getReportsSent(), "Second client should not send the report");
  }

  @Test
  void firstFailsAndSecondSucceeds() throws IOException {
    String reportName = "yolo";

    Path tmpdir = Files.createTempDirectory("tmpDirPrefix");
    InsightsConfiguration cfg =
        new InsightsConfiguration() {
          @Override
          public String getIdentificationName() {
            return "TEST";
          }

          public String getArchiveUploadDir() {
            return tmpdir.toString();
          }
        };

    InsightsLogger logger = new NoopInsightsLogger();

    InsightsReport report = mock(InsightsReport.class);
    when(report.serializeRaw()).thenReturn("foo".getBytes(StandardCharsets.UTF_8));

    InsightsHttpClient failingClient = mock(InsightsHttpClient.class);
    doThrow(new InsightsException("Failing on purpose"))
        .when(failingClient)
        .sendInsightsReport(reportName, report);

    InsightsHttpClient fileClient = new InsightsFileWritingClient(logger, cfg);

    InsightsMultiClient client =
        new InsightsMultiClient(logger, Arrays.asList(failingClient, fileClient));
    client.sendInsightsReport(reportName, report);

    File[] files = tmpdir.toFile().listFiles();
    assertEquals(1, files.length);
    assertEquals(reportName + ".json", files[0].getName());
    assertEquals(3, files[0].length());

    verify(report).decorate("app.client.exception", "Failing on purpose");

    // Cleanup
    Files.delete(files[0].toPath());
    Files.delete(tmpdir);
  }

  @Test
  void theyAllFail() {
    String reportName = "yolo";

    InsightsLogger logger = new NoopInsightsLogger();

    InsightsReport report = mock(InsightsReport.class);
    when(report.serializeRaw()).thenReturn("foo".getBytes(StandardCharsets.UTF_8));

    InsightsHttpClient failingClient = mock(InsightsHttpClient.class);
    doThrow(new InsightsException("Failing on purpose"))
        .when(failingClient)
        .sendInsightsReport(reportName, report);

    InsightsHttpClient alsoFailingClient = mock(InsightsHttpClient.class);
    doThrow(new InsightsException("Also failing on purpose"))
        .when(alsoFailingClient)
        .sendInsightsReport(reportName, report);

    InsightsMultiClient client =
        new InsightsMultiClient(logger, Arrays.asList(failingClient, alsoFailingClient));
    InsightsException err =
        assertThrows(InsightsException.class, () -> client.sendInsightsReport(reportName, report));

    assertTrue(err.getMessage().startsWith("I4ASR0019: All clients failed"));
    assertTrue(err.getMessage().contains("Mock for InsightsHttpClient"));
    assertTrue(err.getMessage().contains(", Mock for InsightsHttpClient"));
    verify(report).decorate("app.client.exception", "Failing on purpose");
  }
}
