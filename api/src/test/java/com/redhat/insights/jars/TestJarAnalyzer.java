/* Copyright (C) Red Hat 2020-2024 */
package com.redhat.insights.jars;

import static org.junit.jupiter.api.Assertions.*;

import com.redhat.insights.doubles.NoopInsightsLogger;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import javax.servlet.jsp.JspPage;
import org.junit.jupiter.api.Test;

public class TestJarAnalyzer {

  static final String POM_PROPS_JAR_PATH = "com/redhat/insights/jars/pom-props-5.5.3.jar";

  /**
   * The path to the jar. The jar was created with "jar -cvfm jarTest.jar mainfestInfo.txt
   * sample.txt". The implementation-version is 2.0. The specification version is 3.0.
   */
  public static final String JAR_PATH = "com/redhat/insights/jars/jarTest.jar";

  /**
   * Path to a second jar. This jar has an implementation-version of 5.0 and a specification version
   * of 6.0.
   */
  public static final String JAR_PATH_2 = "com/redhat/insights/jars/anotherJar.jar";

  private static final String WAR_PATH = "com/redhat/insights/jars/numberguess.war";

  private static final String EAR_PATH = "com/redhat/insights/jars/earApp.ear";

  private static final String THREADILIZER_PATH = "com/redhat/insights/jars/threadilizer.jar";

  /** Text file which should not be picked up by the agent. */
  private static final String TXT_FILE = "com/redhat/insights/jars/manifestInfo.txt";

  private static final String EMBEDDED_JAR = "com/redhat/insights/jars/nr-cat-testnode-0.1.0.jar";

  private static final String REMOTE_JAR = "https://nonexistent.com/file/app.jar";

  /**
   * Heper method - gets the URL for the given resource.
   *
   * @param path Path starting at test.
   * @return The full path to the file.
   */
  public static URL getURL(String path) {
    return ClassLoader.getSystemClassLoader().getResource(path);
  }

  static URL getJarURLInsideWar() throws IOException {
    File tmpFile = File.createTempFile("embedded_war", ".war");
    tmpFile.deleteOnExit();
    URL embeddedJarURL = getURL(EMBEDDED_JAR);
    try (FileOutputStream out = new FileOutputStream(tmpFile);
        InputStream in = embeddedJarURL.openStream()) {
      int nRead;
      byte[] data = new byte[8];
      while ((nRead = in.read(data, 0, data.length)) != -1) {
        out.write(data, 0, nRead);
      }
    }

    return new URL(tmpFile.toURI().toURL().toExternalForm() + "!/lib/test-jar1-1.2.3.jar");
  }

  static URL getEmbeddedJarURL() throws MalformedURLException {
    return new URL(getURL(EMBEDDED_JAR).toExternalForm() + "!/lib/test-jar1-1.2.3.jar");
  }

  @Test
  public void parseJarNameWithJarProtocol() throws MalformedURLException, URISyntaxException {
    JarAnalyzer target = new JarAnalyzer(new NoopInsightsLogger(), true);
    URL url =
        new URL(
            "jar:file:/Users/sdaubin/servers/jboss-as-7.1.1.Final/modules/org/apache/xerces/main/xercesImpl-2.9.1-jbossas-1.jar!/");
    assertEquals("xercesImpl-2.9.1-jbossas-1.jar", target.parseJarName(url));
  }

  @Test
  public void parseJarNameWithoutJarProtocolCurrentDir()
      throws MalformedURLException, URISyntaxException {
    JarAnalyzer target = new JarAnalyzer(new NoopInsightsLogger(), true);
    URL url = new URL("ftp:xercesImpl-2.9.1-jbossas-1.jar!/");
    assertEquals("xercesImpl-2.9.1-jbossas-1.jar", target.parseJarName(url));
  }

  @Test
  public void parseJarNameWithoutJarProtocolRootDir()
      throws MalformedURLException, URISyntaxException {
    JarAnalyzer target = new JarAnalyzer(new NoopInsightsLogger(), true);
    URL url = new URL("ftp:/xercesImpl-2.9.1-jbossas-1.jar!/");
    assertEquals("xercesImpl-2.9.1-jbossas-1.jar", target.parseJarName(url));
  }

  @Test
  public void testProcessJar1() throws URISyntaxException {
    URL jarURL = ClassLoader.getSystemClassLoader().getResource(JAR_PATH);

    JarAnalyzer task = new JarAnalyzer(new NoopInsightsLogger(), true);
    Optional<JarInfo> oJarInfo = task.process(jarURL);
    assertTrue(oJarInfo.isPresent());
    JarInfo jarInfo = oJarInfo.get();

    assertEquals("jarTest.jar", jarInfo.name());
    assertEquals("2.0", jarInfo.version());
  }

  @Test
  public void testProcessJar2() throws URISyntaxException {
    URL jarURL = getURL(JAR_PATH_2);

    JarAnalyzer task = new JarAnalyzer(new NoopInsightsLogger(), true);
    Optional<JarInfo> oJarInfo = task.process(jarURL);
    assertTrue(oJarInfo.isPresent());
    JarInfo jarInfo = oJarInfo.get();

    assertEquals("anotherJar.jar", jarInfo.name());
    assertEquals("5.0", jarInfo.version());
  }

