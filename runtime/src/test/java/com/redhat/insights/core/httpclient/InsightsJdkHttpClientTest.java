/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.core.httpclient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.redhat.insights.InsightsReport;
import com.redhat.insights.config.InsightsConfiguration;
import com.redhat.insights.core.app.AppTopLevelReport;
import com.redhat.insights.doubles.DummyTopLevelReport;
import com.redhat.insights.doubles.MockInsightsConfiguration;
import com.redhat.insights.doubles.NoopInsightsLogger;
import com.redhat.insights.logging.InsightsLogger;
import com.redhat.insights.tls.PEMSupport;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLContext;
import org.junit.jupiter.api.Test;

public class InsightsJdkHttpClientTest {
  private static final InsightsLogger logger = new NoopInsightsLogger();

  @Test
  public void testHttpClientWithMTLS() {
    // default configuration provides no token => client should use MTLS
    InsightsConfiguration config = MockInsightsConfiguration.of("yolo", false);
    InsightsJdkHttpClient insightsClient =
        new InsightsJdkHttpClient(logger, config, () -> mock(SSLContext.class));
    HttpClient httpClient = insightsClient.getHttpClient();

    assertTrue(
        httpClient.sslParameters().getWantClientAuth(), "Client should want clientAuth with MTLS");
  }

  @Test
  public void testHttpClientWithoutMTLS() {
    InsightsConfiguration config = mock(InsightsConfiguration.class);
    when(config.getMaybeAuthToken()).thenReturn(Optional.of("randomToken"));
    when(config.getHttpClientTimeout()).thenReturn(Duration.ofSeconds(30));

    InsightsJdkHttpClient insightsClient = new InsightsJdkHttpClient(logger, config);
    HttpClient httpClient = insightsClient.getHttpClient();

    assertFalse(
        httpClient.sslParameters().getWantClientAuth(),
        "Client should not want SSL client auth, while having token");
  }

  @Test
  public void testHttpClientWithProxy() {
    InsightsConfiguration config = mock(InsightsConfiguration.class);
    when(config.getProxyConfiguration())
        .thenReturn(Optional.of(new InsightsConfiguration.ProxyConfiguration("localhost", 8080)));
    when(config.getHttpClientTimeout()).thenReturn(Duration.ofSeconds(30));

    InsightsJdkHttpClient insightsClient = new InsightsJdkHttpClient(logger, config);
    HttpClient httpClient = insightsClient.getHttpClient();

    // check a proxy is there
    assertTrue(httpClient.proxy().isPresent(), "Proxy should be present");
    // get the proxy
    List<Proxy> proxyList = httpClient.proxy().get().select(URI.create("https://randomSite.com"));

    assertEquals(1, proxyList.size(), "There should be one proxy configured");
    assertTrue(
        proxyList.get(0).address() instanceof InetSocketAddress,
        "Proxy address should be InetSocketAddress");
    InetSocketAddress proxyAddress = (InetSocketAddress) proxyList.get(0).address();

    assertEquals("localhost", proxyAddress.getHostName(), "Proxy hostname should match");
    assertEquals(8080, proxyAddress.getPort(), "Proxy port should match");
  }

  @Test
  public void testDecorateWithMTLS() {
    InsightsConfiguration config = MockInsightsConfiguration.of("yolo", false);
    InsightsJdkHttpClient insightsClient =
        new InsightsJdkHttpClient(logger, config, () -> mock(SSLContext.class));

    DummyTopLevelReport report = new DummyTopLevelReport(logger, Collections.emptyMap());
    insightsClient.decorate(report);

    // check decorations
    Map<String, String> decorations = report.getDecorations();
    assertEquals(2, decorations.size(), "There should be 2 decorations");

    assertTrue(
        decorations.containsKey("app.transport.cert.https"),
        "There should be key app.transport.cert.https");
    assertTrue(
        decorations.containsKey("app.transport.type.https"),
        "There should be key app.transport.type.https");
    assertEquals(
        "mtls",
        decorations.get("app.transport.type.https"),
        "Transport type decoration should be mtls");
  }

