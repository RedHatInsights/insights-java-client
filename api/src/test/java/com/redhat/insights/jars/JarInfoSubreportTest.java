/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.jars;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.redhat.insights.AbstractReportTest;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * Test for JarInfoSubreport, JarInfoSubreportSerializer and ClasspathJarInfoSubreport
 *
 * @see JarInfoSubreport
 * @see JarInfoSubreportSerializer
 * @see ClasspathJarInfoSubreport
 */
public class JarInfoSubreportTest extends AbstractReportTest {
  // list a few jars that should be in a classpath
  private static final List<String> expectedJars =
      Arrays.asList("junit-jupiter-engine", "jackson-core");

  /**
   * Test JarInfoSubreport with no jarInfos test its basic methods and validate basics things in
   * report
   */
  @Test
  public void testEmptyJarInfos() throws JsonProcessingException {
    JarInfoSubreport subreport = new JarInfoSubreport(logger);

    assertTrue(
        subreport.getJarInfos().isEmpty(), "Subreport with no JarInfos should have no JarInfos");

    assertTrue(
        validateVersion(subreport.getVersion()),
        "Subreport version should match regex \\d\\.\\d\\.\\d.*");

    assertTrue(
        subreport.getSerializer() instanceof JarInfoSubreportSerializer,
        "Serializer of JarInfoSubreport should be JarInfoSubreportSerializer");

    // generate report
    String report = new String(subreport.serializeReport(), StandardCharsets.UTF_8);

    // parse report and store it as a key-value map
    Map<?, ?> map = parseReport(report);

    // check there are only 2 entries in the report - it should be "version" and "jars" and nothing
    // more
    assertEquals(2, map.size(), "Report should have exactly 2 entries - version and jars");

    // check version
    assertTrue(map.containsKey("version"), "Report should have entry \"version\"");
    assertTrue(map.get("version") instanceof String, "Version in report should be string");
    assertTrue(validateVersion((String) map.get("version")), "Version in report should be valid");

    // check there is empty jars field in the report
    assertTrue(map.containsKey("jars"), "Report should have entry \"jars\"");
    assertTrue(map.get("jars") instanceof Collection, "Jars in report should be a collection");
    assertTrue(
        ((Collection<?>) map.get("jars")).isEmpty(), "Jars from empty subreport should be empty");
  }

  /**
   * Check JarInfoSubreport with JarInfos Also check some special characters and how are they
   * handled in JSON report
   */
  @Test
  public void testWithJarInfos() throws JsonProcessingException {
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

    JarInfoSubreport subreport =
        new JarInfoSubreport(
            logger, Arrays.asList(jarInfoWithoutAttrs, jarInfoWithAttrs, JarInfo.MISSING));

    // generate report
    String report = new String(subreport.serializeReport(), StandardCharsets.UTF_8);
    Map<?, ?> map = parseReport(report);

    assertTrue(map.get("jars") instanceof List, "Jars in report should be parsed as list");
    List<?> jarList = (List<?>) map.get("jars");

    assertEquals(3, jarList.size(), "There should be 3 jars in the report");

    validateJarInfoEntry(jarInfoWithoutAttrs, (Map<?, ?>) jarList.get(0));
    validateJarInfoEntry(jarInfoWithAttrs, (Map<?, ?>) jarList.get(1));
    validateJarInfoEntry(JarInfo.MISSING, (Map<?, ?>) jarList.get(2));
  }

  /**
   * Test that some at least some jars are detected by ClasspathJarInfoSubreport Doing exhaustive
   * validation of all jars in actual classpath is not feasible Possible future extension is to mock
   * entire classpath and validate all jars in it
   */
  @Test
  public void smokeTestClasspathJarsInReport() throws JsonProcessingException {
    ClasspathJarInfoSubreport classpathJarInfoSubreport = new ClasspathJarInfoSubreport(logger);

    // generate report for jars in current classpath
    classpathJarInfoSubreport.generateReport();

    String report = new String(classpathJarInfoSubreport.serializeReport(), StandardCharsets.UTF_8);
    Map<?, ?> parsedReport = parseReport(report);
    List<Map<String, ?>> jars = (List<Map<String, ?>>) parsedReport.get("jars");

    // check that all expected jars are in a classpath report
    expectedJars.forEach(
        expectedJarName ->
            assertTrue(
                jars.stream().anyMatch(map -> ((String) map.get("name")).contains(expectedJarName)),
                "Expected to find jar " + expectedJarName + " in classpath"));
  }

  private void validateJarInfoEntry(JarInfo expectedInfo, Map<?, ?> infoFromReport) {
    // check name
    assertTrue(
        infoFromReport.containsKey("name"),
        "Info from report should have \"name\" attribute: " + infoFromReport);
    assertTrue(
        infoFromReport.get("name") instanceof String,
        "Name from report should be String " + infoFromReport);
    assertEquals(expectedInfo.name(), infoFromReport.get("name"));

    // check version
    assertTrue(
        infoFromReport.containsKey("version"),
        "Info from report should have \"version\" attribute: " + infoFromReport);
    assertTrue(
        infoFromReport.get("version") instanceof String,
        "Version from report should be String " + infoFromReport);
    assertEquals(expectedInfo.version(), infoFromReport.get("version"));

    // check attributes
    assertTrue(
        infoFromReport.containsKey("attributes"),
        "Info from report should have \"attributes\" attribute: " + infoFromReport);
    assertTrue(
        infoFromReport.get("attributes") instanceof Map,
        "Version from report should be Map " + infoFromReport);
    assertEquals(expectedInfo.attributes(), infoFromReport.get("attributes"));
  }
}
