/* Copyright (C) Red Hat 2023 */
package com.redhat.insights;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.redhat.insights.doubles.DummyTopLevelReport;
import com.redhat.insights.doubles.NoopInsightsLogger;
import com.redhat.insights.jars.ClasspathJarInfoSubreport;
import com.redhat.insights.jars.JarInfo;
import com.redhat.insights.jars.JarInfoSubreport;
import com.redhat.insights.jars.JarInfoSubreportSerializer;
import com.redhat.insights.logging.InsightsLogger;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;
import org.junit.jupiter.api.Test;

public class TestTopLevelReport {
  private static final InsightsLogger logger = new NoopInsightsLogger();

  @Test
  public void testGenerateReportWithoutSubreports() throws IOException {
    String idHash = "RandomIdHash";

    DummyTopLevelReport insightsReport = new DummyTopLevelReport(logger, Collections.emptyMap());
    insightsReport.setIdHash(idHash);
    String report = generateReport(insightsReport);

    Map<?, ?> parsedReport = parseReport(report);

    // there should be 3 top level fields - version, idHash and "basic"(top level report)
    assertEquals(
        3,
        parsedReport.size(),
        "Top level report should have 3 fields - version, idHash and basic");

    // validate version
    assertTrue(parsedReport.containsKey("version"), "Report should have entry \"version\"");
    assertTrue(parsedReport.get("version") instanceof String, "Version in report should be string");
    assertTrue(
        validateVersion((String) parsedReport.get("version")), "Version in report should be valid");

    // validate idHash
    assertTrue(parsedReport.containsKey("idHash"), "Report should have entry \"idHash\"");
    assertEquals(
        idHash,
        parsedReport.get("idHash"),
        "IdHash field in the report, should have the set value");

    // validate object basic
    assertTrue(parsedReport.containsKey("basic"), "Report should have entry \"basic\"");
    Map<?, ?> basicReport = (Map<?, ?>) parsedReport.get("basic");

    // validate fields in basic report
    // check basic ones, if there is a way to validate all field (and know what values they SHOULD
    // have), it can be expanded in the future
    assertTrue(
        basicReport.containsKey("jvm.pid"), "The should be \"jvm.pid\" Field in basic report");
    assertEquals(
        (int) insightsReport.getProcessPID(),
        basicReport.get("jvm.pid"),
        "PID in report should be same as set");
    assertTrue(
        basicReport.containsKey("java.vendor"),
        "The should be \"java.vendor\" Field in basic report");
    assertTrue(
        basicReport.containsKey("system.arch"),
        "The should be \"system.arch\" Field in basic report");
    assertTrue(
        basicReport.containsKey("jvm.packages"),
        "The should be \"jvm.packages\" Field in basic report");
  }

  /** Test serialization of report as empty as it can be */
  @Test
  public void testEmptyReport() throws IOException {
    DummyTopLevelReport insightsReport = new DummyTopLevelReport(logger, Collections.emptyMap());
    insightsReport.setIdHash(null);
    insightsReport.setGenerateEmpty(true);
    String report = generateReport(insightsReport);

    Map<?, ?> parsedReport = parseReport(report);

    // empty report should have just one field - version
    assertEquals(1, parsedReport.size(), "Empty top level report should have 1 field - version");

    assertFalse(parsedReport.containsKey("idHash"), "Null idHash should not be in the report");

    // validate that one field is version
    assertTrue(parsedReport.containsKey("version"), "Report should have entry \"version\"");
  }

  @Test
  public void testGenerateReportWithEmptySubreport() throws IOException {
    JarInfoSubreport jarInfoSubreport =
        new JarInfoSubreport(logger, Collections.singletonList(JarInfo.MISSING));
    InsightsReport insightsReport =
        new DummyTopLevelReport(
            logger,
            new HashMap<String, InsightsSubreport>() {
              {
                put("jarsSubreport", jarInfoSubreport);
              }
            });

    String report = generateReport(insightsReport);

    Map<?, ?> parsedReport = parseReport(report);

    // this report should have 3 fields - version, basic and jarsSubreport
    assertEquals(
        3, parsedReport.size(), "Report should have 3 fields - version, basic and jarsSubreport");

    // validate fields in top level report
    assertTrue(parsedReport.containsKey("version"), "Report should have field \"version\"");
    assertTrue(parsedReport.containsKey("basic"), "Report should have field \"basic\"");
    assertTrue(
        parsedReport.containsKey("jarsSubreport"), "Report should have field \"jarsSubreport\"");

    // validate jars subreport
    Map<?, ?> jarsSubreport = (Map<?, ?>) parsedReport.get("jarsSubreport");

    // check version
    assertTrue(
        jarsSubreport.containsKey("version"), "Jars subreport should have field \"version\"");
    assertTrue(
        validateVersion((String) jarsSubreport.get("version")),
        "Jars subreport version should be valid");

    // check jars included
    assertTrue(jarsSubreport.containsKey("jars"), "Jars subreport should have field \"jars\"");
    assertTrue(jarsSubreport.get("jars") instanceof Collection, "Jars field should be collection");
    assertEquals(
        1, ((Collection) jarsSubreport.get("jars")).size(), "Jars field should have one field");
  }

