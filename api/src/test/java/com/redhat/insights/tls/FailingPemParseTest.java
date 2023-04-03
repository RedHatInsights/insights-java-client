/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.tls;

import com.redhat.insights.doubles.DefaultConfiguration;
import com.redhat.insights.logging.PrintLogger;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

public class FailingPemParseTest {
  @Test
  public void failToWrite() {
    new PEMSupport(PrintLogger.STDOUT_LOGGER, new DefaultConfiguration())
        .createTLSContext(
            getPathFromResource("com/redhat/insights/tls/malformed/cert.pem"),
            getPathFromResource("com/redhat/insights/tls/malformed/key.pem"));
  }

  private Path getPathFromResource(String path) {
    return Paths.get(ClassLoader.getSystemClassLoader().getResource(path).getPath());
  }
}