  @Test
  public void testDecorateWithoutMTLS() {
    InsightsConfiguration config = mock(InsightsConfiguration.class);
    when(config.getMaybeAuthToken()).thenReturn(Optional.of("randomToken"));

    InsightsJdkHttpClient insightsClient = new InsightsJdkHttpClient(logger, config);
    DummyTopLevelReport report = new DummyTopLevelReport(logger, Collections.emptyMap());
    insightsClient.decorate(report);

    // check decorations
    Map<String, String> decorations = report.getDecorations();
    assertEquals(2, decorations.size(), "There should be 2 decorations");

    assertTrue(
        decorations.containsKey("app.transport.type.https"),
        "There should be key app.transport.type.https");
    assertEquals(
        "token",
        decorations.get("app.transport.type.https"),
        "Transport type decoration should be token");

    assertTrue(decorations.containsKey("app.auth.token"), "There should be key app.auth.token");
    assertEquals(
        "randomToken", decorations.get("app.auth.token"), "Token should have the set value");
  }

  @Test
  public void testAssembleURI() {
    InsightsConfiguration config = MockInsightsConfiguration.of("yolo", false);
    InsightsJdkHttpClient insightsClient =
        new InsightsJdkHttpClient(logger, config, () -> mock(SSLContext.class));

    // check missing slash
    assertEquals(
        URI.create("https://site.com/myPath"),
        insightsClient.assembleURI("https://site.com", "myPath"));
    // check double slash
    assertEquals(
        URI.create("https://site.com/myPath"),
        insightsClient.assembleURI("https://site.com/", "/myPath"));
    // check first slash
    assertEquals(
        URI.create("https://site.com/myPath"),
        insightsClient.assembleURI("https://site.com/", "myPath"));
    // check second slash
    assertEquals(
        URI.create("https://site.com/myPath"),
        insightsClient.assembleURI("https://site.com", "/myPath"));
  }

  @Test
  public void testSendReportWithMtls() throws IOException, InterruptedException {
    InsightsConfiguration config = MockInsightsConfiguration.of("yolo", false);
    HttpClient httpClient = mock(HttpClient.class);
    InsightsJdkHttpClient insightsClient =
        new MockInsightsJdkHttpClient(logger, config, httpClient);

    AtomicReference<HttpRequest> request = new AtomicReference<>();

    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenAnswer(
            invocation -> {
              request.set(invocation.getArgument(0));
              return getOKResponse();
            });

    InsightsReport report = AppTopLevelReport.of(logger, config);
    insightsClient.sendInsightsReport("filename", report);

    // if using MTLS, there should be no authorization header
    assertEquals(0, request.get().headers().allValues("Authorization").size());
  }

  @Test
  public void testSendReportWithoutMtls() throws IOException, InterruptedException {
    InsightsConfiguration config = mock(InsightsConfiguration.class);
    when(config.getMaybeAuthToken()).thenReturn(Optional.of("randomToken"));
    when(config.getUploadBaseURL()).thenReturn("https://site.com");
    when(config.getUploadUri()).thenReturn("/path");
    when(config.getHttpClientTimeout()).thenReturn(Duration.ofSeconds(30));

    HttpClient httpClient = mock(HttpClient.class);
    PEMSupport pem = new PEMSupport(logger, config);
    InsightsJdkHttpClient insightsClient =
        new MockInsightsJdkHttpClient(logger, config, () -> pem.createTLSContext(), httpClient);

    AtomicReference<HttpRequest> request = new AtomicReference<>();

    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenAnswer(
            invocation -> {
              request.set(invocation.getArgument(0));
              return getOKResponse();
            });

    InsightsReport report = AppTopLevelReport.of(logger, config);
    insightsClient.sendInsightsReport("filename", report);

    // if using token, there should be authorization header
    assertEquals(1, request.get().headers().allValues("Authorization").size());
    assertEquals("Basic randomToken", request.get().headers().allValues("Authorization").get(0));
  }