  @Test
  public void testGenerateReportWithJarInfoSubreports() throws IOException {
    // prepare JarInfo
    JarInfo jarInfoWithoutAttrs = new JarInfo("RandomName", "0.9", Collections.emptyMap());
    JarInfo jarInfoWithAttrs =
        new JarInfo(
            "DifferentName :\" \n",
            "0.1",
            new HashMap<String, String>() {
              {
                put("attr1", "value1");
                put("attr2", "value2 \t \t ");
              }
            });

    // put JarInfos into subreport
    JarInfoSubreport jarInfoSubreport =
        new JarInfoSubreport(logger, Arrays.asList(jarInfoWithoutAttrs, jarInfoWithAttrs));

    // create top level report with subreports
    InsightsReport insightsReport =
        new DummyTopLevelReport(
            logger,
            new HashMap<String, InsightsSubreport>() {
              {
                put("jarsSubreport", jarInfoSubreport);
                put("classpathSubreport", new ClasspathJarInfoSubreport(logger));
              }
            });

    String report = generateReport(insightsReport);

    Map<?, ?> parsedReport = parseReport(report);

    // this report should have 4 fields - version, basic, jarsSubreport and classpathSubreport
    assertEquals(
        4,
        parsedReport.size(),
        "Top level report should have 4 field - version, basic, jarsSubreport and"
            + " classpathSubreport");

    // validate fields in top level report
    assertTrue(parsedReport.containsKey("version"), "Report should have field \"version\"");
    assertTrue(parsedReport.containsKey("basic"), "Report should have field \"basic\"");
    assertTrue(
        parsedReport.containsKey("jarsSubreport"), "Report should have field \"jarsSubreport\"");
    assertTrue(
        parsedReport.containsKey("classpathSubreport"),
        "Report should have field \"classpathSubreport\"");

    // check there is multiple jars in jarSubreport
    assertEquals(
        2,
        ((List) ((Map<?, ?>) parsedReport.get("jarsSubreport")).get("jars")).size(),
        "There should be 2 entries in jars field");

    // check classpath subreport
    Map<?, ?> classpathSubreport = (Map<?, ?>) parsedReport.get("classpathSubreport");

    // check version
    assertTrue(
        classpathSubreport.containsKey("version"),
        "Classpath subreport should have field \"version\"");
    assertTrue(
        validateVersion((String) classpathSubreport.get("version")),
        "Classpath subreport's version should be valid");

    // check jars
    assertTrue(
        classpathSubreport.containsKey("jars"), "Classpath subreport should have field \"jars\"");
    assertTrue(
        ((Collection) classpathSubreport.get("jars")).size() > 5,
        "Classpath subreport should have more than 5 jars");
  }

  @Test
  public void testGenerateReportWithPackages() throws IOException {
    DummyTopLevelReport insightsReport = new DummyTopLevelReport(logger, Collections.emptyMap());
    insightsReport.setPackages(Package.getPackages());

    String report = generateReport(insightsReport);

    Map<?, ?> basicReport = (Map<?, ?>) parseReport(report).get("basic");

    // check packages field is present
    assertTrue(
        basicReport.containsKey("jvm.packages"), "Basic report should have field \"jvm.packages\"");
    String packagesString = (String) basicReport.get("jvm.packages");

    // briefly check content of packages string
    assertTrue(
        packagesString.contains("package"),
        "There should be substring \"package\" in packages field");
    assertTrue(
        packagesString.contains("version"),
        "There should be substring \"version\" in packages field");

    // check specific packages
    for (Package pack : insightsReport.getPackages()) {
      assertTrue(
          packagesString.contains(pack.getName()),
          "Package " + pack.getName() + " should be in the output");
      if (pack.getSpecificationVersion() != null) {
        assertTrue(
            packagesString.contains(pack.getSpecificationVersion()),
            "Version "
                + pack.getSpecificationVersion()
                + " of package "
                + pack.getName()
                + " should be in the output");
      }
    }
  }

  private JsonGenerator getJsonGenerator(Writer output) throws IOException {
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

  private String generateReport(InsightsReport insightsReport) throws IOException {
    insightsReport.generateReport(Filtering.DEFAULT);

    StringWriter stringWriter = new StringWriter();
    insightsReport.getSerializer().serialize(insightsReport, getJsonGenerator(stringWriter), null);
    return stringWriter.toString();
  }

  private Map<?, ?> parseReport(String report) throws JsonProcessingException {
    JsonMapper mapper = new JsonMapper();
    return mapper.readValue(report, Map.class);
  }

  private boolean validateVersion(String version) {
    return version.matches("^\\d\\.\\d\\.\\d.*$");
  }
}
