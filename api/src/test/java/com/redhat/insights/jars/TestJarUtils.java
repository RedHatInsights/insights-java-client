/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.jars;

import static com.redhat.insights.jars.JarUtils.*;
import static com.redhat.insights.jars.TestJarAnalyzer.JAR_PATH;
import static com.redhat.insights.jars.TestJarAnalyzer.JAR_PATH_2;
import static com.redhat.insights.jars.TestJarAnalyzer.getURL;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import org.junit.jupiter.api.Test;

public class TestJarUtils {

  @Test
  public void basicHashTest() throws IOException, NoSuchAlgorithmException {
    try (InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream(JAR_PATH);
        InputStream is2 = ClassLoader.getSystemClassLoader().getResourceAsStream(JAR_PATH_2)) {

      assertNotEquals(computeSha512(readAllBytes(is)), computeSha512(readAllBytes(is2)));
    }
  }

  @Test
  public void basicCheckJars() throws NoSuchAlgorithmException, IOException, URISyntaxException {
    URL jar1URL = getURL(JAR_PATH);
    URL jar2URL = getURL(JAR_PATH_2);

    assertNotEquals(computeSha256(jar1URL), computeSha256(jar2URL));
  }

  private byte[] readAllBytes(InputStream inputStream) throws IOException {
    try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
      int nRead;
      byte[] data = new byte[4096];
      while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
        buffer.write(data, 0, nRead);
      }
      return buffer.toByteArray();
    }
  }

  @Test
  public void basicCheckJarUrls() throws NoSuchAlgorithmException, IOException, URISyntaxException {
    String file = "jar:" + getURL(JAR_PATH).toExternalForm() + "!/";
    try {
      computeSha(new URL(file));
    } catch (Exception ex) {
      fail(ex);
    }
    file =
        "jar:file:"
            + new File(System.getProperty("basedir")).toPath().toAbsolutePath().toString()
            + "/target/test-classes/com/redhat/insights/jars/sti-sb-tomcat.jar!/BOOT-INF/lib/jackson-databind-2.13.5.jar!/";
    try {
      getInputStream(new URL(file));
    } catch (Exception ex) {
      fail(ex);
    }
    file =
        "jar:file:"
            + new File(System.getProperty("basedir")).toPath().toAbsolutePath().toString()
            + "/target/test-classes/com/redhat/insights/jars/sti-sb-tomcat.jar!/BOOT-INF/classes!/";
    try {
      getInputStream(new URL(file));
    } catch (Exception ex) {
      fail(ex);
    }
  }
}
