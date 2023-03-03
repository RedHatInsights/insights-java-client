/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.tls;

import com.redhat.insights.config.InsightsConfiguration;
import com.redhat.insights.logging.InsightsLogger;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class CertHelper {
  private final InsightsLogger logger;
  private final InsightsConfiguration configuration;

  public CertHelper(InsightsLogger logger, InsightsConfiguration configuration) {
    this.logger = logger;
    this.configuration = configuration;
  }

  public byte[] readUsingHelper(String mode) throws IOException, InterruptedException {
    final ProcessBuilder builder = new ProcessBuilder();
    builder.command(configuration.getCertHelperBinary(), mode);
    builder.directory(new File(System.getProperty("user.home")));
    final Process process = builder.start();
    final StringBuilder sb = new StringBuilder();
    final CertStreamHandler handler =
        new CertStreamHandler(process.getInputStream(), l -> sb.append(l));
    final ExecutorService service = Executors.newSingleThreadExecutor();
    Future<?> future = service.submit(handler);
    final InsightsHelperStatus exitCode = InsightsHelperStatus.fromExitCode(process.waitFor());
    service.shutdown();
    try {
      // We don't care about the return of the future - only that the task exited
      future.get();
    } catch (ExecutionException e) {
      throw new IOException("Helper subprocess execution failed", e);
    }
    if (InsightsHelperStatus.OK.equals(exitCode)) {
      return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
    String msg =
        "Couldn't use helper. Sub-process returned: "
            + exitCode.getCode()
            + " ; "
            + exitCode.getMessage();
    // Should this be an InsightsException ?
    throw new IOException(msg);
  }
}
