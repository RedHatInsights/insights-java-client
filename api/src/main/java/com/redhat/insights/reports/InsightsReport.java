/* Copyright (C) Red Hat 2022-2023 */
package com.redhat.insights.reports;

import static com.redhat.insights.InsightsErrorCode.ERROR_SERIALIZING_TO_JSON;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.insights.Filtering;
import com.redhat.insights.InsightsException;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

/**
 * Top-level insights report.
 *
 * @see InsightsSubreport for runtime-specific sub-reports
 */
public interface InsightsReport extends Closeable {
  static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

  Map<String, InsightsSubreport> getSubreports();

  JsonSerializer<InsightsReport> getSerializer();

  /**
   * Filtering must be passed in as it may not be possible to determine the filtering level when
   * this object is created - the specific product may have to tell us.
   *
   * @param masking a function to filter some insight entries
   */
  void generateReport(Filtering masking);

  Map<String, Object> getBasic();

  String getVersion();

  void setIdHash(String hash);

  String getIdHash();

  void decorate(String key, String value);

  /**
   * Serializes this report to JSON for transport.
   *
   * @return JSON serialized report as a stream of UTF-8 encoded bytes.
   */
  default byte[] serializeRaw() {
    ObjectMapper mapper = ObjectMappers.createFor(this);

    try {
      return mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(this);
    } catch (IOException e) {
      throw new InsightsException(ERROR_SERIALIZING_TO_JSON, "JSON serialization exception", e);
    }
  }

  /**
   * Serializes this report to JSON for transport
   *
   * @return JSON serialized report.
   * @deprecated use #serializeRaw instead.
   */
  @Deprecated
  default String serialize() {
    ObjectMapper mapper = ObjectMappers.createFor(this);

    try {
      return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
    } catch (IOException e) {
      throw new InsightsException(ERROR_SERIALIZING_TO_JSON, "JSON serialization exception", e);
    }
  }

  /**
   * Serializes this report to JSON for transport
   *
   * @return JSON serialized report
   */
  default byte[] getSubModulesReport() {
    ObjectMapper mapper = ObjectMappers.createFor(this);

    try (ByteArrayOutputStream out = new ByteArrayOutputStream();
        JsonGenerator generator = mapper.writerWithDefaultPrettyPrinter().createGenerator(out)) {
      generator.writeStartObject();
      for (Map.Entry<String, InsightsSubreport> entry : getSubreports().entrySet()) {
        generator.writeObjectField(entry.getKey(), entry.getValue());
      }
      generator.writeEndObject();
      generator.flush();
      byte[] report = out.toByteArray();
      // The subreports are in an array and are to be added to the report array.
      // Thus we are removing the {} enclosing the subreports array and adding a ',' to append to
      // the existing array.
      boolean notEmptyArray = report.length > 3;
      if (notEmptyArray) {
        byte[] finalReport = new byte[report.length - 1];
        finalReport[0] = ',';
        System.arraycopy(report, 1, finalReport, 1, report.length - 2);
        return finalReport;
      }
      return EMPTY_BYTE_ARRAY;
    } catch (IOException e) {
      throw new InsightsException(ERROR_SERIALIZING_TO_JSON, "JSON serialization exception", e);
    }
  }
}
