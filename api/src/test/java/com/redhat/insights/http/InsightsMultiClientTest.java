/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.redhat.insights.InsightsException;
import com.redhat.insights.InsightsReport;
import com.redhat.insights.doubles.DummyTopLevelReport;
import com.redhat.insights.doubles.NoopInsightsLogger;
import com.redhat.insights.doubles.StoringInsightsHttpClient;
import com.redhat.insights.logging.InsightsLogger;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

public class InsightsMultiClientTest {
  private static final InsightsLogger logger = new NoopInsightsLogger();

  @Test
  public void testBothClientsOperational() {
    StoringInsightsHttpClient client1 = new StoringInsightsHttpClient();
    StoringInsightsHttpClient client2 = new StoringInsightsHttpClient();
    InsightsReport report = DummyTopLevelReport.of(logger);
    InsightsMultiClient multiClient = new InsightsMultiClient(logger, client1, client2);

    multiClient.sendInsightsReport("", report);

    assertEquals(1, client1.getReportsSent(), "First client should send the report");
    assertEquals(0, client2.getReportsSent(), "Second client should not send the report");
  }

  @Test
  public void testOneClientFailed() {
    InsightsHttpClient client1 = mock(InsightsHttpClient.class);
    doThrow(new InsightsException("failed")).when(client1).sendInsightsReport(anyString(), any());
    StoringInsightsHttpClient client2 = new StoringInsightsHttpClient();

    InsightsReport report = mock(InsightsReport.class);
    AtomicReference<String> decoratedKey = new AtomicReference<>("");
    AtomicReference<String> decoratedValue = new AtomicReference<>("");

    doAnswer(
            invocation -> {
              decoratedKey.set(invocation.getArgument(0));
              decoratedValue.set(invocation.getArgument(1));
              return null;
            })
        .when(report)
        .decorate(any(String.class), any(String.class));

    InsightsMultiClient multiClient = new InsightsMultiClient(logger, client1, client2);
    multiClient.sendInsightsReport("", report);

    assertEquals(1, client2.getReportsSent(), "Second client should send the message");
    assertEquals(
        "client.exception", decoratedKey.get(), "Decoration key should be client.exception");
    assertEquals(
        "failed", decoratedValue.get(), "Decoration message should match exception message");
  }

  @Test
  public void testBothClientsFailed() {
    InsightsHttpClient client1 = mock(InsightsHttpClient.class);
    doThrow(new InsightsException("failed")).when(client1).sendInsightsReport(anyString(), any());
    InsightsHttpClient client2 = mock(InsightsHttpClient.class);
    doThrow(new InsightsException("failed2")).when(client2).sendInsightsReport(anyString(), any());
    InsightsMultiClient multiClient =
        new InsightsMultiClient(logger, Arrays.asList(client1, client2));

    InsightsReport report = DummyTopLevelReport.of(logger);
    // sending report should fail
    assertThrows(
        InsightsException.class,
        () -> multiClient.sendInsightsReport("", report),
        "No client should send the message if both failed");
  }
}