  /**
   * Create temporary file and try to process it, while skipTempJars is true This file should be
   * skipped
   */
  @Test
  public void testProcessSkipTempJar() throws IOException, URISyntaxException {
    JarAnalyzer analyzer = new JarAnalyzer(new NoopInsightsLogger(), true);
    File temp = File.createTempFile("test", ".jar");
    Optional<JarInfo> result = analyzer.process(temp.toURI().toURL());

    assertFalse(result.isPresent(), "Temp file should be skipped");
  }

  /**
   * Create temporary file and try to process it, while skipTempJars is false File should be
   * processed
   */
  @Test
  public void testProcessTempJar() throws IOException, URISyntaxException {
    JarAnalyzer analyzer = new JarAnalyzer(new NoopInsightsLogger(), false);
    File temp = File.createTempFile("test", ".jar");

    // copy jar archive to the temp file, so it has a valid .jar content
    Files.copy(Paths.get(getURL(JAR_PATH).getPath()), new FileOutputStream(temp));

    // process the file
    Optional<JarInfo> result = analyzer.process(temp.toURI().toURL());
    assertTrue(result.isPresent(), "Temp file should be processed");

    JarInfo jarInfo = result.get();
    assertEquals(temp.getName(), jarInfo.name());
    assertEquals("2.0", jarInfo.version());
  }

  @Test
  public void isNotTemp() throws URISyntaxException {
    assertFalse(JarAnalyzer.isTempFile(getURL(JAR_PATH)));
  }

  /** File accessed via another handler than file: should never be temp */
  @Test
  public void remoteIsNotTemp() throws URISyntaxException, MalformedURLException {
    URL url = new URL(REMOTE_JAR);
    assertFalse(JarAnalyzer.isTempFile(url), "Remote file should not be temp");
  }

  @Test
  public void isTemp() throws IOException {
    File temp = File.createTempFile("test", "dude");
    temp.deleteOnExit();
    assertTrue(JarAnalyzer.isTempFile(temp));
  }

  @Test
  public void testProcessWar() throws URISyntaxException {
    JarAnalyzer analyzer = new JarAnalyzer(new NoopInsightsLogger(), true);
    Optional<JarInfo> oJarInfo = analyzer.process(getURL(WAR_PATH));

    assertTrue(oJarInfo.isPresent());
    JarInfo jarInfo = oJarInfo.get();

    assertEquals("numberguess.war", jarInfo.name());
  }

  @Test
  public void testProcessEar() throws URISyntaxException {
    JarAnalyzer analyzer = new JarAnalyzer(new NoopInsightsLogger(), true);
    Optional<JarInfo> oJarInfo = analyzer.process(getURL(EAR_PATH));

    assertTrue(oJarInfo.isPresent());
    JarInfo jarInfo = oJarInfo.get();

    assertEquals("earApp.ear", jarInfo.name());
    assertEquals("1.0-SNAPSHOT", jarInfo.version());
  }

  @Test
  public void embeddedJar() throws IOException {
    JarAnalyzer target = new JarAnalyzer(new NoopInsightsLogger(), true);
    JarInfo jarInfo = target.getJarInfoSafe("test-jar1-1.2.3.jar", getEmbeddedJarURL());

    assertEquals("1.2.3", jarInfo.version());

    assertEquals(
        "436bdbac7290779a1a89909827d8f24f632e3852",
        jarInfo.attributes().get(JarAnalyzer.SHA1_CHECKSUM_KEY));
    assertEquals(
        "03883c9ae1c3c8fdcb49a29af482b42a3bb600f011783b130cdd08d9a4d84d16",
        jarInfo.attributes().get(JarAnalyzer.SHA256_CHECKSUM_KEY));
    assertEquals(
        "5b1c62a33ea496f13e6b4ae77f9827e5ffc3b9121052e8946475ca02aa0aa65abef4c7d4a7fa1b792950caf11fbd0d8970cb6c4db3d9ca0da89a38a980a5b4ef",
        jarInfo.attributes().get(JarAnalyzer.SHA512_CHECKSUM_KEY));
  }

  @Test
  public void embeddedWar() throws IOException {
    JarAnalyzer target = new JarAnalyzer(new NoopInsightsLogger(), true);
    URL url = getJarURLInsideWar();
    assertTrue(url.toString().contains(".war!/"));
    JarInfo jarInfo = target.getJarInfoSafe("embedded_war.war", url);

    assertEquals("1.2.3", jarInfo.version());

    assertEquals(
        "436bdbac7290779a1a89909827d8f24f632e3852",
        jarInfo.attributes().get(JarAnalyzer.SHA1_CHECKSUM_KEY));
    assertEquals(
        "03883c9ae1c3c8fdcb49a29af482b42a3bb600f011783b130cdd08d9a4d84d16",
        jarInfo.attributes().get(JarAnalyzer.SHA256_CHECKSUM_KEY));
    assertEquals(
        "5b1c62a33ea496f13e6b4ae77f9827e5ffc3b9121052e8946475ca02aa0aa65abef4c7d4a7fa1b792950caf11fbd0d8970cb6c4db3d9ca0da89a38a980a5b4ef",
        jarInfo.attributes().get(JarAnalyzer.SHA512_CHECKSUM_KEY));
  }

