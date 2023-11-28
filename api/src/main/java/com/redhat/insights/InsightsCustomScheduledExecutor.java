/* Copyright (C) Red Hat 2023 */
package com.redhat.insights;

import static com.redhat.insights.InsightsErrorCode.ERROR_SCHEDULED_SENT;

import com.redhat.insights.config.InsightsConfiguration;
import com.redhat.insights.logging.InsightsLogger;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** A scheduler based on a single-threaded {@link ScheduledThreadPoolExecutor}. */
public class InsightsCustomScheduledExecutor extends ScheduledThreadPoolExecutor
    implements InsightsScheduler {
  private final InsightsLogger logger;
  private final InsightsConfiguration configuration;

  private InsightsCustomScheduledExecutor(
      InsightsLogger logger, InsightsConfiguration configuration) {
    super(1);
    setKeepAliveTime(10L, TimeUnit.MILLISECONDS);
    this.logger = logger;
    this.configuration = configuration;
  }

  public static InsightsCustomScheduledExecutor of(
      InsightsLogger logger, InsightsConfiguration configuration) {
    InsightsCustomScheduledExecutor pool =
        new InsightsCustomScheduledExecutor(logger, configuration);
    pool.allowCoreThreadTimeOut(true);

    return pool;
  }

  @Override
  public ScheduledFuture<?> scheduleConnect(Runnable sendConnect) {
    return scheduleAtFixedRate(
        sendConnect, 0, configuration.getConnectPeriod().getSeconds(), TimeUnit.SECONDS);
  }

  @Override
  public ScheduledFuture<?> scheduleJarUpdate(Runnable sendNewJarsIfAny) {
    return scheduleAtFixedRate(
        sendNewJarsIfAny,
        configuration.getUpdatePeriod().getSeconds(),
        configuration.getUpdatePeriod().getSeconds(),
        TimeUnit.SECONDS);
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(
      Runnable command, long initialDelay, long period, TimeUnit unit) {
    Runnable wrapped =
        () -> {
          try {
            command.run();
          } catch (InsightsException ix) {
            logger.error(
                ERROR_SCHEDULED_SENT.formatMessage(
                    "Red Hat Insights client scheduler shutdown, scheduled send failed: "
                        + ix.getMessage()),
                ix);
            shutdown();
            throw ix;
          } catch (Exception x) {
            logger.error(
                ERROR_SCHEDULED_SENT.formatMessage(
                    "Red Hat Insights client scheduler shutdown, non-Insights failure: "
                        + x.getMessage()),
                x);
            shutdown();
            throw x;
          }
        };

    return super.scheduleAtFixedRate(wrapped, initialDelay, period, unit);
  }
}
