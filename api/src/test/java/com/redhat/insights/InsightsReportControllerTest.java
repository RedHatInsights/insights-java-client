/* Copyright (C) Red Hat 2022-2023 */
package com.redhat.insights;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.redhat.insights.config.InsightsConfiguration;
import com.redhat.insights.doubles.*;
import com.redhat.insights.http.InsightsHttpClient;
import com.redhat.insights.jars.ClasspathJarInfoSubreport;
import com.redhat.insights.jars.JarInfo;
import com.redhat.insights.jars.JarInfoSubreport;
import com.redhat.insights.logging.InsightsLogger;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * @author Emmanuel Hugonnet (c) 2022 Red Hat, Inc.
 * @author Martin Ocenas
 */
public class InsightsReportControllerTest {
  private static final InsightsLogger logger = new NoopInsightsLogger();

  public InsightsReportControllerTest() {}

  /** Test of generateAndSetReportIdHash method, of class InsightsReportController. */
  @Test
  public void testGenerateAndSetReportIdHash() {
    InsightsHttpClient httpClient = new NoopInsightsHttpClient();
    InsightsConfiguration config = MockInsightsConfiguration.ofOptedOut("test_app");
    AtomicInteger count = new AtomicInteger();
    InsightsReport report =
        new DummyTopLevelReport(logger, Collections.emptyMap()) {
          @Override
          public void setIdHash(String idHash) {
            count.incrementAndGet();
            super.setIdHash(idHash);
          }
        };
    // This should be done with a mock (or some sort of intercepting double), but I can't quite see
    // how
    //    InsightsReport report = mock(InsightsReport.class);
    //    when(report.getSerializer()).thenReturn(new InsightsReportSerializer());
    //    when(report.getSubreports()).thenReturn(Collections.emptyMap());
    //    when(report.getBasic()).thenReturn(Collections.singletonMap("test", "value"));
    InsightsReportController instance =
        InsightsReportController.of(logger, config, report, () -> httpClient);
    instance.generateAndSetReportIdHash();
    instance.generateAndSetReportIdHash();
    assertEquals(1, count.get());
    //    verify(report, times(1)).setIdHash(anyString());
  }

  /** Test there are not reports sent, if opted out */
  @Test
  public void testOptedOut() throws InterruptedException {
    StoringInsightsHttpClient httpClient = new StoringInsightsHttpClient();
    InsightsReport report = mock(InsightsReport.class);
    InsightsConfiguration config = MockInsightsConfiguration.ofOptedOut("test_app");

    InsightsReportController instance =
        InsightsReportController.of(logger, config, report, () -> httpClient);

    // when trying to generate report, it should throw an exception, if opted out
    assertThrows(InsightsException.class, instance::generate);

    // give controller some time to send a report, if it wants
    Thread.sleep(TimeUnit.SECONDS.toSeconds(2));
    instance.shutdown();

    assertEquals(0, httpClient.getReportsSent(), "There should be no reports sent, if opted out");
  }

  /** Test what will happen if http client is not ready It should not send any report */
  @Test
  public void testNotReadyHttpClient() throws InterruptedException {
    StoringInsightsHttpClient httpClient = new StoringInsightsHttpClient(false);
    InsightsReport report = mock(InsightsReport.class);
    InsightsConfiguration config = new DefaultConfiguration();

    InsightsReportController instance =
        InsightsReportController.of(logger, config, report, () -> httpClient);
    instance.generate();

    // give controller some time to send a report, if it wants
    Thread.sleep(TimeUnit.SECONDS.toSeconds(2));
    instance.shutdown();

    assertEquals(
        0, httpClient.getReportsSent(), "There should be no reports sent, if client not ready");
  }

