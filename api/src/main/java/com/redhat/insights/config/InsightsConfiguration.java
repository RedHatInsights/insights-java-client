/* Copyright (C) Red Hat 2023-2024 */
package com.redhat.insights.config;

import java.time.Duration;
import java.util.Optional;

/**
 * Insights client configuration, covering topics such as PEM file locations, endpoint parameters,
 * proxing and more.
 */
public interface InsightsConfiguration {

  String DEFAULT_RHEL_CERT_FILE_PATH = "/etc/pki/consumer/cert.pem";
  String DEFAULT_RHEL_KEY_FILE_PATH = "/etc/pki/consumer/key.pem";
  String DEFAULT_RHEL_MACHINE_ID_FILE_PATH = "/etc/insights-client/machine-id";

  String DEFAULT_UPLOAD_BASE_URL = "https://cert.console.redhat.com";
  String DEFAULT_UPLOAD_URI = "/api/ingress/v1/upload";
  String DEFAULT_ARCHIVE_UPLOAD_DIR = "/var/tmp/insights-runtimes/uploads";

  String DEFAULT_CERT_HELPER_BINARY = "/opt/jboss-cert-helper";

  long DEFAULT_HTTP_CLIENT_RETRY_INITIAL_DELAY = 2000L;
  double DEFAULT_HTTP_CLIENT_RETRY_BACKOFF_FACTOR = 2.0;
  int DEFAULT_HTTP_CLIENT_RETRY_MAX_ATTEMPTS = 5;

  /**
   * The insights client identification name, to be adjusted to allow each runtime to define what an
   * "application name" means for their domain.
   *
   * @return the identification name
   */
  String getIdentificationName();

  default String getCertFilePath() {
    return DEFAULT_RHEL_CERT_FILE_PATH;
  }

  default String getKeyFilePath() {
    return DEFAULT_RHEL_KEY_FILE_PATH;
  }

  default String getMachineIdFilePath() {
    return DEFAULT_RHEL_MACHINE_ID_FILE_PATH;
  }

  default Optional<String> getMaybeAuthToken() {
    return Optional.empty();
  }

  default String getUploadBaseURL() {
    return DEFAULT_UPLOAD_BASE_URL;
  }

  default boolean useMTLS() {
    return !getMaybeAuthToken().isPresent();
  }

  default String getUploadUri() {
    return DEFAULT_UPLOAD_URI;
  }

  default String getArchiveUploadDir() {
    return DEFAULT_ARCHIVE_UPLOAD_DIR;
  }

  default Optional<ProxyConfiguration> getProxyConfiguration() {
    return Optional.empty();
  }

  default boolean isOptingOut() {
    String osName = System.getProperty("os.name");
    if (osName != null) {
      return osName.trim().toLowerCase().contains("windows");
    }
    return false;
  }

  default Duration getConnectPeriod() {
    return Duration.ofDays(1);
  }

  default Duration getUpdatePeriod() {
    return Duration.ofMinutes(5);
  }

  default long getHttpClientRetryInitialDelay() {
    return DEFAULT_HTTP_CLIENT_RETRY_INITIAL_DELAY;
  }

  default double getHttpClientRetryBackoffFactor() {
    return DEFAULT_HTTP_CLIENT_RETRY_BACKOFF_FACTOR;
  }

  default int getHttpClientRetryMaxAttempts() {
    return DEFAULT_HTTP_CLIENT_RETRY_MAX_ATTEMPTS;
  }

  default String getCertHelperBinary() {
    return DEFAULT_CERT_HELPER_BINARY;
  }

  default Duration getHttpClientTimeout() {
    return Duration.ofMinutes(1);
  }

  final class ProxyConfiguration {

    private final String host;
    private final int port;

    public ProxyConfiguration(String host, int port) {
      this.host = host;
      this.port = port;
    }

    public String getHost() {
      return host;
    }

    public int getPort() {
      return port;
    }
  }
}
