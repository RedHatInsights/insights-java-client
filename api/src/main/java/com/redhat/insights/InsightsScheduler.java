/* Copyright (C) Red Hat 2023-2024 */
package com.redhat.insights;

import java.util.List;
import java.util.concurrent.ScheduledFuture;
import org.jspecify.annotations.NullMarked;

/** Interface for scheduling {@code CONNECT} and {@code UPDATE} events. */
@NullMarked
public interface InsightsScheduler {

  /**
   * Schedule a {@code CONNECT} event.
   *
   * @param command the command that sends a {@code CONNECT} event
   * @return a {@link ScheduledFuture} for the command completion
   */
  public ScheduledFuture<?> scheduleConnect(Runnable command);

  /**
   * Schedule a {@code UPDATE} event.
   *
   * @param command the command that sends a {@code UPDATE} event
   * @return a {@link ScheduledFuture} for the command completion
   */
  public ScheduledFuture<?> scheduleJarUpdate(Runnable command);

  public boolean isShutdown();

  public void shutdown();

  public List<Runnable> shutdownNow();
}
