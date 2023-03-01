/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.jars;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;

public final class JarUtils {
  private static final Map<String, String> EMBEDDED_FORMAT_TO_EXTENSION =
      getEmbeddedFormatToExtension("ear", "war", "jar");
  private static final int DEFAULT_BUFFER_SIZE = 1024 * 8;

  private JarUtils() {}

  private static Map<String, String> getEmbeddedFormatToExtension(String... fileExtensions) {
    Map<String, String> out = new HashMap<>();
    Stream.of(fileExtensions).forEach(ext -> out.put('.' + ext + "!/", ext));
    return out;
  }

  /**
   * Open an input stream for the given url. If the url points to a jar within a jar, return an
   * input stream starting at the embedded jar.
   *
   * @param url
   * @throws IOException
   */
  public static InputStream getInputStream(URL url) throws IOException {

    for (Entry<String, String> entry : EMBEDDED_FORMAT_TO_EXTENSION.entrySet()) {
      int index = url.toExternalForm().indexOf(entry.getKey());
      if (index > 0) {
        String path = url.toExternalForm().substring(index + entry.getKey().length());
        // add 1 to skip past the `.` and the value length, which is the length of the file
        // extension
        url = new URL(url.toExternalForm().substring(0, index + 1 + entry.getValue().length()));
        InputStream inputStream = url.openStream();
        JarInputStream jarStream = new JarInputStream(inputStream);

        if (!readToEntry(jarStream, path)) {
          inputStream.close();
          throw new IOException(
              "Unable to open stream for " + path + " in " + url.toExternalForm());
        }
        return jarStream;
      }
    }

    return url.openStream();
  }

  /**
   * Read a jar input stream until a given path is found.
   *
   * @param jarStream
   * @param path
   * @return true if the path was found
   * @throws IOException
   */
  private static boolean readToEntry(JarInputStream jarStream, String path) throws IOException {
    for (JarEntry jarEntry = null; (jarEntry = jarStream.getNextJarEntry()) != null; ) {
      if (path.equals(jarEntry.getName())) {
        return true;
      }
    }
    return false;
  }

  static JarInputStream getJarInputStream(URL url) throws IOException {
    boolean isEmbedded = isEmbedded(url);
    InputStream is = getInputStream(url);
    if (!isEmbedded && is instanceof JarInputStream) {
      return (JarInputStream) is;
    }
    return new JarInputStream(is);
  }

  private static boolean isEmbedded(URL url) {
    String externalForm = url.toExternalForm();
    for (String prefix : EMBEDDED_FORMAT_TO_EXTENSION.keySet()) {
      if (externalForm.contains(prefix)) {
        return true;
      }
    }

    return false;
  }

  /////////////////////////////////////////////////
  // Handle manifests

  public static String getVersionFromManifest(Manifest manifest) {
    String version = getVersion(manifest.getMainAttributes());
    if (version == null && !manifest.getEntries().isEmpty()) {
      version = getVersion(manifest.getEntries().values().iterator().next());
    }

    return version;
  }

  private static String getVersion(Attributes attributes) {
    String version = attributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
    if (version == null) {
      version = attributes.getValue(Attributes.Name.SPECIFICATION_VERSION);
      if (version == null) {
        // lots of jars specify their version in the Bundle-Version. I believe it's an OSGi thing
        version = attributes.getValue("Bundle-Version");
      }
      if (version == null) {
        // some JDBC drivers specify their version in Driver-Version
        version = attributes.getValue("Driver-Version");
      }
    }
    return version;
  }

  //////////////////////////////////////////
  // Handle hashes / digests

  public static String computeSha1(URL url) throws NoSuchAlgorithmException, IOException {
    return computeSha(url, "SHA1");
  }

  public static String computeSha256(URL url) throws NoSuchAlgorithmException, IOException {
    return computeSha(url, "SHA-256");
  }

  public static String computeSha512(URL url) throws NoSuchAlgorithmException, IOException {
    return computeSha(url, "SHA-512");
  }

  public static String computeSha256(final byte[] buffer)
      throws NoSuchAlgorithmException, IOException {
    return computeSha(buffer, "SHA-256");
  }

  public static String computeSha512(final byte[] buffer)
      throws NoSuchAlgorithmException, IOException {
    return computeSha(buffer, "SHA-512");
  }

  static String computeSha(URL url, String algorithm) throws NoSuchAlgorithmException, IOException {
    final InputStream inputStream = JarUtils.getInputStream(url);
    return computeSha(inputStream, algorithm);
  }

  public static String computeSha(InputStream inputStream, String algorithm)
      throws NoSuchAlgorithmException, IOException {
    try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
      int nRead;
      byte[] data = new byte[8];
      while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
        buffer.write(data, 0, nRead);
      }
      return computeSha(buffer.toByteArray(), algorithm);
    }
  }

  public static String computeSha(byte[] buffer, String algorithm) throws NoSuchAlgorithmException {
    final MessageDigest md = MessageDigest.getInstance(algorithm);

    byte[] bytes = md.digest(buffer);
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < bytes.length; i++) {
      sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
    }

    return sb.toString();
  }
}
