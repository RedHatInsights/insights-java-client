/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.configuration;

import static com.redhat.insights.config.EnvAndSysPropsInsightsConfiguration.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.redhat.insights.config.EnvAndSysPropsInsightsConfiguration;
import java.time.Duration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

/**
 * Test that set env variables are passed correctly to InsightsConfiguration and take precedence
 * before system properties.
 */
@ExtendWith(SystemStubsExtension.class)
public class EnvVariableConfigurationTest {

  private static EnvAndSysPropsInsightsConfiguration config;

  @SystemStub
  private EnvironmentVariables environmentVariables =
      new EnvironmentVariables()
          .set(ENV_IDENTIFICATION_NAME, "env-name")
          .set(ENV_KEY_FILE_PATH, "env-key")
          .set(ENV_CERT_FILE_PATH, "env-cert")
          .set(ENV_UPLOAD_BASE_URL, "env-upload-url")
          .set(ENV_UPLOAD_URI, "env-upload-path")
          .set(ENV_PROXY_HOST, "env-proxy-host")
          .set(ENV_PROXY_PORT, "12345")
          .set(ENV_OPT_OUT, "true")
          .set(ENV_CONNECT_PERIOD, "P3D")
          .set(ENV_UPDATE_PERIOD, "PT20M")
          .set(ENV_HTTP_CLIENT_RETRY_INITIAL_DELAY, "5000")
          .set(ENV_HTTP_CLIENT_RETRY_BACKOFF_FACTOR, "3")
          .set(ENV_HTTP_CLIENT_RETRY_MAX_ATTEMPTS, "5")
          .set(ENV_HTTP_CLIENT_TIMEOUT, "PT2M")
          .set(ENV_CERT_HELPER_BINARY, "/usr/local/bin/yolo");

  @BeforeAll
  public static void setup() {
    config = new EnvAndSysPropsInsightsConfiguration();
  }

  @Test
  void testGetIdentificationName() {
    assertEquals(
        "env-name",
        config.getIdentificationName(),
        "Configuration does not contain value passed through environment variable.");
  }

  @Test
  void testGetKeyFilePath() {
    assertEquals(
        "env-key",
        config.getKeyFilePath(),
        "Configuration does not contain value passed through environment variable.");
  }

  @Test
  void testGetCertFilePath() {
    assertEquals(
        "env-cert",
        config.getCertFilePath(),
        "Configuration does not contain value passed through environment variable.");
  }

  @Test
  void testGetUploadURL() {
    assertEquals(
        "env-upload-url",
        config.getUploadBaseURL(),
        "Configuration does not contain value passed through environment variable.");
  }

  @Test
  void testGetUploadPath() {
    assertEquals(
        "env-upload-path",
        config.getUploadUri(),
        "Configuration does not contain value passed through environment variable.");
  }

  @Test
  void testGetProxyConfiguration() {
    assertEquals(
        "env-proxy-host",
        config.getProxyConfiguration().get().getHost(),
        "Configuration does not contain value passed through environment variable.");
    assertEquals(
        12345,
        config.getProxyConfiguration().get().getPort(),
        "Configuration does not contain value passed through environment variable.");
  }

  @Test
  void testIsOptingOut() {
    assertEquals(
        true,
        config.isOptingOut(),
        "Configuration does not contain value passed through environment variable.");
  }

  @Test
  void testDurations() {
    assertEquals(
        Duration.ofDays(3),
        config.getConnectPeriod(),
        "Configuration does not contain value passed through environment variable");

    assertEquals(
        Duration.ofMinutes(20),
        config.getUpdatePeriod(),
        "Configuration does not contain value passed through environment variable");
  }

  @Test
  void testBackoff() {
    assertEquals(
        5000L,
        config.getHttpClientRetryInitialDelay(),
        "Configuration does not contain value passed through environment variable");

    assertEquals(
        3L,
        config.getHttpClientRetryBackoffFactor(),
        "Configuration does not contain value passed through environment variable");

    assertEquals(
        5,
        config.getHttpClientRetryMaxAttempts(),
        "Configuration does not contain value passed through environment variable");
  }

  @Test
  void testCertHelper() {
    assertEquals("/usr/local/bin/yolo", config.getCertHelperBinary());
  }

  @Test
  void testHttpClientTimeout() {
    assertEquals(Duration.ofMinutes(2), config.getHttpClientTimeout());
  }
}
