/* Copyright (C) Red Hat 2023-2024 */
package com.redhat.insights.http;

import static com.redhat.insights.InsightsErrorCode.ERROR_UPLOAD_DIR_CREATION;
import static com.redhat.insights.InsightsErrorCode.ERROR_WRITING_FILE;

import com.redhat.insights.InsightsException;
import com.redhat.insights.InsightsReport;
import com.redhat.insights.config.InsightsConfiguration;
import com.redhat.insights.logging.InsightsLogger;
import java.io.File;
import java.io.IOException;
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
    ensureArchiveUploadDirExists();
  }

  private void ensureArchiveUploadDirExists() {
    Path dir = Paths.get(config.getArchiveUploadDir());
    if (Files.notExists(dir) && !config.isOptingOut()) {
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
    if (config.isOptingOut()) {
      return;
    }
    decorate(report);

    // Can't reuse upload path - as this may be called as part of fallback
    Path p = Paths.get(config.getArchiveUploadDir(), filename + ".json");
    try {
      Files.write(
          p,
          report.serializeRaw(),
          StandardOpenOption.WRITE,
          StandardOpenOption.CREATE,
          StandardOpenOption.TRUNCATE_EXISTING);
    } catch (IOException iox) {
      throw new InsightsException(ERROR_WRITING_FILE, "Could not write to: " + p, iox);
    }
  }

  @Override
  public boolean isReadyToSend() {
    return new File(config.getMachineIdFilePath()).exists();
  }
}
