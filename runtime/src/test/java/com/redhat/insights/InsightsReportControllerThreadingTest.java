/* Copyright (C) Red Hat 2023 */
package com.redhat.insights;

import static com.redhat.insights.config.InsightsConfiguration.DEFAULT_HTTP_CLIENT_RETRY_BACKOFF_FACTOR;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.redhat.insights.core.app.AppTopLevelReport;
import com.redhat.insights.core.httpclient.HttpResponseBase;
import com.redhat.insights.core.httpclient.MockInsightsJdkHttpClient;
import com.redhat.insights.doubles.MockInsightsConfiguration;
import com.redhat.insights.doubles.NoopInsightsLogger;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Duration;
import javax.net.ssl.SSLContext;
import org.junit.jupiter.api.Test;

public class InsightsReportControllerThreadingTest {

  private static HttpResponse<String> makeResponder(int code) {
    return new HttpResponseBase() {
      @Override
      public int statusCode() {
        return code;
      }
    };
  }

  private static InsightsReportController makeControllerWithCode(int code) throws Exception {
    var logger = new NoopInsightsLogger(); // PrintLogger.STDOUT_LOGGER;
    var config =
        MockInsightsConfiguration.ofRetries(
            "test_app", 100L, DEFAULT_HTTP_CLIENT_RETRY_BACKOFF_FACTOR, 3);

    var mock = mock(HttpClient.class);
    HttpResponse<String> response = makeResponder(code);

    when(mock.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(response);
    var httpClient =
        new MockInsightsJdkHttpClient(config, () -> mock(SSLContext.class), logger, mock);

    var report = AppTopLevelReport.of(logger, config);
    return InsightsReportController.of(logger, config, report, () -> httpClient);
  }

  private static void httpCodeCausesShutdown(int code) throws Exception {
    var controller = makeControllerWithCode(code);
    controller.generate();

    await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> controller.isShutdown());
  }

  @Test
  public void testGenerateWith401() throws Exception {
    httpCodeCausesShutdown(401);
  }

  @Test
  public void testGenerateWith413() throws Exception {
    httpCodeCausesShutdown(413);
  }

  @Test
  public void testGenerateWith415() throws Exception {
    httpCodeCausesShutdown(415);
  }

  @Test
  public void testGenerateWith500() throws Exception {
    httpCodeCausesShutdown(500);
  }

  @Test
  public void testGenerateWith503() throws Exception {
    httpCodeCausesShutdown(503);
  }

  @Test
  public void testGenerateWith201() throws Exception {
    var controller = makeControllerWithCode(201);

    Runnable r = () -> controller.generate();
    Thread t = new Thread(r);
    t.start();
    t.join();
    await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> controller.isShutdown());
  }

  @Test
  public void testGenerateWith202() throws Exception {
    var controller = makeControllerWithCode(202);

    Runnable r = () -> controller.generate();
    Thread t = new Thread(r);
    t.start();
    t.join();
    await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> controller.isShutdown());
  }
}
