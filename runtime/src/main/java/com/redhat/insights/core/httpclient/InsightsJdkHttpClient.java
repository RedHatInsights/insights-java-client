/* Copyright (C) Red Hat 2023-2024 */
package com.redhat.insights.core.httpclient;

import static com.redhat.insights.InsightsErrorCode.*;

import com.redhat.insights.InsightsException;
import com.redhat.insights.config.InsightsConfiguration;
import com.redhat.insights.http.BackoffWrapper;
import com.redhat.insights.http.InsightsHttpClient;
import com.redhat.insights.logging.InsightsLogger;
import com.redhat.insights.reports.InsightsReport;
import java.io.File;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.Supplier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

/** An insights HTTP client based on the built-in JDK client. */
public class InsightsJdkHttpClient implements InsightsHttpClient {

  /*
   * Note: the JDK Http client is self-managed. You can't close the connection.
   * This means that the connection will eventually close after some time, and it might fail with a concurrent
   * key rotation.
   */
  private final InsightsConfiguration configuration;
  private final InsightsLogger logger;
  private final Supplier<SSLContext> sslContextSupplier;

  public InsightsJdkHttpClient(
      InsightsLogger logger,
      InsightsConfiguration configuration,
      Supplier<SSLContext> sslContextSupplier) {
    this.logger = logger;
    this.configuration = configuration;
    this.sslContextSupplier = sslContextSupplier;
  }

  public InsightsJdkHttpClient(InsightsLogger logger, InsightsConfiguration configuration) {
    this(
        logger,
        configuration,
        () -> {
          throw new InsightsException(
              ERROR_SSL_CREATING_CONTEXT, "Illegal attempt to create SSLContext for token auth");
        });
  }

  // Package-private for testing
  URI assembleURI(String url, String path) {
    String fullURL;
    if (!url.endsWith("/") && !path.startsWith("/")) {
      fullURL = url + "/" + path;
    } else if (url.endsWith("/") && path.startsWith("/")) {
      fullURL = url + path.substring(1);
    } else {
      fullURL = url + path;
    }
    return URI.create(fullURL);
  }

  HttpClient getHttpClient() {
    var clientBuilder = HttpClient.newBuilder();

    clientBuilder = clientBuilder.connectTimeout(configuration.getHttpClientTimeout());

    if (configuration.useMTLS()) {
      final var sslParameters = new SSLParameters();
      sslParameters.setWantClientAuth(true);
      clientBuilder = clientBuilder.sslParameters(sslParameters);
      final var tlsContext = sslContextSupplier.get();
      clientBuilder = clientBuilder.sslContext(tlsContext);
    }

    if (configuration.getProxyConfiguration().isPresent()) {
      final var conf = configuration.getProxyConfiguration().get();
      clientBuilder =
          clientBuilder.proxy(
              ProxySelector.of(new InetSocketAddress(conf.getHost(), conf.getPort())));
    }

    return clientBuilder.followRedirects(HttpClient.Redirect.NORMAL).build();
  }

  @Override
  public void decorate(InsightsReport report) {
    if (configuration.useMTLS()) {
      report.decorate("app.transport.type.https", "mtls");
      // We can't send anything more useful (e.g. SHA hash of cert file) as until
      // we try to send, we don't know if we can read the file at this path
      report.decorate("app.transport.cert.https", configuration.getCertFilePath());
    } else {
      final var authToken = configuration.getMaybeAuthToken().get();
      report.decorate("app.transport.type.https", "token");
      report.decorate("app.auth.token", authToken);
    }
  }

  @Override
  public void sendInsightsReport(String reportName, InsightsReport report) {
    decorate(report);
    final var client = getHttpClient();
    final var gzipJson = InsightsHttpClient.gzipReport(report.serializeRaw());
    sendInsightsReportWithClient(client, reportName + ".gz", gzipJson);
  }

  protected void sendInsightsReportWithClient(
      HttpClient client, String filename, byte[] gzipReport) {
    var bodyBuilder =
        new MultipartBodyBuilder()
            .addFile("file", filename, GENERAL_MIME_TYPE, gzipReport)
            .addFormData("type", GENERAL_MIME_TYPE)
            .end();

    var body = bodyBuilder.bodyPublisher();

    var requestBuilder =
        HttpRequest.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .header(MultipartBodyBuilder.CONTENT_TYPE_HEADER, bodyBuilder.contentTypeHeaderValue())
            .timeout(configuration.getHttpClientTimeout());

    if (!configuration.useMTLS()) {
      final var authToken = configuration.getMaybeAuthToken().get();
      requestBuilder = requestBuilder.setHeader("Authorization", "Bearer " + authToken);
    }
    requestBuilder =
        requestBuilder.uri(
            assembleURI(configuration.getUploadBaseURL(), configuration.getUploadUri()));

    var request = requestBuilder.POST(body).build();
    logger.debug("Issuing a HTTP POST request to " + request);

    var wrapper =
        new BackoffWrapper(
            logger,
            configuration,
            () -> {
              var response = client.send(request, HttpResponse.BodyHandlers.ofString());
              int statusCode = response.statusCode();
              logger.debug(
                  "Red Hat Insights HTTP Client: status="
                      + statusCode
                      + ", body="
                      + response.body());
              switch (statusCode / 100) {
                case 2:
                  if (statusCode == 201) {
                    logger.debug(
                        "Red Hat Insights - Advisor content type with no metadata accepted for"
                            + " processing");
                  } else {
                    logger.debug("Red Hat Insights - Payload was accepted for processing");
                  }
                  break;
                case 4:
                  switch (statusCode) {
                    case 401:
                      throw new InsightsException(
                          ERROR_HTTP_SEND_AUTH_ERROR, "Authentication missing from request");
                    case 403:
                      throw new InsightsException(ERROR_HTTP_SEND_FORBIDDEN, "Forbidden");
                    case 413:
                      throw new InsightsException(ERROR_HTTP_SEND_PAYLOAD, "Payload too large");
                    case 415:
                      throw new InsightsException(
                          ERROR_HTTP_SEND_INVALID_CONTENT_TYPE,
                          "Content type of payload is unsupported");
                    default:
                      throw new InsightsException(
                          ERROR_HTTP_SEND_CLIENT_ERROR,
                          "Client error with HTTP status code " + statusCode);
                  }
                case 5:
                default:
                  throw new InsightsException(
                      ERROR_HTTP_SEND_SERVER_ERROR,
                      "Request failed on the server with code: " + statusCode);
              }
            });
    try {
      wrapper.run();
    } catch (InsightsException isx) {
      throw isx;
    } catch (Throwable err) {
      throw new InsightsException(ERROR_HTTP_SEND_, "HTTP client request failed", err);
    }
  }

  @Override
  public boolean isReadyToSend() {
    return !configuration.useMTLS()
        || (new File(configuration.getCertFilePath()).exists()
            && new File(configuration.getKeyFilePath()).exists()
            && new File(configuration.getMachineIdFilePath()).exists());
  }
}
