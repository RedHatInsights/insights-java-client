/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.http;

import com.redhat.insights.InsightsException;
import com.redhat.insights.config.InsightsConfiguration;
import com.redhat.insights.logging.InsightsLogger;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class InsightsFileWritingClient implements InsightsHttpClient {
  private final InsightsLogger logger;
  private final InsightsConfiguration config;

  public InsightsFileWritingClient(InsightsLogger logger, InsightsConfiguration config) {
    this.logger = logger;
    this.config = config;
  }

  @Override
  public void sendInsightsReport(String filename, String report) {
    // Can't reuse upload path - as this may be called as part of fallback
    Path p = Paths.get(config.getArchiveUploadDir(), filename);
    try {
      Files.write(
          p,
          report.getBytes(StandardCharsets.UTF_8),
          StandardOpenOption.WRITE,
          StandardOpenOption.CREATE);
    } catch (IOException iox) {
      throw new InsightsException("Could not write to: " + p, iox);
    }
  }

  @Override
  public void sendInsightsReport(String filename, byte[] gzipReport) {
    throw new InsightsException(
        "Unsupported operation attempted on InsightsFileWritingClient: " + filename);
  }
}
