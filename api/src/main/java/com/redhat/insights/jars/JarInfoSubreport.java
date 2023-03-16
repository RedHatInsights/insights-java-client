/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.jars;

import static com.redhat.insights.InsightsErrorCode.ERROR_SERIALIZING_TO_JSON;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.redhat.insights.InsightsException;
import com.redhat.insights.InsightsSubreport;
import com.redhat.insights.logging.InsightsLogger;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class JarInfoSubreport implements InsightsSubreport {
  protected final InsightsLogger logger;
  protected final Collection<JarInfo> jarInfos;

  public JarInfoSubreport(InsightsLogger logger) {
    this.logger = logger;
    this.jarInfos = new ArrayList<>();
  }

  public JarInfoSubreport(InsightsLogger logger, Collection<JarInfo> jarInfos) {
    this.logger = logger;
    this.jarInfos = jarInfos;
  }

  public Collection<JarInfo> getJarInfos() {
    return Collections.unmodifiableCollection(jarInfos);
  }

  @Override
  public void generateReport() {
    // Extension point, to be overridden if needed
  }

  @Override
  public String getVersion() {
    return "1.0.0";
  }

  @Override
  public JsonSerializer<InsightsSubreport> getSerializer() {
    return new JarInfoSubreportSerializer();
  }

  public String serializeReport() {
    ObjectMapper mapper = new ObjectMapper();

    SimpleModule simpleModule =
        new SimpleModule(
            "SimpleModule", new Version(1, 0, 0, null, "com.redhat.insights", "runtimes-java"));
    simpleModule.addSerializer(getClass(), getSerializer());
    mapper.registerModule(simpleModule);

    StringWriter writer = new StringWriter();
    try {
      mapper.writerWithDefaultPrettyPrinter().writeValue(writer, this);
    } catch (IOException e) {
      throw new InsightsException(ERROR_SERIALIZING_TO_JSON, "JSON serialization exception", e);
    }
    return writer.toString();
  }
}
