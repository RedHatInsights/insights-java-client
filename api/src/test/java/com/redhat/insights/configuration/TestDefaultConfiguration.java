/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.configuration;

import static org.junit.jupiter.api.Assertions.*;

import com.redhat.insights.config.InsightsConfiguration;
import com.redhat.insights.doubles.DefaultConfiguration;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import org.junit.jupiter.api.Test;

public class TestDefaultConfiguration {
  private static final InsightsConfiguration defaultConfig = new DefaultConfiguration();

  private static final String DEFAULT_RHEL_CERT_FILE_PATH = "/etc/pki/consumer/cert.pem";
  private static final String DEFAULT_RHEL_KEY_FILE_PATH = "/etc/pki/consumer/key.pem";

  private static final String DEFAULT_UPLOAD_PATH = "/api/ingress/v1/upload";

  @Test
  public void testPaths() {
    assertEquals(
        DEFAULT_RHEL_CERT_FILE_PATH,
        defaultConfig.getCertFilePath(),
        "Default cert file path should lead to " + DEFAULT_RHEL_CERT_FILE_PATH);
    assertEquals(
        DEFAULT_RHEL_KEY_FILE_PATH,
        defaultConfig.getKeyFilePath(),
        "Default key file path should lead to " + DEFAULT_RHEL_KEY_FILE_PATH);
  }

  @Test
  public void testUploadUrl() {
    String defaultUrl = defaultConfig.getUploadBaseURL();
    // validate it's url
    assertTrue(isUrlValid(defaultUrl), "Default upload url " + defaultUrl + " is not valid");

    // validate it points to .redhat.com system
    assertTrue(
        defaultUrl.endsWith(".redhat.com"),
        "Default upload url " + defaultUrl + " should end with .redhat.com");
  }

  @Test
  public void testUploadPath() {
    assertEquals(
        DEFAULT_UPLOAD_PATH,
        defaultConfig.getUploadUri(),
        "Default upload path should lead to " + DEFAULT_UPLOAD_PATH);
  }

  @Test
  public void testDefaultProxy() {
    // by default there should be no proxy
    assertFalse(
        defaultConfig.getProxyConfiguration().isPresent(),
        "By default there should be no proxy configured");
  }

  @Test
  public void testOptingOut() {
    // by default it should not opt out
    assertFalse(defaultConfig.isOptingOut(), "By default config should not opt out");
  }

  @Test
  public void testProxyConf() {
    String host = "https://lorem.ipsum.com";
    int port = 64321;

    // verify proxy conf holds the parameters correctly
    InsightsConfiguration.ProxyConfiguration proxyConfiguration =
        new InsightsConfiguration.ProxyConfiguration(host, port);

    assertEquals(host, proxyConfiguration.getHost());
    assertEquals(port, proxyConfiguration.getPort());
  }

  @Test
  void testHttpClientTimeout() {
    assertEquals(Duration.ofMinutes(1), defaultConfig.getHttpClientTimeout());
  }

  /*
   * This validation is not perfect, but will catch most basic mistakes
   */
  public boolean isUrlValid(String url) {
    try {
      URL obj = new URL(url);
      obj.toURI();
      return true;
    } catch (MalformedURLException | URISyntaxException e) {
      return false;
    }
  }
}
