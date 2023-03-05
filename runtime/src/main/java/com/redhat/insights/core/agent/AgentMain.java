/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.core.agent;

import com.redhat.insights.InsightsReportController;
import com.redhat.insights.core.app.AppTopLevelReport;
import com.redhat.insights.core.httpclient.InsightsJdkHttpClient;
import com.redhat.insights.http.InsightsHttpClient;
import com.redhat.insights.jars.JarInfo;
import com.redhat.insights.logging.InsightsLogger;
import com.redhat.insights.logging.JulLogger;
import com.redhat.insights.tls.PEMSupport;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;

public final class AgentMain {
  private static final String DEFAULT_CERT_PATH = "/etc/pki/consumer/cert.pem";
  private static final String DEFAULT_KEY_PATH = "/etc/pki/consumer/key.pem";

  private final InsightsLogger logger;
  private final AgentConfiguration configuration;
  private final BlockingQueue<JarInfo> waitingJars;

  private AgentMain(
      InsightsLogger logger, Map<String, String> args, LinkedBlockingQueue<JarInfo> jarsToSend) {
    this.logger = logger;
    this.configuration = new AgentConfiguration(args);
    this.waitingJars = jarsToSend;
  }

  public static void premain(String agentArgs, Instrumentation instrumentation) {
    InsightsLogger logger = new JulLogger("AgentMain");
    if (agentArgs == null || "".equals(agentArgs)) {
      logger.error("Unable to start Red Hat Insights client: Need config arguments");
      return;
    }
    var oArgs = parseArgs(logger, agentArgs);
    if (oArgs.isEmpty()) {
      return;
    }
    var args = oArgs.get();

    if (args.get(AgentConfiguration.ARG_NAME) == null
        || "".equals(args.get(AgentConfiguration.ARG_NAME))) {
      logger.error(
          "Unable to start Red Hat Insights client: App requires a name for identification");
      return;
    }

    if (args.get(AgentConfiguration.ARG_CERT) == null
        || "".equals(args.get(AgentConfiguration.ARG_CERT))
        || args.get(AgentConfiguration.ARG_KEY) == null
        || "".equals(args.get(AgentConfiguration.ARG_KEY))) {
      var certPath = Path.of(DEFAULT_CERT_PATH);
      var keyPath = Path.of(DEFAULT_KEY_PATH);
      if (Files.exists(certPath) && Files.exists(keyPath)) {
        args.put("cert", DEFAULT_CERT_PATH);
        args.put("key", DEFAULT_KEY_PATH);
      } else {
        logger.error(
            "Unable to start Red Hat Insights client: Missing certificate or key path arguments and"
                + " default locations empty");
        return;
      }
    }

    var jarsToSend = new LinkedBlockingQueue<JarInfo>();
    try {
      logger.info("Starting Red Hat Insights client");
      new AgentMain(logger, args, jarsToSend).start();
      var noticer = new ClassNoticer(logger, jarsToSend);
      instrumentation.addTransformer(noticer);
    } catch (Throwable t) {
      logger.error("Unable to start Red Hat Insights client", t);
      return;
    }
  }

  static Optional<Map<String, String>> parseArgs(InsightsLogger logger, String agentArgs) {
    var out = new HashMap<String, String>();
    for (var pair : agentArgs.split(":")) {
      var kv = pair.split("=");
      if (kv.length != 2) {
        logger.error(
            "Unable to start Red Hat Insights client: Malformed config arguments (should be"
                + " key-value pairs)");
        return Optional.empty();
      }
      out.put(kv[0], kv[1]);
    }
    return Optional.of(out);
  }

  private void start() {
    final var simpleReport = AppTopLevelReport.of(logger, configuration);
    final var pem = new PEMSupport(logger, configuration);

    final Supplier<InsightsHttpClient> httpClientSupplier =
        () -> new InsightsJdkHttpClient(logger, configuration, () -> pem.createTLSContext());
    final var controller =
        InsightsReportController.of(
            logger, configuration, simpleReport, httpClientSupplier, waitingJars);
    controller.generate();
  }
}
