/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.jars;

import com.redhat.insights.logging.InsightsLogger;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

/**
 * Analyzes individual jars, computes SHA hashes to fingerprint jars. Attempts to open individual
 * jars and obtain more detail from manifests.
 */
public final class JarAnalyzer {
  public static final String SHA1_CHECKSUM_KEY = "sha1Checksum";
  public static final String SHA256_CHECKSUM_KEY = "sha256Checksum";
  public static final String SHA512_CHECKSUM_KEY = "sha512Checksum";
  private static final String JAR_EXTENSION = ".jar";
  static final String UNKNOWN_VERSION = " ";

  @SuppressWarnings("deprecation") // Implementation-Vendor-Id is deprecated
  private static final String[] ATTRIBUTES_TO_COLLECT =
      new String[] {
        Attributes.Name.IMPLEMENTATION_VENDOR.toString(),
        Attributes.Name.IMPLEMENTATION_VENDOR_ID.toString()
      };

  private final InsightsLogger logger;
  private final boolean skipTempJars;
  private final List<String> ignoreJars;

  public JarAnalyzer(InsightsLogger logger, boolean skipTempJars) {
    this.logger = logger;
    this.skipTempJars = skipTempJars;
    if (!skipTempJars) {
      logger.debug("Temporary jars will be transmitted to the host");
    }
    // FIXME Config jars to ignore
    ignoreJars = new ArrayList<>();
  }

  public Optional<JarInfo> process(URL url) throws URISyntaxException {
    String jarFile = parseJarName(url);
    return process(jarFile, url);
  }

  public Optional<JarInfo> process(String file, URL url) throws URISyntaxException {
    if (skipTempJars && isTempFile(url)) {
      logger.debug(url + " Skipping temp jar file");
      return Optional.empty();
    }
    if (isModularJdkJar(url)) {
      logger.debug(url + " Skipping JDK jar file");
      return Optional.empty();
    }
    String archive = file.toLowerCase(Locale.ROOT);
    if (!archive.endsWith(".zip")
        && !archive.endsWith(".jar")
        && !archive.endsWith(".war")
        && !archive.endsWith(".rar")
        && !archive.endsWith(".ear")
        && !"content".equals(archive)) {
      logger.debug(url + " Skipping file with non-jar extension");
      return Optional.empty();
    }

    if (shouldAttemptAdd(file)) {
      logger.debug(url + "  Adding the file " + archive + " with version");
      return Optional.of(getJarInfoSafe(file, url));
    }

    return Optional.empty();
  }

  /**
   * Returns true if the address protocol is "file" and the file resides within the temp directory.
   */
  static boolean isTempFile(URL address) throws URISyntaxException {
    if (!"file".equals(address.getProtocol())) {
      return false;
    }

    return isTempFile(new File(address.toURI()));
  }

  private static final File TEMP_DIRECTORY = new File(System.getProperty("java.io.tmpdir"));

  static boolean isTempFile(File fileParam) {
    File file = fileParam;
    while (file != null) {
      file = file.getParentFile();
      if (TEMP_DIRECTORY.equals(file)) {
        return true;
      }
    }

    return false;
  }

  JarInfo getJarInfoSafe(String jarFile, URL url) {
    Map<String, String> attributes = new HashMap<>();

    try {
      String sha1Checksum = JarUtils.computeSha1(url);
      attributes.put(SHA1_CHECKSUM_KEY, sha1Checksum);
    } catch (Exception ex) {
      logger.error(url + " Error getting jar file sha1 checksum", ex);
    }

    try {
      String sha256Checksum = JarUtils.computeSha256(url);
      attributes.put(SHA256_CHECKSUM_KEY, sha256Checksum);
    } catch (Exception ex) {
      logger.error(url + " Error getting jar file sha256 checksum", ex);
    }

    try {
      String sha512Checksum = JarUtils.computeSha512(url);
      attributes.put(SHA512_CHECKSUM_KEY, sha512Checksum);
    } catch (Exception ex) {
      logger.error(url + " Error getting jar file sha512 checksum", ex);
    }

    JarInfo jarInfo;
    try {
      jarInfo = getJarInfo(jarFile, url, attributes);
    } catch (Exception e) {
      logger.debug(url + " Trouble getting version from jar: adding jar without version");
      jarInfo = new JarInfo(jarFile, UNKNOWN_VERSION, attributes);
    }

    return jarInfo;
  }

