/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.http;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.redhat.insights.InsightsException;
import com.redhat.insights.InsightsReport;
import com.redhat.insights.config.InsightsConfiguration;
import com.redhat.insights.doubles.NoopInsightsLogger;
import com.redhat.insights.logging.InsightsLogger;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

public class InsightsFileWritingClientTest {

  @Test
  public void testWriteToTmp() throws Exception {
    Path tmpdir = Files.createTempDirectory("tmpDirPrefix");
    InsightsConfiguration cfg =
        new InsightsConfiguration() {
          @Override
          public String getIdentificationName() {
            return "TEST";
          }

          @Override
          public String getArchiveUploadDir() {
            return tmpdir.toString();
          }
        };

    InsightsLogger logger = new NoopInsightsLogger();
    InsightsHttpClient client = new InsightsFileWritingClient(logger, cfg);
    InsightsReport report = mock(InsightsReport.class);
    when(report.serialize()).thenReturn("foo");

    client.sendInsightsReport("foo", report);
    File[] files = tmpdir.toFile().listFiles();
    assertEquals(1, files.length);
    assertEquals("foo.json", files[0].getName());
    assertEquals(3, files[0].length());
    // Cleanup
    Files.delete(files[0].toPath());
    Files.delete(tmpdir);
  }

  @Test
  public void testIsReadyToSend() throws IOException {
    Path tmpdir = Files.createTempDirectory("tmpDirPrefix");

    InsightsConfiguration goodConfig =
        new InsightsConfiguration() {
          @Override
          public String getIdentificationName() {
            return "GOOD";
          }

          @Override
          public String getArchiveUploadDir() {
            return tmpdir.resolve("inner-folder").toString();
          }

          @Override
          public String getMachineIdFilePath() {
            return getPathFromResource("com/redhat/insights/machine-id").toString();
          }
        };

    InsightsConfiguration wrongMachineIdConfig =
        new InsightsConfiguration() {
          @Override
          public String getIdentificationName() {
            return "GOOD";
          }

          @Override
          public String getArchiveUploadDir() {
            return tmpdir.toString();
          }

          @Override
          public String getMachineIdFilePath() {
            return "BAD";
          }
        };

    InsightsConfiguration wrongUploadPathConfig =
        new InsightsConfiguration() {
          @Override
          public String getIdentificationName() {
            return "GOOD";
          }

          @Override
          public String getArchiveUploadDir() {
            try {
              Path dir = Files.createTempDirectory("insights-tests");
              if (!dir.toFile().setWritable(false)) {
                throw new IllegalStateException("Cannot create a non-writable directory: " + dir);
              }
              return dir.resolve("should-not-be-created").toString();
            } catch (IOException e) {
              throw new IllegalStateException(e);
            }
          }

          @Override
          public String getMachineIdFilePath() {
            return "BAD";
          }
        };

    InsightsLogger logger = new NoopInsightsLogger();

    InsightsHttpClient client = new InsightsFileWritingClient(logger, goodConfig);
    assertTrue(client.isReadyToSend(), "Client should be ready to send");

    client = new InsightsFileWritingClient(logger, wrongMachineIdConfig);
    assertFalse(
        client.isReadyToSend(),
        "Client shouldn't be ready to send because of wrong machine-id path");

    InsightsException err =
        assertThrows(
            InsightsException.class,
            () -> new InsightsFileWritingClient(logger, wrongUploadPathConfig),
            "Constructor should throw because of non-creatable upload directory");
    assertTrue(err.getMessage().startsWith("I4ASR0024"));
  }

  private Path getPathFromResource(String path) {
    return Paths.get(ClassLoader.getSystemClassLoader().getResource(path).getPath());
  }
}
