/* Copyright (C) Red Hat 2023-2024 */
package com.redhat.insights.jars;

import static org.junit.jupiter.api.Assertions.*;

import com.redhat.insights.doubles.NoopInsightsLogger;
import com.redhat.insights.logging.InsightsLogger;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

public class RecursiveJarAnalyzerHelperTest {

  public RecursiveJarAnalyzerHelperTest() {}

  /** Test of listDeploymentContent method, of class RecursiveJarAnalyzerHelper. */
  @Test
  public void testListDeploymentContent() throws Exception {
    InsightsLogger logger = new NoopInsightsLogger();
    JarAnalyzer analyzer = new JarAnalyzer(logger, true);
    Path tempDir = Paths.get("target", "tmp");
    Files.createDirectories(tempDir);
    String parentName = "numberguess.war";
    Path deployment =
        Paths.get("target", "test-classes", "com", "redhat", "insights", "jars", "numberguess.war");
    RecursiveJarAnalyzerHelper instance = new RecursiveJarAnalyzerHelper(logger);
    List<JarInfo> result =
        instance.listDeploymentContent(analyzer, tempDir, parentName, deployment);
    assertNotNull(result);
    assertEquals(3, result.size());
    Map<String, JarInfo> jars =
        result.stream().collect(Collectors.toMap(JarInfo::name, Function.identity()));
    assertEquals(
        "fontbox-2.0.27.jar",
        jars.getOrDefault("fontbox-2.0.27.jar", new JarInfo("dummy", "0.0.0", new HashMap<>()))
            .name());
    assertEquals(
        "numberguess.war/WEB-INF/lib/fontbox-2.0.27.jar",
        jars.getOrDefault("fontbox-2.0.27.jar", new JarInfo("dummy", "0.0.0", new HashMap<>()))
            .attributes()
            .get("path"));
    assertEquals(
        "d08c064d18b2b149da937d15c0d1708cba03f29d",
        jars.getOrDefault("fontbox-2.0.27.jar", new JarInfo("dummy", "0.0.0", new HashMap<>()))
            .attributes()
            .get(JarAnalyzer.SHA1_CHECKSUM_KEY));
    assertEquals(
        "dc7429868aaf3d313c524b9aab846a405e89ca4927f35762ca4d1a60bce1d7f4",
        jars.getOrDefault("fontbox-2.0.27.jar", new JarInfo("dummy", "0.0.0", new HashMap<>()))
            .attributes()
            .get(JarAnalyzer.SHA256_CHECKSUM_KEY));
    assertEquals(
        "38c8a1edb8c1c92a598c82fc7dd283c50feeac975ef75513c8d048e8c4b41b3ec2c941688b5fc54b8dee461edfd1174ae8457f48f5e3c365065810b3623e90e8",
        jars.getOrDefault("fontbox-2.0.27.jar", new JarInfo("dummy", "0.0.0", new HashMap<>()))
            .attributes()
            .get(JarAnalyzer.SHA512_CHECKSUM_KEY));
    assertEquals(
        "pdfbox-2.0.27.jar",
        jars.getOrDefault("pdfbox-2.0.27.jar", new JarInfo("dummy", "0.0.0", new HashMap<>()))
            .name());
    assertEquals(
        "numberguess.war/WEB-INF/lib/fontbox-2.0.27.jar",
        jars.getOrDefault("fontbox-2.0.27.jar", new JarInfo("dummy", "0.0.0", new HashMap<>()))
            .attributes()
            .get("path"));
    assertEquals(
        "a25ad2a0be6b0bf9eb0e972abd09c34c0e797a3ce2a980d5ff035ff4cf078037",
        jars.getOrDefault("pdfbox-2.0.27.jar", new JarInfo("dummy", "0.0.0", new HashMap<>()))
            .attributes()
            .get(JarAnalyzer.SHA256_CHECKSUM_KEY));
    assertEquals(
        "e49f3c7c8d58d9aeecebcce052ec5915de4e67f9f81f5a23c295127d284e7ac69c74a0239d3b1db2d65e23f3d1d8a0baf39645dfa1f85c1955cfaca04b0128b6",
        jars.getOrDefault("pdfbox-2.0.27.jar", new JarInfo("dummy", "0.0.0", new HashMap<>()))
            .attributes()
            .get(JarAnalyzer.SHA512_CHECKSUM_KEY));
    assertEquals(
        "commons-logging-1.2.jar",
        jars.getOrDefault("commons-logging-1.2.jar", new JarInfo("dummy", "0.0.0", new HashMap<>()))
            .name());
    assertEquals(
        "numberguess.war/WEB-INF/lib/commons-logging-1.2.jar",
        jars.getOrDefault("commons-logging-1.2.jar", new JarInfo("dummy", "0.0.0", new HashMap<>()))
            .attributes()
            .get("path"));
    assertEquals(
        "daddea1ea0be0f56978ab3006b8ac92834afeefbd9b7e4e6316fca57df0fa636",
        jars.getOrDefault("commons-logging-1.2.jar", new JarInfo("dummy", "0.0.0", new HashMap<>()))
            .attributes()
            .get(JarAnalyzer.SHA256_CHECKSUM_KEY));
    assertEquals(
        "ed00dbfabd9ae00efa26dd400983601d076fe36408b7d6520084b447e5d1fa527ce65bd6afdcb58506c3a808323d28e88f26cb99c6f5db9ff64f6525ecdfa557",
        jars.getOrDefault("commons-logging-1.2.jar", new JarInfo("dummy", "0.0.0", new HashMap<>()))
            .attributes()
            .get(JarAnalyzer.SHA512_CHECKSUM_KEY));
    deployment =
        Paths.get("target", "test-classes", "com", "redhat", "insights", "jars", "jarTest.jar");
    result = instance.listDeploymentContent(analyzer, tempDir, parentName, deployment);
    assertNotNull(result);
    assertEquals(0, result.size());
    instance.deleteSilentlyRecursively(tempDir);
  }

