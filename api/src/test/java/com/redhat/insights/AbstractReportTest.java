/* Copyright (C) Red Hat 2023 */
package com.redhat.insights;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.redhat.insights.doubles.NoopInsightsLogger;
import com.redhat.insights.jars.JarInfoSubreport;
import com.redhat.insights.jars.JarInfoSubreportSerializer;
import com.redhat.insights.logging.InsightsLogger;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

public abstract class AbstractReportTest {
  protected static final InsightsLogger logger = new NoopInsightsLogger();

  /**
   * Check if version matches format: number dot number dot number suffix e.g 1.0.0 2.3.1-alpha
   * 3.2.2.GA
   */
  protected boolean validateVersion(String version) {
    return version.matches("^\\d\\.\\d\\.\\d.*$");
  }

  protected Map<?, ?> parseReport(String report) throws JsonProcessingException {
    JsonMapper mapper = new JsonMapper();
    return mapper.readValue(report, Map.class);
  }

  protected JsonGenerator getJsonGenerator(Writer output) throws IOException {
    SimpleModule simpleModule =
        new SimpleModule(
            "SimpleModule", new Version(1, 0, 0, null, "com.redhat.insights", "runtimes-java"));
    simpleModule.addSerializer(InsightsReport.class, new InsightsReportSerializer());
    simpleModule.addSerializer(JarInfoSubreport.class, new JarInfoSubreportSerializer());

    JsonMapper jsonMapper = new JsonMapper();
    jsonMapper.registerModule(simpleModule);

    JsonFactory factory = new JsonFactory();
    JsonGenerator jsonGenerator = factory.createGenerator(output);
    jsonGenerator.setCodec(jsonMapper);

    return jsonGenerator;
  }

  protected String generateReport(InsightsReport insightsReport) throws IOException {
    insightsReport.generateReport(Filtering.DEFAULT);

    StringWriter stringWriter = new StringWriter();
    insightsReport.getSerializer().serialize(insightsReport, getJsonGenerator(stringWriter), null);
    return stringWriter.toString();
  }
}