  @Test
  public void testConnectReportSent() throws InterruptedException, IOException {
    InsightsReport report = prepareReport();
    InsightsConfiguration config = new DefaultConfiguration();
    StoringInsightsHttpClient httpClient = new StoringInsightsHttpClient();
    BlockingQueue<JarInfo> jarsQueue = new ArrayBlockingQueue<>(10);
    jarsQueue.add(new JarInfo("RandomName", "0.9", Collections.emptyMap()));

    InsightsReportController instance =
        InsightsReportController.of(logger, config, report, () -> httpClient, jarsQueue);
    instance.generate();

    // wait for controller to asynchronously sent report
    Thread.sleep(TimeUnit.SECONDS.toMillis(3));
    instance.shutdown();

    // start checking
    assertTrue(
        instance.isShutdown(), "Controller should be shutdown, after it was forced to shutdown");
    assertEquals(jarsQueue, instance.getJarsToSend(), "JarsToSend should be the same as set");

    assertEquals(1, httpClient.getReportsSent(), "There should be one report sent");
    assertTrue(
        httpClient.getReportFilename().matches("^.*_connect\\.gz"),
        "Report filename should be *_connect.gz");

    Map<?, ?> parsedReport = parseReport(httpClient.getReportContent().serialize());
    assertTrue(parsedReport.containsKey("version"), "Report should have version");
    assertTrue(parsedReport.containsKey("idHash"), "Report should have idHash");
    assertFalse(((String) parsedReport.get("idHash")).isEmpty(), "Report iHash not be empty");
    assertTrue(parsedReport.containsKey("basic"), "Report should have basic");
    assertTrue(parsedReport.containsKey("jarsSubreport"), "Report should have jarsSubreport");
    assertTrue(
        parsedReport.containsKey("classpathSubreport"), "Report should have classpathSubreport");
  }

  /** Test that after update period, there should be another report sent */
  @Test
  public void testUpdateReportSent() throws InterruptedException, IOException {
    InsightsReport report = prepareReport();
    InsightsConfiguration config =
        MockInsightsConfiguration.of("test", false, Duration.ofDays(1), Duration.ofSeconds(5));
    StoringInsightsHttpClient httpClient = new StoringInsightsHttpClient();
    BlockingQueue<JarInfo> jarsQueue = new ArrayBlockingQueue<>(10);
    jarsQueue.add(new JarInfo("RandomName", "0.9", Collections.emptyMap()));

    InsightsReportController instance =
        InsightsReportController.of(logger, config, report, () -> httpClient, jarsQueue);
    instance.generate();

    // sleep for time, that multiple update events can be sent
    // but only 1 should be send, because after one, there are no change jars
    Thread.sleep(TimeUnit.SECONDS.toMillis(20));

    // There should be two reports sent - one connect and one update
    assertEquals(2, httpClient.getReportsSent(), "There should be 2 reports sent");
    assertTrue(
        httpClient.getReportFilename().matches("^.*_update\\.gz"),
        "Update report filename should be *_update.gz");

    // validate content of report
    Map<?, ?> parsedReport = parseReport(httpClient.getReportContent().serialize());
    assertTrue(parsedReport.containsKey("version"), "Report should have version");
    assertTrue(parsedReport.containsKey("idHash"), "Report should have idHash");
    assertFalse(((String) parsedReport.get("idHash")).isEmpty(), "Report iHash not be empty");
    assertTrue(parsedReport.containsKey("updated-jars"), "Report should have basic");
  }

