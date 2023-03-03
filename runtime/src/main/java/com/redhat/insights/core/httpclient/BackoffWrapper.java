/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.core.httpclient;

import static com.redhat.insights.InsightsErrorCode.ERROR_CLIENT_BACKOFF_RETRIES_FAILED;
import static com.redhat.insights.InsightsErrorCode.ERROR_INTERRUPTED_THREAD;

import com.redhat.insights.InsightsException;
import com.redhat.insights.config.InsightsConfiguration;
import com.redhat.insights.logging.InsightsLogger;

/**
 * A general-purpose exponential backoff implementation.
 *
 * <p>It wraps an execution that might throw an exception (see {@link Action}). In this case
 * attempts will be retried with the provided parameters (count, initial delay, factor).
 */
public final class BackoffWrapper {

  @FunctionalInterface
  public interface Action {
    void run() throws Throwable;
  }

  private final InsightsLogger logger;
  private final long initialDelay;
  private final long factor;
  private final int max;
  private final Action action;

  BackoffWrapper(InsightsLogger logger, long initialDelay, long factor, int max, Action action) {
    this.logger = logger;
    this.initialDelay = initialDelay;
    this.factor = factor;
    this.max = max;
    this.action = action;
  }

  public BackoffWrapper(InsightsLogger logger, InsightsConfiguration configuration, Action action) {
    this(
        logger,
        configuration.getHttpClientRetryInitialDelay(),
        configuration.getHttpClientRetryBackoffFactor(),
        configuration.getHttpClientRetryMaxAttempts(),
        action);
  }

  public int run() {
    long delay = initialDelay;
    int count = 0;
    InsightsException retryFailure = null;
    while (true) {
      try {
        action.run();
        return count;
      } catch (Throwable err) {
        if (retryFailure == null) {
          retryFailure =
              new InsightsException(
                  ERROR_CLIENT_BACKOFF_RETRIES_FAILED, "Exponential backoff retries have failed");
        }
        retryFailure.addSuppressed(err);
        logger.debug("Backoff #" + (count + 1) + "/" + max + ", sleeping " + delay + "ms", err);
        try {
          Thread.sleep(delay);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new InsightsException(
              ERROR_INTERRUPTED_THREAD, "HTTP sending thread interrupted", e);
        }
        count = count + 1;
        if (count == max) {
          throw retryFailure;
        }
        delay = delay * factor;
      }
    }
  }
}
