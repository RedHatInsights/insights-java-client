/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.core.httpclient;

import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A multipart body builder to use from {@link InsightsJdkHttpClient}, because the JDK HTTP client
 * APIs do not provide such a basic functionality.
 */
public final class MultipartBodyBuilder {

  public static final String CONTENT_TYPE_HEADER = "Content-Type";

  /*
   * Note: this class does some input sanitization, but it is not bullet-proof and shall not be used outside
   * of tightly-controlled environments.
   */

  private final List<byte[]> parts = new ArrayList<>();
  private final String boundary = UUID.randomUUID().toString();

  private static final String CR_LF = "\r\n";

  public String contentTypeHeaderValue() {
    return "multipart/form-data; boundary=" + boundary;
  }

  private String sanitize(String input) {
    // See
    // https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#multipart-form-data
    String str = input.replace(Character.toString(0x0A), "%0A");
    str = str.replace(Character.toString(0x0D), "%0D");
    str = str.replace(Character.toString(0x22), "%22");
    return str;
  }

  private void boundaryLine(StringBuilder builder) {
    builder.append("--").append(boundary).append(CR_LF);
  }

  public MultipartBodyBuilder addFormData(String name, String value) {
    StringBuilder builder = new StringBuilder();

    boundaryLine(builder);
    builder
        .append("Content-Disposition: form-data; name=\"")
        .append(sanitize(name))
        .append("\"")
        .append(CR_LF);
    builder.append(CR_LF);
    builder.append(value).append(CR_LF);

    parts.add(builder.toString().getBytes(StandardCharsets.UTF_8));
    return this;
  }

  public MultipartBodyBuilder addFile(
      String name, String filename, String contentType, byte[] data) {
    StringBuilder builder = new StringBuilder();

    boundaryLine(builder);
    builder
        .append("Content-Disposition: form-data; name=\"")
        .append(sanitize(name))
        .append("\"; filename=\"")
        .append(sanitize(filename))
        .append("\"")
        .append(CR_LF);
    builder.append(CONTENT_TYPE_HEADER).append(": ").append(sanitize(contentType)).append(CR_LF);
    builder.append(CR_LF);
    parts.add(builder.toString().getBytes(StandardCharsets.UTF_8));

    parts.add(data);
    parts.add(CR_LF.getBytes(StandardCharsets.UTF_8));

    return this;
  }

  public MultipartBodyBuilder end() {
    StringBuilder builder = new StringBuilder();
    builder.append("--").append(boundary).append("--").append(CR_LF);
    parts.add(builder.toString().getBytes(StandardCharsets.UTF_8));
    return this;
  }

  public HttpRequest.BodyPublisher bodyPublisher() {
    return HttpRequest.BodyPublishers.ofByteArrays(parts);
  }
}
