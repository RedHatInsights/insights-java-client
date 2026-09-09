/* Copyright (C) Red Hat 2024-2026 */
package com.redhat.insights;

import static org.junit.jupiter.api.Assertions.*;

import com.redhat.insights.config.InsightsConfiguration;
import com.redhat.insights.doubles.DummyTopLevelReport;
import com.redhat.insights.doubles.MockInsightsConfiguration;
import com.redhat.insights.doubles.NoopInsightsLogger;
import com.redhat.insights.doubles.StoringInsightsHttpClient;
import com.redhat.insights.jars.JarInfo;
import com.redhat.insights.logging.InsightsLogger;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * Tests for InsightsReportController that exercise the scheduled lambdas synchronously by injecting
 * a fake InsightsScheduler that captures and runs tasks inline.
 *
 * <p>Fix 1: the scheduled lambdas in generate() were previously untested because the real
 * InsightsCustomScheduledExecutor runs tasks asynchronously. A synchronous fake scheduler lets us
 * drive each lambda from the test without timing dependencies.
 */
public class InsightsReportControllerSchedulingTest {

  private static final InsightsLogger logger = new NoopInsightsLogger();

  /** A no-op ScheduledFuture returned by the capturing scheduler. */
  @SuppressWarnings("NullAway") // stub — all methods return safe defaults
  static final class NoopScheduledFuture<V> implements ScheduledFuture<V> {
    @Override
    public long getDelay(TimeUnit unit) {
      return 0;
    }

    @Override
    public int compareTo(Delayed o) {
      return 0;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      return false;
    }

    @Override
    public boolean isCancelled() {
      return false;
    }

    @Override
    public boolean isDone() {
      return true;
    }

    @Override
    public @Nullable V get() {
      return null;
    }

    @Override
    public @Nullable V get(long timeout, TimeUnit unit) {
      return null;
    }
  }

  /**
   * A fake scheduler that captures the connect and jar-update runnables and exposes them for
   * synchronous invocation in tests. shutdown() / isShutdown() behave correctly.
   */
  static final class CapturingScheduler implements InsightsScheduler {
    private @Nullable Runnable connectTask = null;
    private @Nullable Runnable jarUpdateTask = null;
    private boolean shutdown = false;

    @Override
    public ScheduledFuture<?> scheduleConnect(Runnable command) {
      this.connectTask = command;
      return new NoopScheduledFuture<>();
    }

    @Override
    public ScheduledFuture<?> scheduleJarUpdate(Runnable command) {
      this.jarUpdateTask = command;
      return new NoopScheduledFuture<>();
    }

    @Override
    public boolean isShutdown() {
      return shutdown;
    }

    @Override
    public void shutdown() {
      shutdown = true;
    }

    @Override
    public List<Runnable> shutdownNow() {
      shutdown = true;
      return Collections.emptyList();
    }

    void runConnect() {
      if (connectTask != null) connectTask.run();
    }

    void runJarUpdate() {
      if (jarUpdateTask != null) jarUpdateTask.run();
    }
  }

  // ── connect task ────────────────────────────────────────────────────────

  @Test
  void connectTask_sendsReportWhenClientIsReady() {
    // ! Fix 1: drives the connect lambda synchronously to verify it calls sendInsightsReport
    // ! with a filename containing the id hash and "_connect" suffix.
    InsightsConfiguration config = MockInsightsConfiguration.of("test_app", false);
    StoringInsightsHttpClient client = new StoringInsightsHttpClient(/* readyToSend= */ true);
    DummyTopLevelReport report = DummyTopLevelReport.of(logger);
    BlockingQueue<JarInfo> jarsToSend = new LinkedBlockingQueue<>();
    CapturingScheduler scheduler = new CapturingScheduler();

    InsightsReportController controller =
        InsightsReportController.of(logger, config, report, () -> client, scheduler, jarsToSend);
    controller.generate();

    scheduler.runConnect();

    assertEquals(1, client.getReportsSent());
    String filename = client.getReportFilename();
    assertNotNull(filename);
    assertTrue(
        filename.endsWith("_connect.txt"),
        "Expected filename to end with '_connect.txt', got: " + filename);
  }

  @Test
  void connectTask_doesNotSendWhenClientIsNotReady() {
    // ! Fix 1: verifies that the connect lambda skips sending when isReadyToSend() is false.
    InsightsConfiguration config = MockInsightsConfiguration.of("test_app", false);
    StoringInsightsHttpClient client = new StoringInsightsHttpClient(/* readyToSend= */ false);
    DummyTopLevelReport report = DummyTopLevelReport.of(logger);
    BlockingQueue<JarInfo> jarsToSend = new LinkedBlockingQueue<>();
    CapturingScheduler scheduler = new CapturingScheduler();

    InsightsReportController controller =
        InsightsReportController.of(logger, config, report, () -> client, scheduler, jarsToSend);
    controller.generate();

    scheduler.runConnect();

    assertEquals(0, client.getReportsSent());
  }

  // ── jar-update task ──────────────────────────────────────────────────────

  @Test
  void jarUpdateTask_sendsUpdateWhenJarsQueued() {
    // ! Fix 1: verifies the jar-update lambda sends exactly one UPDATE report when the queue
    // ! is non-empty after the connect task has established the id hash.
    InsightsConfiguration config = MockInsightsConfiguration.of("test_app", false);
    StoringInsightsHttpClient client = new StoringInsightsHttpClient(/* readyToSend= */ true);
    DummyTopLevelReport report = DummyTopLevelReport.of(logger);
    BlockingQueue<JarInfo> jarsToSend = new LinkedBlockingQueue<>();
    CapturingScheduler scheduler = new CapturingScheduler();

    InsightsReportController controller =
        InsightsReportController.of(logger, config, report, () -> client, scheduler, jarsToSend);
    controller.generate();

    // Connect first to establish the idHash
    scheduler.runConnect();
    int connectCount = client.getReportsSent();

    // Enqueue a jar and run the update task
    jarsToSend.add(new JarInfo("some.jar", "1.0", java.util.Collections.emptyMap()));
    scheduler.runJarUpdate();

    assertEquals(connectCount + 1, client.getReportsSent());
    String filename = client.getReportFilename();
    assertNotNull(filename);
    assertTrue(
        filename.endsWith("_update.txt"),
        "Expected filename to end with '_update.txt', got: " + filename);
  }

  @Test
  void jarUpdateTask_doesNotSendWhenQueueEmpty() {
    // ! Fix 1: verifies the jar-update lambda is a no-op when the jar queue is empty.
    InsightsConfiguration config = MockInsightsConfiguration.of("test_app", false);
    StoringInsightsHttpClient client = new StoringInsightsHttpClient(/* readyToSend= */ true);
    DummyTopLevelReport report = DummyTopLevelReport.of(logger);
    BlockingQueue<JarInfo> jarsToSend = new LinkedBlockingQueue<>();
    CapturingScheduler scheduler = new CapturingScheduler();

    InsightsReportController controller =
        InsightsReportController.of(logger, config, report, () -> client, scheduler, jarsToSend);
    controller.generate();

    scheduler.runConnect();
    int connectCount = client.getReportsSent();

    // Queue is empty — update task must not send
    scheduler.runJarUpdate();

    assertEquals(connectCount, client.getReportsSent());
  }
}
