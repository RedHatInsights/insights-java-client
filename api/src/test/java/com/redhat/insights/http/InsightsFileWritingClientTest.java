/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.http;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.redhat.insights.InsightsReport;
import com.redhat.insights.config.InsightsConfiguration;
import com.redhat.insights.doubles.NoopInsightsLogger;
import com.redhat.insights.logging.InsightsLogger;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
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

          public String getArchiveUploadDir() {
            return tmpdir.toString();
          }
        };

    InsightsLogger logger = new NoopInsightsLogger();
    InsightsHttpClient client = new InsightsFileWritingClient(logger, cfg);
    InsightsReport report = mock(InsightsReport.class);
    when(report.serialize()).thenReturn("foo");

    client.sendInsightsReport("foo.txt", report);
    File[] files = tmpdir.toFile().listFiles();
    assertEquals(1, files.length);
    assertEquals(3, files[0].length());
    // Cleanup
    Files.delete(files[0].toPath());
    Files.delete(tmpdir);
  }
}
