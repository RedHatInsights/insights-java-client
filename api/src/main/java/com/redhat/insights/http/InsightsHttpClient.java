/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.http;

import com.redhat.insights.InsightsException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

/**
 * The main interface used for delivering Insights data (aka archives) to Red Hat. Note that the
 * interface name contains "Http" because the primary (and desired) transport is over HTTPS.
 * However, we need to also support a file-based mechanism - but only temporarily - so we don't warp
 * the interface name.
 */
public interface InsightsHttpClient {

  public static final String GENERAL_MIME_TYPE =
      "application/vnd.redhat.runtimes-java-general.analytics+tgz";

  /**
   * Send the report, which has not been gzipped.
   *
   * @param filename the name of the report.
   * @param report the report payload.
   */
  default void sendInsightsReport(String filename, String report) {
    sendInsightsReport(filename, gzipReport(report));
  }

  /**
   * Send the report, which has already been gzipped
   *
   * @param filename the name of the report.
   * @param gzipReport the gzip containing the report payload.
   */
  void sendInsightsReport(String filename, byte[] gzipReport);

  /**
   * Indicates if the HttpClient is ready to send the data.
   *
   * @return {@code true} when ready, {@code false} otherwise.
   */
  default boolean isReadyToSend() {
    return true;
  }

  static byte[] gzipReport(final String report) {
    try (final ByteArrayOutputStream baos = new ByteArrayOutputStream(report.length())) {
      final byte[] buffy = report.getBytes(StandardCharsets.UTF_8);

      final GZIPOutputStream gzip = new GZIPOutputStream(baos);
      gzip.write(buffy, 0, buffy.length);
      // An explicit close is necessary before we call toByteArray()
      gzip.close();

      return baos.toByteArray();
    } catch (IOException iox) {
      throw new InsightsException("Failed to GZIP report: " + report, iox);
    }
  }
}
