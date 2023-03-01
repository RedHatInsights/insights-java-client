/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.core.httpclient;

import static org.junit.jupiter.api.Assertions.*;

import com.redhat.insights.InsightsException;
import com.redhat.insights.logging.PrintLogger;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class BackoffWrapperTest {

  @Test
  void notFailingAction() {
    var logger = PrintLogger.STDOUT_LOGGER;
    var backoff = new BackoffWrapper(logger, 100L, 2L, 3, () -> {});
    assertEquals(0, backoff.run());
  }

  @Test
  void alwaysFailingAction() {
    var start = System.currentTimeMillis();
    var logger = PrintLogger.STDOUT_LOGGER;
    var backoff =
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
  void eventuallySucceedingAction() {
    var start = System.currentTimeMillis();
    var count = new AtomicInteger(0);
    var logger = PrintLogger.STDOUT_LOGGER;
    var backoff =
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
