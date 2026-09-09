/* Copyright (C) Red Hat 2023-2026 */
package com.redhat.insights.http;

import static org.junit.jupiter.api.Assertions.*;

import com.redhat.insights.InsightsException;
import com.redhat.insights.logging.PrintLogger;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

// NonRetryableInsightsException is in the same package — no import needed for same-package use,
// but the InsightsErrorCode reference below uses its fully qualified name.

class BackoffWrapperTest {

  @Test
  void notFailingAction() {
    PrintLogger logger = PrintLogger.STDOUT_LOGGER;
    BackoffWrapper backoff = new BackoffWrapper(logger, 100L, 2L, 3, () -> {});
    assertEquals(0, backoff.run());
  }

  @Test
  void alwaysFailingAction() {
    long start = System.currentTimeMillis();
    PrintLogger logger = PrintLogger.STDOUT_LOGGER;
    BackoffWrapper backoff =
        new BackoffWrapper(
            logger,
            10L,
            2L,
            3,
            () -> {
              throw new IOException("fail");
            });
    InsightsException err = assertThrows(InsightsException.class, backoff::run);
    Throwable[] suppressed = err.getSuppressed();
    assertEquals(3, suppressed.length);
    assertInstanceOf(IOException.class, suppressed[0]);
    assertEquals("fail", suppressed[0].getMessage());
    assertInstanceOf(IOException.class, suppressed[1]);
    assertEquals("fail", suppressed[1].getMessage());
    assertInstanceOf(IOException.class, suppressed[2]);
    assertEquals("fail", suppressed[2].getMessage());
    assertTrue(System.currentTimeMillis() - start >= 70L);
  }

  @Test
  void nonRetryableActionIsNotRetried() {
    // ! Fix 4: verifies that an action throwing a NonRetryable exception bypasses the backoff
    // ! loop immediately — no sleep, no retry budget consumed.
    AtomicInteger count = new AtomicInteger(0);
    PrintLogger logger = PrintLogger.STDOUT_LOGGER;
    BackoffWrapper backoff =
        new BackoffWrapper(
            logger,
            10_000L, // large delay so any retry would be obvious
            2L,
            5,
            () -> {
              count.incrementAndGet();
              throw new NonRetryableInsightsException(
                  com.redhat.insights.InsightsErrorCode.ERROR_HTTP_SEND_AUTH_ERROR,
                  "Authentication missing from request");
            });
    RuntimeException err = assertThrows(RuntimeException.class, backoff::run);
    assertInstanceOf(NonRetryableInsightsException.class, err);
    assertEquals(1, count.get(), "Action must be called exactly once — no retries");
  }

  @Test
  void eventuallySucceedingAction() {
    long start = System.currentTimeMillis();
    AtomicInteger count = new AtomicInteger(0);
    PrintLogger logger = PrintLogger.STDOUT_LOGGER;
    BackoffWrapper backoff =
        new BackoffWrapper(
            logger,
            10L,
            2L,
            10,
            () -> {
              if (count.getAndIncrement() != 2) {
                throw new IOException("fail");
              }
            });
    assertEquals(2, backoff.run());
    assertTrue(System.currentTimeMillis() - start >= 30L);
  }
}
