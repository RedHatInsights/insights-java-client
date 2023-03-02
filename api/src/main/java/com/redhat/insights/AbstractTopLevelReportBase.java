/* Copyright (C) Red Hat 2022-2023 */
package com.redhat.insights;

import com.fasterxml.jackson.databind.JsonSerializer;
import com.redhat.insights.logging.InsightsLogger;
import java.lang.management.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Base class that collects basic information and delegates to the subreport for product-specific
 * details.
 *
 * <p>Products that want to provide Insights reports will subclass this class and also provide a
 * subreport class and a serializer.
 */
public abstract class AbstractTopLevelReportBase implements InsightsReport {
  private static final int BYTES_PER_MB = 1048576;

  private static final Pattern JSON_WORKAROUND = Pattern.compile("\\\\+$");

  private final Map<String, Object> options = new HashMap<>();

  private final Map<String, InsightsSubreport> subReports;
  private final JsonSerializer<InsightsReport> serializer;
  private final InsightsLogger logger;

  // Can't be set properly until after report has been generated
  private String idHash = "";

  public AbstractTopLevelReportBase(
      InsightsLogger logger, Map<String, InsightsSubreport> subReports) {
    this.logger = logger;
    this.subReports = subReports;
    this.serializer = new InsightsReportSerializer();
  }

  @Override
  public void setIdHash(String idHash) {
    this.idHash = idHash;
  }

  @Override
  public String getIdHash() {
    return idHash;
  }

  @Override
  public Map<String, InsightsSubreport> getSubreports() {
    return Collections.unmodifiableMap(subReports);
  }

  @Override
  public JsonSerializer<InsightsReport> getSerializer() {
    return serializer;
  }

  @Override
  public void decorate(String key, String value) {
    options.put(key, value);
  }

  @Override
  public void generateReport(Filtering masking) {
    // FIXME: Should we skip report generation if idHash is already set?
    // The invariant here is that idHash must be the same between CONNECT events from the
    // same long-lived process

    ///////////////// Core Java Properties Details

    // FIXME Move to a constant when finalized
    Set<String> skipKeys =
        Stream.of(
                "sun.jnu.encoding",
                "sun.arch.data.model",
                "java.vendor.url",
                "sun.java.launcher",
                "user.country",
                "sun.boot.library.path",
                "jdk.debug",
                "sun.cpu.endian",
                "user.home",
                "user.language",
                "sun.stderr.encoding",
                "java.version.date",
                "file.separator",
                "java.vm.compressedOopsMode",
                "sun.stdout.encoding",
                "java.specification.name",
                "sun.management.compiler",
                "path.separator",
                "java.runtime.name",
                "java.vendor.url.bug",
                "java.io.tmpdir",
                "java.vm.specification.name",
                "native.encoding",
                "java.vm.info",
                "gopherProxySet",
                "awt.toolkit",
                "sun.cpu.isalist",
                "ftp.nonProxyHosts",
                "os.version",
                "user.timezone",
                "os.name",
                "java.awt.printerjob",
                "sun.os.patch.level",
                "line.separator",
                "java.awt.graphicsenv",
                "sun.io.unicode.encoding",
                "socksNonProxyHosts")
            .collect(Collectors.toCollection(HashSet::new));

    // What about JVM arguments? PII?
    //    var maybe = List.of("user.dir", "java.library.path");

    Properties properties = System.getProperties();
    Map<String, String> stringProperties = new HashMap<>();
    properties.forEach(
        (k, v) -> {
          String key = k.toString();
          if (!skipKeys.contains(key)) {
            stringProperties.put(key, v.toString());
          }
        });
    options.putAll(stringProperties);

    ///////////////// System Details

    String defaultHost = "localhost";
    try {
      InetAddress host = InetAddress.getLocalHost();
      if (host != null) {
        defaultHost = host.getHostName();
      }
    } catch (UnknownHostException e) {
      logger.error("Unknown Host in lookup, continuing with localhost", e);
    }
    options.put("system.hostname", defaultHost);
    options.put("jvm.launch_time", System.currentTimeMillis());
    //    options.put("system.report_time", LocalDateTime.now());

    OperatingSystemMXBean systemMXBean = ManagementFactory.getOperatingSystemMXBean();
    options.put("system.cores.logical", systemMXBean.getAvailableProcessors());
    options.put("system.arch", systemMXBean.getArch());
    options.put("system.os.version", systemMXBean.getVersion());
    options.put("system.os.name", systemMXBean.getName());

    ///////////////// JVM Details

    long pid = getProcessPID();
    options.put("jvm.pid", pid);

    MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
    options.put("jvm.heap.min", heapMemoryUsage.getInit() / BYTES_PER_MB);
    options.put("jvm.heap.max", heapMemoryUsage.getMax() / BYTES_PER_MB);

    RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
    List<String> inputArguments = fixInputArguments(runtimeMXBean.getInputArguments());
    options.put("jvm.args", inputArguments);

    List<GarbageCollectorMXBean> gcMxBeans = ManagementFactory.getGarbageCollectorMXBeans();
    StringBuilder gcDetails = new StringBuilder("gc");
    for (GarbageCollectorMXBean gcMxBean : gcMxBeans) {
      gcDetails.append("::").append(gcMxBean.getName()); // gcMxBean.getObjectName()
    }
    options.put("jvm.heap.gc.details", gcDetails.toString());

    //        if (instanceName != null) {
    //            options.put("instance.name", instanceName);
    //        }
    //        options.put("app.name", appNames);

    Package[] packages = getPackages();
    // This is Object[], not String[] b/c toArray() is not a generic method
    Object[] out = Arrays.stream(packages).map(Package::toString).toArray();
    options.put("jvm.packages", Arrays.toString(out));

    // Final step - apply filtering
    //    options = masking.apply(options);
    for (InsightsSubreport subReport : subReports.values()) {
      subReport.generateReport();
    }
  }

  protected abstract long getProcessPID();

  protected abstract Package[] getPackages();

  @Override
  public Map<String, Object> getBasic() {
    return Collections.unmodifiableMap(options);
  }

  static List<String> fixInputArguments(List<String> args) {
    List<String> fixed = new ArrayList<>(args.size());
    for (String arg : args) {
      fixed.add(fixString(arg));
    }
    return fixed;
  }

  static String fixString(String arg) {
    Matcher matcher = JSON_WORKAROUND.matcher(arg);
    return matcher.replaceAll("");
  }

  @Override
  public String getVersion() {
    return "1.0.0";
  }
}
