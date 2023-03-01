/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.jars;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class TestJarInfo {

  @Test
  public void testJarInfoAttributes() {
    String name = "randomName";
    String version = "randomVersion";
    Map<String, String> attributes = Collections.singletonMap("key", "value");

    JarInfo jarInfo = new JarInfo(name, version, attributes);

    assertEquals(name, jarInfo.name(), "Name should match");
    assertEquals(version, jarInfo.version(), "Version should match");
    assertEquals(attributes, jarInfo.attributes(), "Attributes should match");
  }

  @Test
  public void testJarInfoMissing() {
    JarInfo missingJarInfo = JarInfo.MISSING;

    assertTrue(
        missingJarInfo.name().contains("unknown"),
        "Missing jar info name should contain \"unknown\"");
    assertTrue(
        missingJarInfo.version().contains("missing"),
        "Missing jar info version should contain \"missing\"");
    assertTrue(
        missingJarInfo.attributes().isEmpty(), "Missing jar info attributes should be empty");
  }
}