  @Test
  public void testIsReadyToSendMTLS() {
    InsightsConfiguration config =
        new InsightsConfiguration() {
          @Override
          public String getIdentificationName() {
            return "GOOD";
          }

          @Override
          public String getKeyFilePath() {
            return getPathFromResource("com/redhat/insights/tls/dummy.key").toString();
          }

          @Override
          public String getCertFilePath() {
            return getPathFromResource("com/redhat/insights/tls/dummy.cert").toString();
          }

          @Override
          public Optional<String> getMaybeAuthToken() {
            return Optional.empty();
          }

          @Override
          public String getMachineIdFilePath() {
            return getPathFromResource("com/redhat/insights/machine-id").toString();
          }
        };
    assertFalse(config.getMaybeAuthToken().isPresent());
    assertTrue(new File(config.getCertFilePath()).exists());
    assertTrue(new File(config.getKeyFilePath()).exists());
    assertTrue(new File(config.getMachineIdFilePath()).exists());
    assertTrue(
        config.getMaybeAuthToken().isPresent()
            || (new File(config.getCertFilePath()).exists()
                && new File(config.getKeyFilePath()).exists()
                && new File(config.getMachineIdFilePath()).exists()));
    assertTrue(
        new InsightsJdkHttpClient(logger, config, () -> mock(SSLContext.class)).isReadyToSend(),
        "Client should be ready to send");
  }

  @Test
  public void testIsReadyToSendWithToken() {
    InsightsConfiguration config =
        new InsightsConfiguration() {
          @Override
          public String getIdentificationName() {
            return "GOOD_MTLS";
          }

          @Override
          public String getKeyFilePath() {
            return getPathFromResource("com/redhat/insights/tls/dummy.key")
                .resolveSibling("wrong.key")
                .toString();
          }

          @Override
          public String getCertFilePath() {
            return getPathFromResource("com/redhat/insights/tls/dummy.cert")
                .resolveSibling("wrong.cert")
                .toString();
          }

          @Override
          public Optional<String> getMaybeAuthToken() {
            return Optional.of("random-token");
          }
        };
    assertTrue(
        new InsightsJdkHttpClient(logger, config, () -> mock(SSLContext.class)).isReadyToSend(),
        "Client should be ready to send");
  }

  @Test
  public void testIsReadyToSendWithMissingCert() {
    InsightsConfiguration config =
        new InsightsConfiguration() {
          @Override
          public String getIdentificationName() {
            return "BAD_CERT";
          }

          @Override
          public String getKeyFilePath() {
            return getPathFromResource("com/redhat/insights/tls/dummy.key").toString();
          }

          @Override
          public String getCertFilePath() {
            return getPathFromResource("com/redhat/insights/tls/dummy.cert")
                .resolveSibling("wrong.cert")
                .toString();
          }
        };
    assertFalse(
        new InsightsJdkHttpClient(logger, config, () -> mock(SSLContext.class)).isReadyToSend(),
        "Client shouldn't be ready to send because of wrong certificate path");
  }

  @Test
  public void testIsReadyToSendWithMissingKey() {
    InsightsConfiguration config =
        new InsightsConfiguration() {
          @Override
          public String getIdentificationName() {
            return "BAD_KEY";
          }

          @Override
          public String getKeyFilePath() {
            return getPathFromResource("com/redhat/insights/tls/dummy.key")
                .resolveSibling("wrong.key")
                .toString();
          }

          @Override
          public String getCertFilePath() {
            return getPathFromResource("com/redhat/insights/tls/dummy.cert").toString();
          }
        };
    assertFalse(
        new InsightsJdkHttpClient(logger, config, () -> mock(SSLContext.class)).isReadyToSend(),
        "Client shouldn't be ready to send because of wrong key path");
  }

  private Path getPathFromResource(String path) {
    return Paths.get(ClassLoader.getSystemClassLoader().getResource(path).getPath());
  }

  private HttpResponse<String> getOKResponse() {
    return new HttpResponseBase() {
      @Override
      public int statusCode() {
        return 202;
      }
    };
  }
}
