/* Copyright (C) Red Hat 2023-2024 */
package com.redhat.insights.http;

import static com.redhat.insights.InsightsErrorCode.ERROR_GZIP_FILE;

import com.redhat.insights.InsightsException;
import com.redhat.insights.reports.InsightsReport;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.GZIPOutputStream;
import org.jspecify.annotations.NullMarked;

/**
 * The main interface used for delivering Insights data (aka archives) to Red Hat. Note that the
 * interface name contains "Http" because the primary (and desired) transport is over HTTPS.
 * However, we need to also support a file-based mechanism - but only temporarily - so we don't warp
 * the interface name.
 */
@NullMarked
public interface InsightsHttpClient {

  public static final String GENERAL_MIME_TYPE =
      "application/vnd.redhat.runtimes-java-general.analytics+tgz";

  /**
   * Decorates the Insights report with any additional metadata that the client wants sent. This is
   * a mandatory method, rather than optional, because client implementors should consider what data
   * they need to convey to the Insights services.
   *
   * @param report
   */
  void decorate(InsightsReport report);

  /**
   * Send the report, which has not been gzipped.
   *
   * @param reportName the name of the report.
   * @param report the report payload.
   */
  void sendInsightsReport(String reportName, InsightsReport report);

  /**
   * Indicates if the HttpClient is ready to send the data.
   *
   * @return {@code true} when ready, {@code false} otherwise.
   */
  default boolean isReadyToSend() {
    return true;
  }

  /**
   * Static gzip helper method
   *
   * @param report
   * @return gzipped bytes
   * @deprecated use #gzipReport(final byte[] report)
   */
  static byte[] gzipReport(final String report) {
    return gzipReport(report.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Static gzip helper method
   *
   * @param report
   * @return gzipped bytes
   */
  static byte[] gzipReport(final byte[] report) {
    try (final ByteArrayOutputStream baos = new ByteArrayOutputStream(report.length)) {
      final GZIPOutputStream gzip = new GZIPOutputStream(baos);
      gzip.write(report);
      // An explicit close is necessary before we call toByteArray()
      gzip.close();
      return baos.toByteArray();
    } catch (IOException iox) {
      throw new InsightsException(
          ERROR_GZIP_FILE, "Failed to GZIP report: " + Arrays.toString(report), iox);
    }
  }
}
