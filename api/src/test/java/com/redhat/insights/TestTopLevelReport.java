/* Copyright (C) Red Hat 2023-2024 */
package com.redhat.insights;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.RETURNS_DEFAULTS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.redhat.insights.doubles.DummyTopLevelReport;
import com.redhat.insights.jars.ClasspathJarInfoSubreport;
import com.redhat.insights.jars.JarInfo;
import com.redhat.insights.jars.JarInfoSubreport;
import com.redhat.insights.reports.InsightsReport;
import com.redhat.insights.reports.InsightsSubreport;
import java.io.IOException;
import java.lang.management.*;
import java.util.*;
import org.jspecify.annotations.NullUnmarked;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

@NullUnmarked
public class TestTopLevelReport extends AbstractReportTest {
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
    Map<String, InsightsSubreport> reports = new HashMap<>();
    reports.put("jarsSubreport", jarInfoSubreport);

    InsightsReport insightsReport = new DummyTopLevelReport(logger, reports);

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
    Map<String, String> attrs = new HashMap<>();
    attrs.put("attr1", "value1");
    attrs.put("attr2", "value2 \t \t ");

    // prepare JarInfo
    JarInfo jarInfoWithoutAttrs = new JarInfo("RandomName", "0.9", Collections.emptyMap());
    JarInfo jarInfoWithAttrs = new JarInfo("DifferentName :\" \n", "0.1", attrs);

    // put JarInfos into subreport
    JarInfoSubreport jarInfoSubreport =
        new JarInfoSubreport(logger, Arrays.asList(jarInfoWithoutAttrs, jarInfoWithAttrs));

    Map<String, InsightsSubreport> reports = new HashMap<>();
    reports.put("jarsSubreport", jarInfoSubreport);
    reports.put("classpathSubreport", new ClasspathJarInfoSubreport(logger));

    // create top level report with subreports
    InsightsReport insightsReport = new DummyTopLevelReport(logger, reports);

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

  @Test
  public void testReportSanitization() throws IOException {
    DummyTopLevelReport insightsReport = new DummyTopLevelReport(logger, Collections.emptyMap());
    insightsReport.setPackages(Package.getPackages());

    List<String> unsanitizedJvmArgs =
        Arrays.asList(
            "-D[Standalone]",
            "-verbose:gc",
            "-Xloggc:/opt/jboss-eap-7.4.0/standalone/log/gc.log",
            "-XX:+PrintGCDetails",
            "-XX:+PrintGCDateStamps",
            "-XX:+UseGCLogFileRotation",
            "-XX:NumberOfGCLogFiles=5",
            "-XX:GCLogFileSize=3M",
            "-XX:-TraceClassUnloading",
            "-Djdk.serialFilter=maxbytes=10485760;maxdepth=128;maxarray=100000;maxrefs=300000",
            "-Xms1303m",
            "-Xmx2048m",
            "-XX:MetaspaceSize=128M",
            "-XX:MaxMetaspaceSize=512m",
            "-Djava.net.preferIPv4Stack=true",
            "-Djboss.modules.system.pkgs=org.jboss.byteman",
            "-Djava.awt.headless=true",
            "-Dorg.jboss.boot.log.file=/opt/jboss-eap-7.4.0/standalone/log/server.log",
            "-Dsome.dumb.practice=\"Man I hope \\\"' this = works\"",
            "-Dlogging.configuration=file:/opt/jboss-eap-7.4.0/standalone/configuration/logging.properties");
    List<String> sanitizedJvmArgs =
        Arrays.asList(
            "-D[Standalone]",
            "-verbose:gc",
            "-Xloggc:/opt/jboss-eap-7.4.0/standalone/log/gc.log",
            "-XX:+PrintGCDetails",
            "-XX:+PrintGCDateStamps",
            "-XX:+UseGCLogFileRotation",
            "-XX:NumberOfGCLogFiles=5",
            "-XX:GCLogFileSize=3M",
            "-XX:-TraceClassUnloading",
            "-Djdk.serialFilter=ZZZZZZZZZ",
            "-Xms1303m",
            "-Xmx2048m",
            "-XX:MetaspaceSize=128M",
            "-XX:MaxMetaspaceSize=512m",
            "-Djava.net.preferIPv4Stack=ZZZZZZZZZ",
            "-Djboss.modules.system.pkgs=ZZZZZZZZZ",
            "-Djava.awt.headless=ZZZZZZZZZ",
            "-Dorg.jboss.boot.log.file=ZZZZZZZZZ",
            "-Dsome.dumb.practice=ZZZZZZZZZ",
            "-Dlogging.configuration=ZZZZZZZZZ");

    // Mock the ManagementFactory and RuntimeMXBean to make it give our data
    // But first collect the necessary beans to give back to the ManagementFactory
    // If you don't those methods will return null, even if you use
    // CallRealMethod or RETURNS_DEFAULTS
    OperatingSystemMXBean systemMXBean = ManagementFactory.getOperatingSystemMXBean();
    MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    List<GarbageCollectorMXBean> gcMxBeans = ManagementFactory.getGarbageCollectorMXBeans();
    try (MockedStatic<ManagementFactory> mockFactory =
        mockStatic(ManagementFactory.class, withSettings().defaultAnswer(RETURNS_DEFAULTS))) {
      RuntimeMXBean mockRuntimeBean =
          mock(RuntimeMXBean.class, withSettings().defaultAnswer(RETURNS_DEFAULTS));
      mockFactory.when(ManagementFactory::getOperatingSystemMXBean).thenReturn(systemMXBean);
      mockFactory.when(ManagementFactory::getMemoryMXBean).thenReturn(memoryMXBean);
      mockFactory.when(ManagementFactory::getGarbageCollectorMXBeans).thenReturn(gcMxBeans);
      when(mockRuntimeBean.getInputArguments()).thenReturn(unsanitizedJvmArgs);
      mockFactory.when(ManagementFactory::getRuntimeMXBean).thenReturn(mockRuntimeBean);

      String unsanitizedJavaCommand =
          "/opt/jboss/7/eap/jboss-modules.jar -mp"
              + " /opt/jboss/7/eap/modules:/opt/jboss/7/eap/../modules org.jboss.as.standalone"
              + " -Djboss.home.dir=/opt/jboss/7/eap"
              + " -Djboss.server.base.dir=/opt/jboss/7/instances/jboss-bdi-dwhprosa -c"
              + " standalone.xml -Djboss.server.base.dir=/opt/jboss/7/instances/jboss-bdi-dwhprosa";
      String sanitizedJavaCommand =
          "/opt/jboss/7/eap/jboss-modules.jar -mp"
              + " /opt/jboss/7/eap/modules:/opt/jboss/7/eap/../modules org.jboss.as.standalone"
              + " -Djboss.home.dir=ZZZZZZZZZ -Djboss.server.base.dir=ZZZZZZZZZ -c standalone.xml"
              + " -Djboss.server.base.dir=ZZZZZZZZZ";

      // Set our java command property
      System.setProperty("sun.java.command", unsanitizedJavaCommand);

      String report = generateReport(insightsReport);
      Map<?, ?> basicReport = (Map<?, ?>) parseReport(report).get("basic");

      assertEquals(
          String.join(" ", sanitizedJvmArgs.toArray(new String[0])),
          basicReport.get("jvm.args"),
          "The \"jvm.args\" property in the basic report should be properly sanitized.");
      assertEquals(
          sanitizedJavaCommand,
          basicReport.get("java.command"),
          "The \"java.command\" property in the basic report should be properly sanitized.");
    }
  }
}
