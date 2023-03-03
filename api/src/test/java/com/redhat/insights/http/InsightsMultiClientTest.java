/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.http;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.redhat.insights.InsightsException;
import com.redhat.insights.InsightsReport;
import com.redhat.insights.config.InsightsConfiguration;
import com.redhat.insights.doubles.NoopInsightsLogger;
import com.redhat.insights.logging.InsightsLogger;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class InsightsMultiClientTest {

  @Test
  void firstFailsAndSecondSucceeds() throws IOException {
    String filename = "yolo.txt";

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
    when(report.serialize()).thenReturn("foo");

    InsightsHttpClient failingClient = mock(InsightsHttpClient.class);
    doThrow(new InsightsException("Failing on purpose"))
        .when(failingClient)
        .sendInsightsReport(filename, report);

    InsightsHttpClient fileClient = new InsightsFileWritingClient(logger, cfg);

    InsightsMultiClient client =
        new InsightsMultiClient(logger, Arrays.asList(failingClient, fileClient));
    client.sendInsightsReport(filename, report);

    File[] files = tmpdir.toFile().listFiles();
    assertEquals(1, files.length);
    assertEquals(3, files[0].length());

    verify(report).decorate("client.exception", "Failing on purpose");

    // Cleanup
    Files.delete(files[0].toPath());
    Files.delete(tmpdir);
  }

  @Test
  void theyAllFail() throws IOException {
    String filename = "yolo.txt";

    InsightsLogger logger = new NoopInsightsLogger();

    InsightsReport report = mock(InsightsReport.class);
    when(report.serialize()).thenReturn("foo");

    InsightsHttpClient failingClient = mock(InsightsHttpClient.class);
    doThrow(new InsightsException("Failing on purpose"))
        .when(failingClient)
        .sendInsightsReport(filename, report);

    InsightsHttpClient alsoFailingClient = mock(InsightsHttpClient.class);
    doThrow(new InsightsException("Also failing on purpose"))
        .when(alsoFailingClient)
        .sendInsightsReport(filename, report);

    InsightsMultiClient client =
        new InsightsMultiClient(logger, Arrays.asList(failingClient, alsoFailingClient));
    InsightsException err =
        assertThrows(InsightsException.class, () -> client.sendInsightsReport(filename, report));

    assertTrue(err.getMessage().startsWith("I4ASR0018: All clients failed"));
    assertTrue(err.getMessage().contains("Mock for InsightsHttpClient"));
    assertTrue(err.getMessage().contains(", Mock for InsightsHttpClient"));
    verify(report).decorate("client.exception", "Failing on purpose");
  }
}