  private JarInfo getJarInfo(String jarFilename, URL url, Map<String, String> attributes)
      throws IOException {
    try (JarInputStream jarInputStream = JarUtils.getJarInputStream(url)) {
      try {
        getExtraAttributes(jarInputStream, attributes);

        Map<String, String> pom = getPom(jarInputStream);

        // if we find exactly one pom, use it
        if (pom != null) {
          attributes.putAll(pom);
          return new JarInfo(jarFilename, pom.get("version"), attributes);
        }
      } catch (Exception ex) {
        logger.error(url + "Exception getting extra attributes or pom", ex);
      }

      String version = getVersion(jarInputStream);
      if (version == null || "".equals(version)) {
        version = UNKNOWN_VERSION;
      }

      return new JarInfo(jarFilename, version, attributes);
    }
  }

  /**
   * Returns the values from pom.properties if this file is found. If multiple pom.properties files
   * are found, return null.
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  private static Map<String, String> getPom(JarInputStream jarFile) throws IOException {
    Map<String, String> pom = null;

    for (JarEntry entry = jarFile.getNextJarEntry();
        entry != null;
        entry = jarFile.getNextJarEntry()) {
      if (entry.getName().startsWith("META-INF/maven")
          && entry.getName().endsWith("pom.properties")) {
        if (pom != null) {
          // we've found multiple pom files. bail!
          return null;
        }
        Properties props = new Properties();
        props.load(jarFile);

        pom = (Map) props;
      }
    }

    return pom;
  }

  static void getExtraAttributes(JarInputStream jarFile, Map<String, String> map) {
    Manifest manifest = jarFile.getManifest();
    if (manifest == null) {
      return;
    }

    Attributes attributes = manifest.getMainAttributes();
    for (String name : ATTRIBUTES_TO_COLLECT) {
      String value = attributes.getValue(name);
      if (null != value) {
        map.put(name, value);
      }
    }
  }

  static String getVersion(JarInputStream jarFile) {
    Manifest manifest = jarFile.getManifest();
    if (manifest == null) {
      return null;
    }

    return JarUtils.getVersionFromManifest(manifest);
  }

  /**
   * Removes the full package from the jar name and also gets rid of spaces. This only needs to be
   * called from addJarAndVersion.
   *
   * @param url The name of the jar. This can be full path.
   * @return The name of the jar to put in the internal map.
   */
  String parseJarName(final URL url) throws URISyntaxException {
    // Skip JDK jars
    if (isModularJdkJar(url)) {
      return "";
    }

    if ("file".equals(url.getProtocol())) {
      File file = new File(url.toURI());
      return file.getName().trim();
    }

    logger.debug("Parsing jar file name " + url);
    String path = url.getFile();
    int end = path.lastIndexOf(JAR_EXTENSION);
    if (end > 0) {
      path = path.substring(0, end);
      int start = path.lastIndexOf(File.separator);
      if (start > -1) {
        return path.substring(start + 1) + JAR_EXTENSION;
      }

      return path + JAR_EXTENSION;
    }

    throw new URISyntaxException(url.getPath(), "Unable to parse the jar file name from a URL");
  }

  private boolean isModularJdkJar(URL url) {
    return "jrt".equals(url.getProtocol());
  }

  /**
   * Returns true if the jar file should be sent to the collector.
   *
   * @param jarFile The name of the jar file.
   * @return True if the jar file should be added, else false.
   */
  private boolean shouldAttemptAdd(final String jarFile) {
    return !ignoreJars.contains(jarFile);
  }
}
