/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.http;

import static com.redhat.insights.InsightsErrorCode.ERROR_UPLOAD_DIR_CREATION;

import com.redhat.insights.InsightsException;
import com.redhat.insights.InsightsReport;
import com.redhat.insights.config.InsightsConfiguration;
import com.redhat.insights.logging.InsightsLogger;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class InsightsFileWritingClient implements InsightsHttpClient {
  private final InsightsLogger logger;
  private final InsightsConfiguration config;

  public InsightsFileWritingClient(InsightsLogger logger, InsightsConfiguration config) {
    this.logger = logger;
    this.config = config;
    ensureArchiveUploadDirExists();
  }

  private void ensureArchiveUploadDirExists() {
    Path dir = Paths.get(config.getArchiveUploadDir());
    if (Files.notExists(dir)) {
      try {
        Files.createDirectories(dir);
      } catch (IOException e) {
        throw new InsightsException(
            ERROR_UPLOAD_DIR_CREATION, "Could not create directories for path " + dir);
      }
    }
  }

  @Override
  public void decorate(InsightsReport report) {
    report.decorate("app.transport.type.file", "rhel");
  }

  @Override
  public void sendInsightsReport(String filename, InsightsReport report) {
    decorate(report);

    // Can't reuse upload path - as this may be called as part of fallback
    Path p = Paths.get(config.getArchiveUploadDir(), filename + ".json");
    report.serialize(p.toFile());
  }

  @Override
  public boolean isReadyToSend() {
    return new File(config.getMachineIdFilePath()).exists();
  }
}
