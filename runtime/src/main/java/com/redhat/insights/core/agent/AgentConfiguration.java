/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.core.agent;

import com.redhat.insights.config.InsightsConfiguration;
import java.util.Map;
import java.util.Optional;

public final class AgentConfiguration implements InsightsConfiguration {

  public static final String ARG_NAME = "name";
  public static final String ARG_CERT = "cert";
  public static final String ARG_KEY = "key";
  public static final String ARG_URL = "url";
  public static final String ARG_PATH = "path";
  public static final String ARG_PROXY = "proxy";
  public static final String ARG_PROXY_PORT = "proxyPort";
  public static final String ARG_OPT_OUT = "optOut";

  private final Map<String, String> args;

  public AgentConfiguration(Map<String, String> args) {
    this.args = args;
  }

  @Override
  public String getIdentificationName() {
    return args.get(ARG_NAME);
  }

  @Override
  public String getCertFilePath() {
    return args.get(ARG_CERT);
  }

  @Override
  public String getKeyFilePath() {
    return args.get(ARG_KEY);
  }

  @Override
  public String getUploadBaseURL() {
    if (args.containsKey(ARG_URL)) {
      return args.get(ARG_URL);
    }
    return InsightsConfiguration.DEFAULT_UPLOAD_BASE_URL;
  }

  @Override
  public String getUploadUri() {
    if (args.containsKey(ARG_PATH)) {
      return args.get(ARG_PATH);
    }
    return InsightsConfiguration.DEFAULT_UPLOAD_URI;
  }

  @Override
  public Optional<ProxyConfiguration> getProxyConfiguration() {
    if (args.containsKey(ARG_PROXY) && args.containsKey(ARG_PROXY_PORT)) {
      return Optional.of(
          new ProxyConfiguration(
              args.get(ARG_PROXY), Integer.parseUnsignedInt(args.get(ARG_PROXY_PORT))));
    }
    return Optional.empty();
  }

  @Override
  public boolean isOptingOut() {
    if (args.containsKey(ARG_OPT_OUT)) {
      return "true".equalsIgnoreCase(args.get(ARG_OPT_OUT));
    }
    return false;
  }
}
