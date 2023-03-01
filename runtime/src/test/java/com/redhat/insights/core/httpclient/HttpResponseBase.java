/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.core.httpclient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import javax.net.ssl.SSLSession;

public class HttpResponseBase implements HttpResponse<String> {

  @Override
  public int statusCode() {
    return 0;
  }

  @Override
  public HttpRequest request() {
    return null;
  }

  @Override
  public Optional<HttpResponse<String>> previousResponse() {
    return Optional.empty();
  }

  @Override
  public HttpHeaders headers() {
    return null;
  }

  @Override
  public String body() {
    return null;
  }

  @Override
  public Optional<SSLSession> sslSession() {
    return Optional.empty();
  }

  @Override
  public URI uri() {
    return null;
  }

  @Override
  public HttpClient.Version version() {
    return null;
  }
}
