/* Copyright (C) Red Hat 2022-2024 */
package com.redhat.insights;

import static com.redhat.insights.InsightsErrorCode.ERROR_GENERATING_HASH;
import static com.redhat.insights.InsightsErrorCode.OPT_OUT;
import static com.redhat.insights.http.InsightsHttpClient.gzipReport;
import static com.redhat.insights.jars.JarUtils.computeSha512;

import com.redhat.insights.config.InsightsConfiguration;
import com.redhat.insights.http.InsightsHttpClient;
import com.redhat.insights.jars.JarInfo;
import com.redhat.insights.logging.InsightsLogger;
import com.redhat.insights.reports.InsightsReport;
import com.redhat.insights.reports.UpdateReportImpl;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * The controller class has primarily responsibility for managing the upload of {@code CONNECT} and
 * {@code UPDATE} events.
 *
 * <p>Client code must explicitly manage the lifecycle of the controller object, and shut it down at
 * application exit.
 */
public final class InsightsReportController {

  private final InsightsLogger logger;

  private final InsightsConfiguration configuration;

  private final InsightsReport report;

  private final Supplier<InsightsHttpClient> httpClientSupplier;

  private final InsightsScheduler scheduler;

  private final Filtering masking;

  private final CompletableFuture<String> idHashHolder;

  private final BlockingQueue<JarInfo> jarsToSend;

  private InsightsReportController(
      InsightsLogger logger,
      InsightsConfiguration configuration,
      InsightsReport report,
      Supplier<InsightsHttpClient> httpClientSupplier,
      InsightsScheduler scheduler,
      BlockingQueue<JarInfo> jarsToSend) {
    this.logger = logger;
    this.configuration = configuration;
    this.report = report;
    this.httpClientSupplier = httpClientSupplier;
    this.scheduler = scheduler;
    this.jarsToSend = jarsToSend;

    this.masking = Filtering.DEFAULT;
    this.idHashHolder = new CompletableFuture<>();
  }

  public static InsightsReportController of(
      InsightsLogger logger,
      InsightsConfiguration configuration,
      InsightsReport report,
      Supplier<InsightsHttpClient> httpClientSupplier) {
    return new InsightsReportController(
        logger,
        configuration,
        report,
        httpClientSupplier,
        InsightsCustomScheduledExecutor.of(logger, configuration),
        new LinkedBlockingQueue<>());
  }

  public static InsightsReportController of(
      InsightsLogger logger,
      InsightsConfiguration configuration,
      InsightsReport report,
      Supplier<InsightsHttpClient> httpClientSupplier,
      BlockingQueue<JarInfo> jarsToSend) {
    return new InsightsReportController(
        logger,
        configuration,
        report,
        httpClientSupplier,
        InsightsCustomScheduledExecutor.of(logger, configuration),
        jarsToSend);
  }

  public static InsightsReportController of(
      InsightsLogger logger,
      InsightsConfiguration configuration,
      InsightsReport report,
      Supplier<InsightsHttpClient> httpClientSupplier,
      InsightsScheduler scheduler,
      BlockingQueue<JarInfo> jarsToSend) {
    return new InsightsReportController(
        logger, configuration, report, httpClientSupplier, scheduler, jarsToSend);
  }

  /** Generates the report (including subreports), computes identifying hash and schedules sends */
  public void generate() {
    try {
      if (configuration.isOptingOut()) {
        throw new InsightsException(OPT_OUT, "Opting out of the Red Hat Insights client");
      }
      if (System.getProperty("os.name").toLowerCase().contains("windows")) {
        throw new InsightsException(OPT_OUT, "Red Hat Insights is not supported on Windows.");
      }

      // Schedule initial event
      Runnable sendConnect =
          () -> {
            InsightsHttpClient httpClient = httpClientSupplier.get();
            if (httpClient.isReadyToSend()) {
              generateConnectReport();
              try {
                httpClient.sendInsightsReport(getIdHash() + "_connect", report);
              } finally {
                try {
                  report.close();
                } catch (IOException ioex) {
                  // Nothing to be done there
                }
              }
            } else {
              logger.debug("Insights is not configured to send: " + configuration);
            }
          };
      scheduler.scheduleConnect(sendConnect);

      // Schedule a possible Jar send (every few mins? Defaults to 5 min)
      final InsightsReport updateReport = new UpdateReportImpl(jarsToSend, logger);
      Runnable sendNewJarsIfAny =
          () -> {
            InsightsHttpClient httpClient = httpClientSupplier.get();
            if (httpClient.isReadyToSend() && !jarsToSend.isEmpty()) {
              updateReport.setIdHash(getIdHash());
              updateReport.generateReport(masking);
              httpClient.sendInsightsReport(getIdHash() + "_update", updateReport);
            }
          };
      scheduler.scheduleJarUpdate(sendNewJarsIfAny);

    } catch (InsightsException isx) {
      logger.error(
          "Red Hat Insights client scheduler shutdown due to a controller startup error", isx);
      scheduler.shutdown();
      throw isx;
    }
  }

  void generateConnectReport() {
    report.generateReport(masking);
    generateAndSetReportIdHash();
  }

  /** Forward the shutdown-related calls to the scheduler */
  public void shutdown() {
    scheduler.shutdown();
  }

  public boolean isShutdown() {
    return scheduler.isShutdown();
  }

  /**
   * Compute identifying hash and store it. Note that:
   *
   * <p>1.) the report already contains the report generation time, so we don't need to add a
   * timestamp for uniqueness here. 2.) this method mutates both the controller and report objects
   */
  void generateAndSetReportIdHash() {
    try {
      if (!idHashHolder.isDone()) {
        String hash = computeSha512(gzipReport(report.serializeRaw()));
        idHashHolder.complete(hash);
        report.setIdHash(hash);
      }
    } catch (NoSuchAlgorithmException | IOException x) {
      throw new InsightsException(ERROR_GENERATING_HASH, "Exception when generating ID Hash: ", x);
    }
  }

  String getIdHash() {
    try {
      return idHashHolder.get();
    } catch (InterruptedException | ExecutionException x) {
      throw new InsightsException(
          ERROR_GENERATING_HASH, "Exception while trying to compute ID Hash: ", x);
    }
  }

  public BlockingQueue<JarInfo> getJarsToSend() {
    return jarsToSend;
  }

  public InsightsScheduler getScheduler() {
    return scheduler;
  }

  @Override
  public String toString() {
    final StringBuffer sb = new StringBuffer("InsightsReportController{");
    sb.append("logger=").append(logger);
    sb.append(", configuration=").append(configuration);
    sb.append(", report=").append(report);
    sb.append(", httpClientSupplier=").append(httpClientSupplier);
    sb.append(", scheduler=").append(scheduler);
    sb.append(", masking=").append(masking);
    sb.append(", idHashHolder=").append(idHashHolder);
    sb.append(", jarsToSend=").append(jarsToSend);
    sb.append(", shutdown=").append(isShutdown());
    sb.append('}');
    return sb.toString();
  }
}
