/* Copyright (C) Red Hat 2022-2023 */
package com.redhat.insights.tls;

import static com.redhat.insights.InsightsErrorCode.*;

import com.redhat.insights.InsightsException;
import com.redhat.insights.config.InsightsConfiguration;
import com.redhat.insights.logging.InsightsLogger;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import org.wildfly.common.iteration.CodePointIterator;
import org.wildfly.security.pem.Pem;

public class PEMSupport {

  private final InsightsLogger logger;
  private final InsightsConfiguration configuration;

  public PEMSupport(InsightsLogger logger, InsightsConfiguration configuration) {
    this.logger = logger;
    this.configuration = configuration;
  }

  public SSLContext createTLSContext() {
    byte[] certBytes = getBytesPossiblyPrivileged("--cert");
    byte[] keyBytes = getBytesPossiblyPrivileged("--key");
    if (certBytes.length == 0 || keyBytes.length == 0) {
      throw new InsightsException(
          ERROR_SSL_READING_CERTS, "SSLContext creation error - could not get file bytes");
    }
    logger.debug("Cert and key obtained successfully, trying to create TLS context");
    return createTLSContext(certBytes, keyBytes);
  }

  // For low-level testing only
  public SSLContext createTLSContext(Path certificatePath, Path keyPath) {
    try {
      byte[] certBytes = Files.readAllBytes(certificatePath);
      byte[] keyBytes = Files.readAllBytes(keyPath);
      return createTLSContext(certBytes, keyBytes);
    } catch (IOException err) {
      throw new InsightsException(ERROR_SSL_CREATING_CONTEXT, "SSLContext creation error", err);
    }
  }

  byte[] getBytesPossiblyPrivileged(String mode) {
    // First determine the path to look for
    String pathStr = "";
    switch (mode) {
      case "--cert":
        pathStr = configuration.getCertFilePath();
        break;
      case "--key":
        pathStr = configuration.getKeyFilePath();
        break;
      default:
        throw new InsightsException(
            ERROR_SSL_READING_CERTS_INVALID_MODE,
            "Invalid mode " + mode + " passed for cert retrieval. This should not happen.");
    }
    try {
      return Files.readAllBytes(Paths.get(pathStr));
    } catch (IOException iox) {
      // Direct read failed - try to use the helper, if configured
      logger.debug("Direct read of cert and key failed.", iox);
      logger.debug(
          "Trying to use the helper binary "
              + configuration.getCertHelperBinary()
              + " to read default cert: "
              + InsightsConfiguration.DEFAULT_RHEL_CERT_FILE_PATH
              + " and key: "
              + InsightsConfiguration.DEFAULT_RHEL_CERT_FILE_PATH);
      CertHelper helper = new CertHelper(logger, configuration);

      try {
        return helper.readUsingHelper(mode);
      } catch (IOException | InterruptedException e) {
        throw new InsightsException(ERROR_SSL_CREATING_CONTEXT, "SSLContext creation error", e);
      }
    }
  }

  SSLContext createTLSContext(byte[] certPemData, byte[] keyPemData) {
    char[] keystorePassword = new char[0];
    try {

      Certificate[] certificates =
          parsePemData(Certificate.class, certPemData).toArray(new Certificate[0]);
      List<PrivateKey> keys = parsePemData(PrivateKey.class, keyPemData);

      KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
      keyStore.load(null);

      for (int i = 0; i < certificates.length; i++) {
        keyStore.setCertificateEntry("cert-" + i, certificates[i]);
      }

      for (int i = 0; i < keys.size(); i++) {
        keyStore.setKeyEntry("key-" + i, keys.get(i), keystorePassword, certificates);
      }

      KeyManagerFactory keyManagerFactory =
          KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      keyManagerFactory.init(keyStore, keystorePassword);
      SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
      sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

      return sslContext;

    } catch (CertificateException err) {
      throw new InsightsException(ERROR_SSL_CERTS_PROBLEM, "Certificates error", err);
    } catch (UnrecoverableKeyException
        | KeyStoreException
        | IOException
        | NoSuchAlgorithmException
        | KeyManagementException err) {
      throw new InsightsException(ERROR_SSL_CREATING_CONTEXT, "SSLContext creation error", err);
    }
  }

  private static <T> List<T> parsePemData(Class<T> type, byte[] data) {
    ArrayList<T> items = new ArrayList<>();
    Pem.parsePemContent(CodePointIterator.ofUtf8Bytes(data))
        .forEachRemaining(
            pemEntry -> {
              T cast = pemEntry.tryCast(type);
              if (cast == null) {
                throw new InsightsException(
                    ERROR_SSL_PARSING_CERTS,
                    "Could not cast the a PemEntry of type " + type + " to class " + type);
              }
              items.add(cast);
            });
    return items;
  }
}
