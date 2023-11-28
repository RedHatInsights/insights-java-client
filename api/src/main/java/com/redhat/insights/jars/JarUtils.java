/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.jars;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
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
  private static final String JAR_PROTOCOL = "jar:";
  private static final String BANG_SEPARATOR = "!/";

  private JarUtils() {}

  private static Map<String, String> getEmbeddedFormatToExtension(String... fileExtensions) {
    Map<String, String> out = new HashMap<>();
    Stream.of(fileExtensions).forEach(ext -> out.put('.' + ext + BANG_SEPARATOR, ext));
    return out;
  }

  /**
   * Open an input stream for the given url. If the url points to a jar within a jar, return an
   * input stream starting at the embedded jar.
   *
   * @param url
   * @return a Stream to the specified URL.
   * @throws IOException
   */
  public static InputStream getInputStream(URL url) throws IOException {
    String jarLocation = url.toExternalForm();
    URL jarURL = url;
    for (Entry<String, String> entry : EMBEDDED_FORMAT_TO_EXTENSION.entrySet()) {
      int index = jarLocation.indexOf(entry.getKey());
      // if the target is the jar file then we can't use openStream as it is there to look into the
      // jar content.
      if (index > 0 && (index + entry.getKey().length()) < jarLocation.length()) {
        String path = url.toExternalForm().substring(index + entry.getKey().length());
        if (path.endsWith(BANG_SEPARATOR)) {
          path = path.substring(0, path.length() - BANG_SEPARATOR.length());
        }
        // add 1 to skip past the `.` and the value length, which is the length of the file
        // extension
        String jar = jarURL.toExternalForm().substring(0, index + 1 + entry.getValue().length());
        if (jarLocation.startsWith(JAR_PROTOCOL)) {
          jar = jar.substring(JAR_PROTOCOL.length());
        }
        jarURL = new URL(jar);
        InputStream inputStream = jarURL.openStream();
        JarInputStream jarStream = new JarInputStream(inputStream);

        if (!readToEntry(jarStream, path)) {
          inputStream.close();
          throw new IOException(
              "Unable to open stream for " + path + " in " + jarURL.toExternalForm());
        }
        return jarStream;
      }
    }
    if (jarLocation.startsWith(JAR_PROTOCOL) && jarLocation.endsWith(BANG_SEPARATOR)) {
      String jarLoc = jarLocation.substring(4, jarLocation.length() - BANG_SEPARATOR.length());
      jarURL = new URL(jarLoc);
    }
    return jarURL.openStream();
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
    String folderPath = path.endsWith("/") ? path : path + '/';
    for (JarEntry jarEntry = null; (jarEntry = jarStream.getNextJarEntry()) != null; ) {
      if (path.equals(jarEntry.getName()) || folderPath.equals(jarEntry.getName())) {
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
    final MessageDigest md = MessageDigest.getInstance(algorithm);
    final int readLen = 128;
    try (final DigestInputStream dis = new DigestInputStream(inputStream, md)) {
      final byte[] readBytes = new byte[readLen];
      while (dis.read(readBytes) != -1) {}
    }
    return toHex(md.digest());
  }

  public static String computeSha(byte[] buffer, String algorithm) throws NoSuchAlgorithmException {
    final MessageDigest md = MessageDigest.getInstance(algorithm);
    return toHex(md.digest(buffer));
  }

  private static String toHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < bytes.length; i++) {
      sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
    }
    return sb.toString();
  }

  public static String[] computeSha(URL url) throws NoSuchAlgorithmException, IOException {
    try (final InputStream inputStream = JarUtils.getInputStream(url)) {
      return computeSha(inputStream);
    }
  }

  public static final String[] computeSha(InputStream inputStream)
      throws NoSuchAlgorithmException, IOException {
    final MessageDigest mdSha1 = MessageDigest.getInstance("SHA1");
    final MessageDigest mdSha256 = MessageDigest.getInstance("SHA-256");
    final MessageDigest mdSha512 = MessageDigest.getInstance("SHA-512");

    try (final DigestOutputStream outSha1 = new DigestOutputStream(nullOutputStream(), mdSha1);
        final DigestOutputStream outSha256 = new DigestOutputStream(nullOutputStream(), mdSha256);
        final DigestOutputStream outSha512 = new DigestOutputStream(nullOutputStream(), mdSha512)) {
      int nRead;
      byte[] data = new byte[4096];
      while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
        outSha1.write(data, 0, nRead);
        outSha256.write(data, 0, nRead);
        outSha512.write(data, 0, nRead);
      }
    }
    return new String[] {
      toHex(mdSha1.digest()), toHex(mdSha256.digest()), toHex(mdSha512.digest())
    };
  }

  public static OutputStream nullOutputStream() {
    return new OutputStream() {
      private volatile boolean closed;

      private void ensureOpen() throws IOException {
        if (closed) {
          throw new IOException("Stream closed");
        }
      }

      @Override
      public void write(int b) throws IOException {
        ensureOpen();
      }

      @Override
      public void write(byte b[], int off, int len) throws IOException {
        ensureOpen();
      }

      @Override
      public void close() {
        closed = true;
      }
    };
  }
}
