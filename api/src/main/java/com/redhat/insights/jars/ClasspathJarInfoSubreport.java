/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.jars;

import com.redhat.insights.InsightsException;
import com.redhat.insights.logging.InsightsLogger;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Optional;

/** Classpath entries sub-report. */
public class ClasspathJarInfoSubreport extends JarInfoSubreport {
  private static final String CLASSPATH_ENV = "java.class.path";
  private static final String USER_DIR = "user.dir";

  public ClasspathJarInfoSubreport(InsightsLogger logger) {
    super(logger);
  }

  @Override
  public void generateReport() {
    JarAnalyzer analyzer = new JarAnalyzer(logger, true);
    String cwd = System.getProperty(USER_DIR);
    String cpRaw = System.getProperty(CLASSPATH_ENV);

    jarInfos.clear();
    for (String entry : splitClassPathElements(cpRaw, File.pathSeparator)) {
      logger.debug(entry);
      try {
        if (!entry.startsWith(File.separator)) {
          entry = cwd + File.separatorChar + entry;
        }
        URL url = urlFor(entry);
        Optional<JarInfo> oJar = analyzer.process(url);
        oJar.ifPresent(jarInfos::add);
      } catch (MalformedURLException | URISyntaxException e) {
        throw new InsightsException("JAR hashing error", e);
      }
    }
  }

  static String[] splitClassPathElements(String classpath, String separator) {
    return classpath.split(separator);
  }

  static URL urlFor(String entry) throws MalformedURLException {
    return Paths.get(entry).toUri().toURL();
  }
}
