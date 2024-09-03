/* Copyright (C) Red Hat 2023-2024 */
package com.redhat.insights;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.redhat.insights.jars.JarInfo;
import com.redhat.insights.reports.UpdateReportImpl;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import org.junit.jupiter.api.Test;

public class TestUpdateReport extends AbstractReportTest {

  @Test
  public void testSupplementaryMethods() {
    BlockingQueue<JarInfo> blockingQueue = new ArrayBlockingQueue<>(10);
    UpdateReportImpl updateReport = new UpdateReportImpl(blockingQueue, logger);

    assertTrue(
        updateReport.getSubreports().isEmpty(), "There should be no subreports in empty report");
    assertTrue(
        updateReport.getBasic().isEmpty(), "There should be no basic report in update report");
    assertTrue(validateVersion(updateReport.getVersion()), "Report's version should be valid");

    assertTrue(
        updateReport.getIdHash() == null || updateReport.getIdHash().isEmpty(),
        "Default idHash should be empty in Update report");

    String idHash = "randomHash";
    updateReport.setIdHash(idHash);
    assertEquals(idHash, updateReport.getIdHash(), "IdHash should have the set value");
  }

  @Test
  public void testEmptyQueue() throws IOException {
    BlockingQueue<JarInfo> blockingQueue = new ArrayBlockingQueue<>(10);

    UpdateReportImpl updateReport = new UpdateReportImpl(blockingQueue, logger);
    String report = generateReport(updateReport);
    Map<?, ?> parsedReport = parseReport(report);

    // there should be only version field
    assertEquals(1, parsedReport.size(), "Empty update report should have 1 field - \"version\"");

    assertTrue(parsedReport.containsKey("version"), "There should be version field");
    assertTrue(parsedReport.get("version") instanceof String, "Version should be String");
    assertTrue(validateVersion((String) parsedReport.get("version")), "Version should be valid");
  }

  @Test
  public void testFilledQueue() throws IOException {
    String idHash = "Lorem ipsum dolor sit amet";
    BlockingQueue<JarInfo> blockingQueue = new ArrayBlockingQueue<>(10);

    Map<String, String> attrs = new HashMap<>();
    attrs.put("attr1", "value1");
    attrs.put("attr2", "value2 \t \t ");

    JarInfo jarInfoWithoutAttrs = new JarInfo("RandomName", "0.9", Collections.emptyMap());
    JarInfo jarInfoWithAttrs = new JarInfo("DifferentName", "0.1", attrs);

    blockingQueue.add(jarInfoWithoutAttrs);
    blockingQueue.add(jarInfoWithAttrs);
    blockingQueue.add(JarInfo.MISSING);

    UpdateReportImpl updateReport = new UpdateReportImpl(blockingQueue, logger);
    updateReport.setIdHash(idHash);

    String report = generateReport(updateReport);

    Map<?, ?> parsedReport = parseReport(report);

    // report should have 3 field - version, idHash and "updated-jars"
    assertEquals(
        3,
        parsedReport.size(),
        "Updated report should have 3 field - version, idHash and updated-jars");

    // check version
    assertTrue(parsedReport.containsKey("version"), "There should be field version in report");
    assertTrue(validateVersion((String) parsedReport.get("version")), "Version should be valid");

    // check idHash
    assertTrue(parsedReport.containsKey("idHash"), "There should be field idHash in the report");
    assertEquals(idHash, parsedReport.get("idHash"), "IdHash should be the set one");

    // check updated-jars
    assertTrue(
        parsedReport.containsKey("updated-jars"),
        "There should be field \"updated-jars\" in the report");
    Map<?, ?> updatedJars = (Map<?, ?>) parsedReport.get("updated-jars");

    // check nested version
    assertTrue(updatedJars.containsKey("version"), "Updated-jars should have version");
    assertTrue(
        validateVersion((String) updatedJars.get("version")),
        "Updated-jars' version should be valid");

    // check jars
    assertTrue(
        updatedJars.containsKey("jars"), "There should be field \"jars\" in the update-jars");
    List<Map<?, ?>> jars = (List<Map<?, ?>>) updatedJars.get("jars");

    // check jars name
    assertEquals("RandomName", jars.get(0).get("name"), "Jars name should be same as set one");
    assertEquals("DifferentName", jars.get(1).get("name"), "Jars name should be same as set one");
    assertTrue(
        ((String) jars.get(2).get("name")).contains("unknown"),
        "Name of missing jar should contain \"unknown\"");
  }
}