  @Test
  public void getJarInfo_withoutPom() {
    JarAnalyzer target = new JarAnalyzer(new NoopInsightsLogger(), true);
    JarInfo jarInfo = target.getJarInfoSafe("jarTest.jar", getURL(JAR_PATH));

    assertEquals(
        "b82b735bc9ddee35c7fe6780d68f4a0256c4bd7a",
        jarInfo.attributes().get(JarAnalyzer.SHA1_CHECKSUM_KEY));
    assertEquals(
        "198e36d43d7bab0aa733688e1290c64c7e616fc322c897c11579b8ee8f2f1e49",
        jarInfo.attributes().get(JarAnalyzer.SHA256_CHECKSUM_KEY));
    assertEquals(
        "7101d4cdd4f68f81411f0150900f6b6cfc0c5547a8fb944daf7f5225033a0598ec93f16cb080e89ca0892362f71700e29a639eb2d750930a53168c43a735e050",
        jarInfo.attributes().get(JarAnalyzer.SHA512_CHECKSUM_KEY));
  }

  @Test
  public void getJarInfo_noVersion() {
    JarAnalyzer target = new JarAnalyzer(new NoopInsightsLogger(), true);
    JarInfo jarInfo = target.getJarInfoSafe("threadilizer.jar", getURL(THREADILIZER_PATH));

    assertEquals(
        "88bea5fb5cdb6397bc160bff8088add9d868231ced59e91f29f30741415d6985",
        jarInfo.attributes().get(JarAnalyzer.SHA256_CHECKSUM_KEY));
    assertEquals(JarAnalyzer.UNKNOWN_VERSION, jarInfo.version());
  }

  @Test
  public void getJarInfo_withPomProperties() {
    JarAnalyzer target = new JarAnalyzer(new NoopInsightsLogger(), true);
    JarInfo jarInfo = target.getJarInfoSafe("pom-props-5.5.3.jar", getURL(POM_PROPS_JAR_PATH));
    assertEquals("com.newrelic.pom.props", jarInfo.attributes().get("groupId"));
    assertEquals("pom-props", jarInfo.attributes().get("artifactId"));
    assertEquals(
        "799e9831ceca92aec5d983a3577a81dd7fd0a254bc1e38a18d64d1894ece942f",
        jarInfo.attributes().get(JarAnalyzer.SHA256_CHECKSUM_KEY));
    assertEquals("5.5.3", jarInfo.attributes().get("version"));
  }

  @Test
  public void testProcessEmptyJar() throws Exception {
    File jar = File.createTempFile("test", "jar");
    jar.deleteOnExit();

    JarAnalyzer processor = new JarAnalyzer(new NoopInsightsLogger(), true);
    JarInfo jarInfo = processor.getJarInfoSafe(jar.getName(), jar.toURI().toURL());
    assertEquals(JarAnalyzer.UNKNOWN_VERSION, jarInfo.version());
  }

  @Test
  public void textFilesReturnNull() throws URISyntaxException {
    URL txtURL = getURL(TXT_FILE);
    JarAnalyzer task = new JarAnalyzer(new NoopInsightsLogger(), true);
    Optional<JarInfo> oJarInfo = task.process(txtURL);
    assertFalse(oJarInfo.isPresent());
  }

  @Test
  public void getVersion() throws IOException {
    // The jsp jar's version is not in the main attributes, it is tucked in the entries. Make sure
    // we find it.
    Optional<String> oVersion =
        JarAnalyzer.getVersion(
            JarUtils.getJarInputStream(
                JspPage.class.getProtectionDomain().getCodeSource().getLocation()));
    assertTrue(oVersion.isPresent());
  }

  @Test
  void checkUnixClassPathSplitting() {
    String classpath = "/a/a.jar:/b/b.jar";
    String[] splits = ClasspathJarInfoSubreport.splitClassPathElements(classpath, ":");
    assertArrayEquals(new String[] {"/a/a.jar", "/b/b.jar"}, splits);
  }

  @Test
  void checkWindowsClassPathSplitting() {
    String classpath = "C:\\Program Files\\app\\a.jar;D:\\App\\lib\\b.jar";
    String[] splits = ClasspathJarInfoSubreport.splitClassPathElements(classpath, ";");
    assertArrayEquals(
        new String[] {"C:\\Program Files\\app\\a.jar", "D:\\App\\lib\\b.jar"}, splits);
  }

  @Test
  void checkUrlForEntries() throws MalformedURLException {
    String input;
    String expected;
    if (";".equals(File.pathSeparator)) {
      input = "C:\\Program Files\\app\\a.jar";
      expected = "file:///C:\\Program%20Files\\app\\a.jar";
    } else {
      input = "/a b c/a.jar";
      expected = "file:/a%20b%20c/a.jar";
    }
    assertEquals(expected, ClasspathJarInfoSubreport.urlFor(input).toString());
  }
}