  /** Test of listingRequired method, of class RecursiveJarAnalyzerHelper. */
  @Test
  public void testListingRequired() throws Exception {
    Path zip =
        Paths.get("target", "test-classes", "com", "redhat", "insights", "jars", "numberguess.war");
    boolean result = RecursiveJarAnalyzerHelper.listingRequired(zip);
    assertEquals(true, result, zip.toAbsolutePath() + " has embedded archive");
    zip = Paths.get("target", "test-classes", "com", "redhat", "insights", "jars", "jarTest.jar");
    result = RecursiveJarAnalyzerHelper.listingRequired(zip);
    assertEquals(false, result, zip.toAbsolutePath() + " has no embedded archive");
  }

  /** Test of isArchive method, of class RecursiveJarAnalyzerHelper. */
  @Test
  public void testIsArchive_Path() throws Exception {
    Path zip =
        Paths.get("target", "test-classes", "com", "redhat", "insights", "jars", "numberguess.war");
    boolean result = RecursiveJarAnalyzerHelper.isArchive(zip);
    assertEquals(true, result, zip.toAbsolutePath() + " is an archive");
    zip = Paths.get("target", "test-classes", "com", "redhat", "insights", "jars", "jarTest.jar");
    result = RecursiveJarAnalyzerHelper.isArchive(zip);
    assertEquals(true, result, zip.toAbsolutePath() + " is an archive");
  }

  /** Test of isArchive method, of class RecursiveJarAnalyzerHelper. */
  @Test
  public void testIsArchive_InputStream() throws Exception {
    Path zip =
        Paths.get("target", "test-classes", "com", "redhat", "insights", "jars", "numberguess.war");
    try (InputStream in = Files.newInputStream(zip)) {
      boolean result = RecursiveJarAnalyzerHelper.isArchive(in);
      assertEquals(true, result, zip.toAbsolutePath() + " is an archive");
    }
    zip = Paths.get("target", "test-classes", "com", "redhat", "insights", "jars", "jarTest.jar");
    try (InputStream in = Files.newInputStream(zip)) {
      boolean result = RecursiveJarAnalyzerHelper.isArchive(in);
      assertEquals(true, result, zip.toAbsolutePath() + " is an archive");
    }
  }
}
