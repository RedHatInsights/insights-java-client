/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.configuration;

import static com.redhat.insights.config.EnvAndSysPropsInsightsConfiguration.*;
import static com.redhat.insights.config.EnvAndSysPropsInsightsConfiguration.ENV_UPDATE_PERIOD;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.redhat.insights.config.EnvAndSysPropsInsightsConfiguration;
import java.time.Duration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.properties.SystemProperties;

/** Test that set system properties are passed correctly to InsightsConfiguration. */
@ExtendWith(SystemStubsExtension.class)
public class SysPropConfigurationTest {

  private static EnvAndSysPropsInsightsConfiguration config;

  @SystemStub
  private SystemProperties systemProperties =
      new SystemProperties()
          .set(ENV_IDENTIFICATION_NAME.toLowerCase().replace("_", "."), "sys-name")
          .set(ENV_KEY_FILE_PATH.toLowerCase().replace("_", "."), "sys-key")
          .set(ENV_CERT_FILE_PATH.toLowerCase().replace("_", "."), "sys-cert")
          .set(ENV_UPLOAD_BASE_URL.toLowerCase().replace("_", "."), "sys-upload-url")
          .set(ENV_UPLOAD_URI.toLowerCase().replace("_", "."), "sys-upload-path")
          .set(ENV_PROXY_HOST.toLowerCase().replace("_", "."), "sys-proxy-host")
          .set(ENV_PROXY_PORT.toLowerCase().replace("_", "."), "54321")
          .set(ENV_OPT_OUT.toLowerCase().replace("_", "."), "true")
          .set(ENV_CONNECT_PERIOD.toLowerCase().replace("_", "."), "P3D")
          .set(ENV_UPDATE_PERIOD.toLowerCase().replace("_", "."), "PT20M")
          .set(ENV_HTTP_CLIENT_RETRY_INITIAL_DELAY.toLowerCase().replace("_", "."), "5000")
          .set(ENV_HTTP_CLIENT_RETRY_BACKOFF_FACTOR.toLowerCase().replace("_", "."), "3")
          .set(ENV_HTTP_CLIENT_RETRY_MAX_ATTEMPTS.toLowerCase().replace("_", "."), "5")
          .set(ENV_HTTP_CLIENT_TIMEOUT.toLowerCase().replace("_", "."), "PT2M")
          .set(ENV_CERT_HELPER_BINARY.toLowerCase().replace("_", "."), "/usr/local/bin/yolo");

  // clean env variables which might interfere this test
  @SystemStub
  private EnvironmentVariables environmentVariables =
      new EnvironmentVariables()
          .set(ENV_IDENTIFICATION_NAME, null)
          .set(ENV_KEY_FILE_PATH, null)
          .set(ENV_CERT_FILE_PATH, null)
          .set(ENV_UPLOAD_BASE_URL, null)
          .set(ENV_UPLOAD_URI, null)
          .set(ENV_PROXY_HOST, null)
          .set(ENV_PROXY_PORT, null)
          .set(ENV_OPT_OUT, null)
          .set(ENV_CONNECT_PERIOD, null)
          .set(ENV_UPDATE_PERIOD, null);

  @BeforeAll
  public static void setup() {
    config = new EnvAndSysPropsInsightsConfiguration();
  }

  @Test
  void testGetIdentificationName() {
    assertEquals(
        "sys-name",
        config.getIdentificationName(),
        "Configuration does not contain value passed through system property.");
  }

  @Test
  void testGetKeyFilePath() {
    assertEquals(
        "sys-key",
        config.getKeyFilePath(),
        "Configuration does not contain value passed through system property.");
  }

  @Test
  void testGetCertFilePath() {
    assertEquals(
        "sys-cert",
        config.getCertFilePath(),
        "Configuration does not contain value passed through system property.");
  }

  @Test
  void testGetUploadURL() {
    assertEquals(
        "sys-upload-url",
        config.getUploadBaseURL(),
        "Configuration does not contain value passed through system property.");
  }

  @Test
  void testGetUploadPath() {
    assertEquals(
        "sys-upload-path",
        config.getUploadUri(),
        "Configuration does not contain value passed through system property.");
  }

  @Test
  void testGetProxyConfiguration() {
    assertEquals(
        "sys-proxy-host",
        config.getProxyConfiguration().get().getHost(),
        "Configuration does not contain value passed through system property.");
    assertEquals(
        54321,
        config.getProxyConfiguration().get().getPort(),
        "Configuration does not contain value passed through system property.");
  }

  @Test
  void testIsOptingOut() {
    assertEquals(
        true,
        config.isOptingOut(),
        "Configuration does not contain value passed through system property.");
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
