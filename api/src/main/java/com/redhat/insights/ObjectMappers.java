/* Copyright (C) Red Hat 2023 */
package com.redhat.insights;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * And util class to provide a factory method for ObjectMapper used within {@link
 * InsightsReport#serialize} methods
 */
class ObjectMappers {

  public static ObjectMapper createFor(InsightsReport insightsReport) {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());

    SimpleModule simpleModule =
        new SimpleModule(
            "SimpleModule", new Version(1, 0, 0, null, "com.redhat.insights", "runtimes-java"));
    simpleModule.addSerializer(InsightsReport.class, insightsReport.getSerializer());
    for (InsightsSubreport subreport : insightsReport.getSubreports().values()) {
      simpleModule.addSerializer(subreport.getClass(), subreport.getSerializer());
    }
    mapper.registerModule(simpleModule);
    return mapper;
  }
}
