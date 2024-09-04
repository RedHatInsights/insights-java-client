/* Copyright (C) Red Hat 2023-2024 */
package com.redhat.insights.doubles;

import com.redhat.insights.config.DefaultInsightsConfiguration;

public class DefaultConfiguration extends DefaultInsightsConfiguration {
  @Override
  public String getIdentificationName() {
    return "[default]";
  }
}