  @Test
  public void testMultipleUpdateReportsSent() throws InterruptedException, IOException {
    InsightsReport report = prepareReport();
    InsightsConfiguration config =
        MockInsightsConfiguration.of("test", false, Duration.ofDays(1), Duration.ofSeconds(5));
    StoringInsightsHttpClient httpClient = new StoringInsightsHttpClient();
    BlockingQueue<JarInfo> jarsQueue = new ArrayBlockingQueue<>(10);
    jarsQueue.add(new JarInfo("RandomName", "0.9", Collections.emptyMap()));

    InsightsReportController instance =
        InsightsReportController.of(logger, config, report, () -> httpClient, jarsQueue);
    instance.generate();

    // sleep for a time, that 1 update reports should be sent
    Thread.sleep(TimeUnit.SECONDS.toMillis(8));
    assertEquals(2, httpClient.getReportsSent(), "There should be 2 reports sent");

    // add another jars to report, so there should be another update report send
    String newJarName = "newJarName";
    jarsQueue.add(new JarInfo(newJarName, "1.0", Collections.emptyMap()));

    // give it time to send report, and assert it was sent
    Thread.sleep(TimeUnit.SECONDS.toMillis(10));
    instance.shutdown();
    assertEquals(3, httpClient.getReportsSent(), "There should be 3 reports sent");

    assertTrue(
        httpClient.getReportFilename().matches("^.*_update\\.gz"),
        "Update report filename should be *_update.gz");
    // validate content of last report
    Map<?, ?> parsedReport = parseReport(httpClient.getReportContent().serialize());
    assertTrue(parsedReport.containsKey("version"), "Report should have version");
    assertTrue(parsedReport.containsKey("idHash"), "Report should have idHash");
    assertFalse(((String) parsedReport.get("idHash")).isEmpty(), "Report iHash not be empty");
    assertTrue(parsedReport.containsKey("updated-jars"), "Report should have basic");

    Map<?, ?> updatedJars = (Map<?, ?>) parsedReport.get("updated-jars");

    assertEquals(
        newJarName,
        ((Map<?, ?>) ((List) updatedJars.get("jars")).get(0)).get("name"),
        "Jar in report should have the set name");
  }

  /**
   * Test scenario where multiple reports are sent, and then httpClient is no longer ready to send
   * Simulates that client breaks during sending
   */
  @Test
  public void testHttpClientStopsWorkingDuringUpdate() throws InterruptedException {
    InsightsReport report = prepareReport();
    InsightsConfiguration config =
        MockInsightsConfiguration.of("test", false, Duration.ofDays(1), Duration.ofSeconds(5));
    StoringInsightsHttpClient httpClient = new StoringInsightsHttpClient();
    BlockingQueue<JarInfo> jarsQueue = new ArrayBlockingQueue<>(10);
    jarsQueue.add(new JarInfo("RandomName", "0.9", Collections.emptyMap()));

    InsightsReportController instance =
        InsightsReportController.of(logger, config, report, () -> httpClient, jarsQueue);
    instance.generate();

    // wait for some time, after one update is sent
    Thread.sleep(TimeUnit.SECONDS.toMillis(8));
    int reportsSent = httpClient.getReportsSent();

    // "break" the http client
    httpClient.setReadyToSend(false);
    // Now controller should have not ready client and no jars to send, so it should not send
    // anything

    // after while also add some Jars to queue so controller is tempted to send them
    Thread.sleep(TimeUnit.SECONDS.toMillis(10));
    jarsQueue.add(JarInfo.MISSING);
    jarsQueue.add(new JarInfo("RandomName", "0.9", Collections.emptyMap()));

    // wait for some time, if any reports are sent
    // now controller have not ready http client, but jars to send
    Thread.sleep(TimeUnit.SECONDS.toMillis(10));
    // there should be no more reports sent

    // "fix" the client, and give it some time
    // controller should recover and send the report
    httpClient.setReadyToSend(true);
    Thread.sleep(TimeUnit.SECONDS.toMillis(10));
    instance.shutdown();

    // now there should be another report send
    assertEquals(
        reportsSent + 1,
        httpClient.getReportsSent(),
        "There should be no reports sent, after http client is not ready to send");
  }

  /** Prepare report to be sent */
  private InsightsReport prepareReport() {
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
    return new DummyTopLevelReport(
        logger,
        new HashMap<String, InsightsSubreport>() {
          {
            put("jarsSubreport", jarInfoSubreport);
            put("classpathSubreport", new ClasspathJarInfoSubreport(logger));
          }
        });
  }

  private Map<?, ?> parseReport(String report) throws JsonProcessingException {
    JsonMapper mapper = new JsonMapper();
    return mapper.readValue(report, Map.class);
  }
}
