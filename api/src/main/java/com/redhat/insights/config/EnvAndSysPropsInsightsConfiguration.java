/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.config;

import static com.redhat.insights.InsightsErrorCode.ERROR_IDENTIFICATION_NOT_DEFINED;

import com.redhat.insights.InsightsException;
import java.time.Duration;
import java.util.Optional;

/**
 * Configuration where values from {@link DefaultInsightsConfiguration} can be overridden by
 * environment variables and system properties.
 *
 * <p>Environment variables take priority over system properties.
 */
public class EnvAndSysPropsInsightsConfiguration extends DefaultInsightsConfiguration {

  public static final String ENV_IDENTIFICATION_NAME = "RHT_INSIGHTS_JAVA_IDENTIFICATION_NAME";
  public static final String ENV_CERT_FILE_PATH = "RHT_INSIGHTS_JAVA_CERT_FILE_PATH";
  public static final String ENV_KEY_FILE_PATH = "RHT_INSIGHTS_JAVA_KEY_FILE_PATH";
  public static final String ENV_AUTH_TOKEN = "RHT_INSIGHTS_JAVA_AUTH_TOKEN";
  public static final String ENV_UPLOAD_BASE_URL = "RHT_INSIGHTS_JAVA_UPLOAD_BASE_URL";
  public static final String ENV_UPLOAD_URI = "RHT_INSIGHTS_JAVA_UPLOAD_URI";
  public static final String ENV_ARCHIVE_UPLOAD_DIR = "RHT_INSIGHTS_JAVA_ARCHIVE_UPLOAD_DIR";
  public static final String ENV_PROXY_HOST = "RHT_INSIGHTS_JAVA_PROXY_HOST";
  public static final String ENV_PROXY_PORT = "RHT_INSIGHTS_JAVA_PROXY_PORT";
  public static final String ENV_OPT_OUT = "RHT_INSIGHTS_JAVA_OPT_OUT";
  public static final String ENV_CONNECT_PERIOD = "RHT_INSIGHTS_JAVA_CONNECT_PERIOD";
  public static final String ENV_UPDATE_PERIOD = "RHT_INSIGHTS_JAVA_UPDATE_PERIOD";
  public static final String ENV_HTTP_CLIENT_RETRY_INITIAL_DELAY =
      "RHT_INSIGHTS_JAVA_HTTP_CLIENT_RETRY_INITIAL_DELAY";
  public static final String ENV_HTTP_CLIENT_RETRY_BACKOFF_FACTOR =
      "RHT_INSIGHTS_JAVA_HTTP_CLIENT_RETRY_BACKOFF_FACTOR";
  public static final String ENV_HTTP_CLIENT_RETRY_MAX_ATTEMPTS =
      "RHT_INSIGHTS_JAVA_HTTP_CLIENT_RETRY_MAX_ATTEMPTS";

  public static final String ENV_CERT_HELPER_BINARY = "RHT_INSIGHTS_JAVA_CERT_HELPER_BINARY";

  private String lookup(String env) {
    String value = System.getenv(env);
    if (value == null) {
      value = System.getProperty(env.toLowerCase().replace('_', '.'));
    }
    return value;
  }

  @Override
  public String getIdentificationName() {
    String value = lookup(ENV_IDENTIFICATION_NAME);
    if (value == null) {
      // TODO should we guess a name instead?
      //  Maybe: This has some subtleties and is not as simple as it might appear
      throw new InsightsException(
          ERROR_IDENTIFICATION_NOT_DEFINED, "The identification name has not been defined");
    }
    return value;
  }

  @Override
  public String getCertFilePath() {
    String value = lookup(ENV_CERT_FILE_PATH);
    if (value != null) {
      return value;
    }
    return super.getCertFilePath();
  }

  @Override
  public String getKeyFilePath() {
    String value = lookup(ENV_KEY_FILE_PATH);
    if (value != null) {
      return value;
    }
    return super.getKeyFilePath();
  }

  @Override
  public Optional<String> getMaybeAuthToken() {
    String value = lookup(ENV_AUTH_TOKEN);
    if (value != null) {
      return Optional.of(value);
    }
    return super.getMaybeAuthToken();
  }

  @Override
  public String getUploadBaseURL() {
    String value = lookup(ENV_UPLOAD_BASE_URL);
    if (value != null) {
      return value;
    }
    return super.getUploadBaseURL();
  }

  @Override
  public String getUploadUri() {
    String value = lookup(ENV_UPLOAD_URI);
    if (value != null) {
      return value;
    }
    return super.getUploadUri();
  }

  @Override
  public String getArchiveUploadDir() {
    String value = lookup(ENV_ARCHIVE_UPLOAD_DIR);
    if (value != null) {
      return value;
    }
    return super.getArchiveUploadDir();
  }

  @Override
  public Optional<ProxyConfiguration> getProxyConfiguration() {
    String host = lookup(ENV_PROXY_HOST);
    String port = lookup(ENV_PROXY_PORT);
    if (host == null || port == null) {
      return Optional.empty();
    }
    return Optional.of(new ProxyConfiguration(host, Integer.parseUnsignedInt(port)));
  }

  @Override
  public boolean isOptingOut() {
    String value = lookup(ENV_OPT_OUT);
    if (value != null) {
      return "true".equalsIgnoreCase(value.trim());
    }
    return super.isOptingOut();
  }

  @Override
  public Duration getConnectPeriod() {
    String value = lookup(ENV_CONNECT_PERIOD);
    if (value != null) {
      return Duration.parse(value);
    }
    return super.getConnectPeriod();
  }

  @Override
  public Duration getUpdatePeriod() {
    String value = lookup(ENV_UPDATE_PERIOD);
    if (value != null) {
      return Duration.parse(value);
    }
    return super.getUpdatePeriod();
  }

  @Override
  public long getHttpClientRetryInitialDelay() {
    String value = lookup(ENV_HTTP_CLIENT_RETRY_INITIAL_DELAY);
    if (value != null) {
      return Long.parseLong(value);
    }
    return super.getHttpClientRetryInitialDelay();
  }

  @Override
  public long getHttpClientRetryBackoffFactor() {
    String value = lookup(ENV_HTTP_CLIENT_RETRY_BACKOFF_FACTOR);
    if (value != null) {
      return Long.parseLong(value);
    }
    return super.getHttpClientRetryBackoffFactor();
  }

  @Override
  public int getHttpClientRetryMaxAttempts() {
    String value = lookup(ENV_HTTP_CLIENT_RETRY_MAX_ATTEMPTS);
    if (value != null) {
      return Integer.parseInt(value);
    }
    return super.getHttpClientRetryMaxAttempts();
  }

  @Override
  public String getCertHelperBinary() {
    String value = lookup(ENV_CERT_HELPER_BINARY);
    if (value != null) {
      return value;
    }
    return super.getCertHelperBinary();
  }
}
