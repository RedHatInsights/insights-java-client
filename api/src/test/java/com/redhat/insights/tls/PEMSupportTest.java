/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.tls;

import static org.junit.jupiter.api.Assertions.*;

import com.redhat.insights.InsightsException;
import com.redhat.insights.doubles.DefaultConfiguration;
import com.redhat.insights.logging.TestLogger;
import java.io.IOException;
import java.nio.file.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PEMSupportTest {

  @Test
  public void testLoadingCertAndKey() {
    new PEMSupport(new TestLogger(), new DefaultConfiguration())
        .createTLSContext(
            getPathFromResource("com/redhat/insights/tls/dummy.cert"),
            getPathFromResource("com/redhat/insights/tls/dummy.key"));
  }

  @Test
  public void testNoErrorForMissingKey() {
    InsightsException exception =
        Assertions.assertThrows(
            InsightsException.class,
            () ->
                new PEMSupport(new TestLogger(), new DefaultConfiguration())
                    .createTLSContext(
                        getPathFromResource("com/redhat/insights/tls/dummy.cert"),
                        Paths.get("missing.key")));

    Assertions.assertEquals("I4ASR0015: SSLContext creation error", exception.getMessage());
    Assertions.assertTrue(exception.getCause() instanceof NoSuchFileException);
    Assertions.assertEquals("missing.key", exception.getCause().getMessage());
  }

  @Test
  public void testNoErrorForMissingCert() {
    InsightsException exception =
        Assertions.assertThrows(
            InsightsException.class,
            () ->
                new PEMSupport(new TestLogger(), new DefaultConfiguration())
                    .createTLSContext(
                        Paths.get("missing.cert"),
                        getPathFromResource("com/redhat/insights/tls/dummy.key")));

    Assertions.assertEquals("I4ASR0015: SSLContext creation error", exception.getMessage());
    Assertions.assertTrue(exception.getCause() instanceof NoSuchFileException);
    Assertions.assertEquals("missing.cert", exception.getCause().getMessage());
  }

  @Test
  public void testNoErrorForNonReadableKey() throws IOException {
    InsightsException exception =
        Assertions.assertThrows(
            InsightsException.class,
            () ->
                new PEMSupport(new TestLogger(), new DefaultConfiguration())
                    .createTLSContext(
                        getPathFromResource("com/redhat/insights/tls/dummy.cert"),
                        createNonReadableFile()));

    Assertions.assertEquals("I4ASR0015: SSLContext creation error", exception.getMessage());
    Assertions.assertTrue(exception.getCause() instanceof AccessDeniedException);
  }

  @Test
  public void testNoErrorForNonReadableCert() throws IOException {
    InsightsException exception =
        Assertions.assertThrows(
            InsightsException.class,
            () ->
                new PEMSupport(new TestLogger(), new DefaultConfiguration())
                    .createTLSContext(
                        createNonReadableFile(),
                        getPathFromResource("com/redhat/insights/tls/dummy.key")));

    Assertions.assertEquals("I4ASR0015: SSLContext creation error", exception.getMessage());
    Assertions.assertTrue(exception.getCause() instanceof AccessDeniedException);
  }

  private Path createNonReadableFile() throws IOException {
    Path nonReadableCert = Files.createTempFile(null, null);
    Assertions.assertTrue(nonReadableCert.toFile().setReadable(false));
    return nonReadableCert;
  }

  private Path getPathFromResource(String path) {
    return Paths.get(ClassLoader.getSystemClassLoader().getResource(path).getPath());
  }
}
