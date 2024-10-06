/* Copyright (C) Red Hat 2022-2024 */
package com.redhat.insights.reports;

import com.fasterxml.jackson.databind.JsonSerializer;
import com.redhat.insights.Filtering;
import com.redhat.insights.config.InsightsConfiguration;
import com.redhat.insights.logging.InsightsLogger;
import java.io.IOException;
import java.lang.management.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * Base class that collects basic information and delegates to the subreport for product-specific
 * details.
 *
 * <p>Products that want to provide Insights reports will subclass this class and also provide a
 * subreport class and a serializer.
 */
public abstract class AbstractTopLevelReportBase implements InsightsReport {
  private static final int BYTES_PER_KB = 1024;
  private static final int BYTES_PER_MB = 1048576;
  private static final int BYTES_PER_GB = 1073741824;

  private static final Pattern JSON_WORKAROUND = Pattern.compile("\\\\+$");

  private static final Set<String> allowKeys;
  private static final Map<String, String> renameKeys;

  static {
    allowKeys =
        Stream.of(
                "http.nonProxyHosts",
                "java.class.path",
                "java.class.version",
                "java.home",
                "java.library.path",
                "java.runtime.version",
                "java.specification.vendor",
                "java.specification.version",
                "java.vendor",
                "java.vendor.version",
                "java.version",
                "java.vm.name",
                "java.vm.specification.vendor",
                "java.vm.specification.version",
                "java.vm.vendor",
                "jvm.args",
                "jvm.heap.gc.details",
                "jvm.heap.max",
                "jvm.heap.min",
                "jvm.report_time",
                "jvm.packages",
                "jvm.pid",
                "sun.java.command",
                "system.arch",
                "system.cores.logical",
                "system.hostname",
                "system.os.name",
                "system.os.version",
                "user.dir",
                "user.name")
            .collect(Collectors.toCollection(HashSet::new));

    // Massage a few names into proper namespaces
    Map<String, String> tmpRenames = new HashMap<>();
    tmpRenames.put("http.nonProxyHosts", "jvm.http.nonProxyHosts");
    tmpRenames.put("sun.java.command", "java.command");
    tmpRenames.put("user.dir", "app.user.dir");
    tmpRenames.put("user.name", "app.user.name");

    renameKeys = tmpRenames;
  }

  // This is non-final because we want to apply global filtering to this value as the final step
  private Map<String, Object> options = new HashMap<>();

  private final Map<String, InsightsSubreport> subReports;
  private final JsonSerializer<InsightsReport> serializer;
  private final InsightsLogger logger;
  private final InsightsConfiguration config;
  private byte @Nullable [] subReport;

  // Can't be set properly until after report has been generated
  private String idHash = "";

  public AbstractTopLevelReportBase(
      InsightsLogger logger,
      InsightsConfiguration config,
      Map<String, InsightsSubreport> subReports) {
    this.config = config;
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
    // Should we skip report generation if idHash is already set?
    // The invariant here is that idHash must be the same between CONNECT events from the
    // same long-lived process

    ///////////////// Core Java Properties Details

    Properties properties = System.getProperties();
    Map<String, String> stringProperties = new HashMap<>();
    properties.forEach(
        (k, v) -> {
          String key = k.toString();
          if (allowKeys.contains(key)) {
            if (renameKeys.containsKey(key)) {
              stringProperties.put(renameKeys.get(key), v.toString());
            } else {
              stringProperties.put(key, v.toString());
            }
          }
        });
    options.putAll(stringProperties);

    ///////////////// App and System Details

    // This is the top-level name - implementors that may have to care about multi-tenancy
    // should handle that in product-specific code.
    final String name = getIdentificationName();
    options.put("app.name", name);

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

    OperatingSystemMXBean systemMXBean = ManagementFactory.getOperatingSystemMXBean();
    options.put("system.cores.logical", systemMXBean.getAvailableProcessors());
    options.put("system.arch", systemMXBean.getArch());
    options.put("system.os.version", systemMXBean.getVersion());
    options.put("system.os.name", systemMXBean.getName());

    ///////////////// JVM Details

    options.put("jvm.report_time", System.currentTimeMillis());
    options.put("jvm.pid", getProcessPID());

    RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
    List<String> inputArguments = fixInputArguments(runtimeMXBean.getInputArguments());
    options.put("jvm.args", String.join(" ", inputArguments));

    MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
    long heapMin = heapMemoryUsage.getInit();
    long heapMax = heapMemoryUsage.getMax();

    // Check for -Xmn and -Xmx options on the command line
    for (String arg : inputArguments) {
      if (arg.startsWith("-Xms")) {
        heapMin = getMemorySize(arg.substring(4)).orElse(heapMin);
      } else if (arg.startsWith("-Xmx")) {
        heapMax = getMemorySize(arg.substring(4)).orElse(heapMax);
      }
    }

    options.put("jvm.heap.min", heapMin / BYTES_PER_MB);
    options.put("jvm.heap.max", heapMax / BYTES_PER_MB);

    List<GarbageCollectorMXBean> gcMxBeans = ManagementFactory.getGarbageCollectorMXBeans();
    StringBuilder gcDetails = new StringBuilder("gc");
    for (GarbageCollectorMXBean gcMxBean : gcMxBeans) {
      gcDetails.append("::").append(gcMxBean.getName()); // gcMxBean.getObjectName()
    }
    options.put("jvm.heap.gc.details", gcDetails.toString());

    Package[] packages = getPackages();
    // This is Object[], not String[] b/c toArray() is not a generic method
    Object[] out = Arrays.stream(packages).map(Package::toString).toArray();
    options.put("jvm.packages", Arrays.toString(out));

    for (InsightsSubreport subReport : subReports.values()) {
      subReport.generateReport();
    }

    // Final step - apply filtering
    options = masking.apply(options);
  }

  protected abstract long getProcessPID();

  protected abstract Package[] getPackages();

  @Override
  public byte[] getSubModulesReport() {
    if (subReport == null) {
      subReport = InsightsReport.super.getSubModulesReport();
    }
    return subReport;
  }

  @Override
  public void close() throws IOException {
    subReport = null;
  }

  /**
   * This is the top-level name - implementors that may have to care about multi-tenancy should
   * handle that in product-specific code.
   *
   * @return an id of the current application.
   */
  protected String getIdentificationName() {
    return config.getIdentificationName();
  }

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

  static OptionalLong getMemorySize(String size) {
    if (size == null) {
      return OptionalLong.empty();
    }
    try {
      if (size.endsWith("k") || size.endsWith("K")) {
        return OptionalLong.of(Long.parseLong(size.substring(0, size.length() - 1)) * BYTES_PER_KB);
      } else if (size.endsWith("m") || size.endsWith("M")) {
        return OptionalLong.of(Long.parseLong(size.substring(0, size.length() - 1)) * BYTES_PER_MB);
      } else if (size.endsWith("g") || size.endsWith("G")) {
        return OptionalLong.of(Long.parseLong(size.substring(0, size.length() - 1)) * BYTES_PER_GB);
      } else {
        return OptionalLong.of(Long.parseLong(size));
      }
    } catch (NumberFormatException e) {
      return OptionalLong.empty();
    }
  }

  @Override
  public String getVersion() {
    return "1.0.1";
  }
}
