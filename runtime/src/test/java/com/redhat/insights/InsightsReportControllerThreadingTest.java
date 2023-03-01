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

  @Test
  public void testGenerateWith403() throws Exception {
    var logger = new NoopInsightsLogger(); // PrintLogger.STDOUT_LOGGER;
    var config =
        MockInsightsConfiguration.ofRetries(
            "test_app", 100L, DEFAULT_HTTP_CLIENT_RETRY_BACKOFF_FACTOR, 3);

    var mock = mock(HttpClient.class);
    HttpResponse<String> response =
        new HttpResponseBase() {
          @Override
          public int statusCode() {
            return 403;
          }
        };
    when(mock.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(response);
    var httpClient =
        new MockInsightsJdkHttpClient(config, () -> mock(SSLContext.class), logger, mock);

    var report = AppTopLevelReport.of(logger);
    var controller = InsightsReportController.of(logger, config, report, () -> httpClient);
    controller.generate();

    await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> controller.isShutdown());
  }
}
