/* Copyright (C) Red Hat 2023 */
package com.redhat.insights;

import static org.junit.jupiter.api.Assertions.*;

import com.redhat.insights.config.EnvAndSysPropsInsightsConfiguration;
import com.redhat.insights.core.app.AppInsightsSubreport;
import com.redhat.insights.core.app.AppTopLevelReport;
import com.redhat.insights.jars.ClasspathJarInfoSubreport;
import com.redhat.insights.jars.JarInfo;
import com.redhat.insights.logging.PrintLogger;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TopLevelReportBaseImplTest {

  @Test
  void findJupiterInLocalHashes() {
    var logger = PrintLogger.STDOUT_LOGGER;
    var configuration = new EnvAndSysPropsInsightsConfiguration();
    ClasspathJarInfoSubreport classpathJarInfoSubreport = new ClasspathJarInfoSubreport(logger);
    var simpleReport =
        new AppTopLevelReport(
            logger,
            Map.of("jars", classpathJarInfoSubreport, "details", new AppInsightsSubreport()));

    simpleReport.generateReport(Filtering.NOTHING);
    var hashes = classpathJarInfoSubreport.getJarInfos();
    assertFalse(hashes.isEmpty());
    JarInfo jupiterEngine = null;
    for (JarInfo hash : hashes) {
      if (hash.name().startsWith("junit-jupiter-engine-")) {
        jupiterEngine = hash;
        break;
      }
    }
    assertNotNull(jupiterEngine);
    assertTrue(jupiterEngine.attributes().containsKey("sha256Checksum"));
    assertTrue(jupiterEngine.attributes().containsKey("sha512Checksum"));
  }
}
