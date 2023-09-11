/* Copyright (C) Red Hat 2023 */
package com.redhat.insights;

import com.fasterxml.jackson.databind.JsonSerializer;
import com.redhat.insights.jars.JarInfo;
import com.redhat.insights.jars.JarInfoSubreport;
import com.redhat.insights.logging.InsightsLogger;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;

/** An {@code UPDATE} event report implementation. */
public class UpdateReportImpl implements InsightsReport {

  private String idHash = "";
  private final BlockingQueue<JarInfo> updatedJars;
  private final InsightsLogger logger;
  private Optional<JarInfoSubreport> subreport = Optional.empty();
  private final InsightsReportSerializer serializer;

  public UpdateReportImpl(BlockingQueue<JarInfo> updatedJars, InsightsLogger logger) {
    this.updatedJars = updatedJars;
    this.logger = logger;
    this.serializer = new InsightsReportSerializer();
  }

  @Override
  public Map<String, InsightsSubreport> getSubreports() {
    if (!subreport.isPresent()) {
      return Collections.emptyMap();
    }
    return Collections.singletonMap("updated-jars", subreport.get());
  }

  @Override
  public JsonSerializer<InsightsReport> getSerializer() {
    return serializer;
  }

  @Override
  public void generateReport(Filtering masking) {
    if (!updatedJars.isEmpty()) {
      List<JarInfo> jars = new ArrayList<>();
      int sendCount = updatedJars.drainTo(jars);
      JarInfoSubreport jarInfoSubreport = new JarInfoSubreport(logger, jars);
      jarInfoSubreport.generateReport();
      subreport = Optional.of(jarInfoSubreport);
      logger.debug("Sending " + sendCount + " jars from " + getIdHash());
    }
  }

  @Override
  public Map<String, Object> getBasic() {
    return Collections.emptyMap();
  }

  @Override
  public String getVersion() {
    return "1.0.0";
  }

  @Override
  public void setIdHash(String idHash) {
    this.idHash = idHash;
  }

  @Override
  public String getIdHash() {
    return idHash;
  }

  @Override
  public void decorate(String key, String value) {
    logger.debug(
        String.format("Attempt to add %s => %s to an update report. Ignored.", key, value));
  }

  @Override
  public void close() throws IOException {}
}
