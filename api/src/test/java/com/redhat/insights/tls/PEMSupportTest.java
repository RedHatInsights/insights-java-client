/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.tls;

import com.redhat.insights.InsightsException;
import com.redhat.insights.doubles.DefaultConfiguration;
import com.redhat.insights.logging.TestLogger;
import java.io.IOException;
import java.nio.file.*;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import javax.security.auth.x500.X500Principal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.wildfly.common.bytes.ByteStringBuilder;
import org.wildfly.security.pem.Pem;
import org.wildfly.security.x500.cert.SelfSignedX509CertificateAndSigningKey;
import org.wildfly.security.x500.cert.X509CertificateBuilder;

public class PEMSupportTest {

  @Test
  public void testLoadingCertAndKey()
      throws NoSuchAlgorithmException, CertificateException, IOException {
    SelfSignedX509CertificateAndSigningKey ca =
        SelfSignedX509CertificateAndSigningKey.builder()
            .setDn(
                new X500Principal(
                    "O=Root Certificate Authority, EMAILADDRESS=elytron@wildfly.org, C=UK,"
                        + " ST=Elytron, CN=Elytron CA"))
            .setKeyAlgorithmName("RSA")
            .setSignatureAlgorithmName("SHA256withRSA")
            .addExtension(false, "BasicConstraints", "CA:true,pathlen:2147483647")
            .build();
    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
    KeyPair generatedKeys = keyPairGenerator.generateKeyPair();
    PublicKey publicKey = generatedKeys.getPublic();
    X509Certificate subjectCertificate =
        new X509CertificateBuilder()
            .setIssuerDn(ca.getSelfSignedCertificate().getIssuerX500Principal())
            .setSubjectDn(new X500Principal("O=Elytron, OU=Elytron, C=UK, ST=Elytron, CN=Firefly"))
            .setSignatureAlgorithmName("SHA256withRSA")
            .setSigningKey(ca.getSigningKey())
            .setNotValidBefore(ZonedDateTime.now().minusYears(1))
            .setNotValidAfter(ZonedDateTime.now().plusYears(1))
            .setPublicKey(publicKey)
            .build();
    ByteStringBuilder target = new ByteStringBuilder();
    Pem.generatePemX509Certificate(target, ca.getSelfSignedCertificate());
    Pem.generatePemX509Certificate(target, subjectCertificate);
    Path pemFile =
        getPathFromResource("com/redhat/insights/tls/dummy.cert").resolveSibling("pem.cert");
    Path keyFile =
        getPathFromResource("com/redhat/insights/tls/dummy.key").resolveSibling("pem.key");
    Files.write(pemFile, target.toArray());
    Files.write(keyFile, generatedKeys.getPrivate().getEncoded());
    new PEMSupport(new TestLogger(), new DefaultConfiguration()).createTLSContext(pemFile, keyFile);
  }

  @Test
  public void testLoadingCertAndKeyOutOfDate()
      throws NoSuchAlgorithmException, CertificateException, IOException {
    SelfSignedX509CertificateAndSigningKey ca =
        SelfSignedX509CertificateAndSigningKey.builder()
            .setDn(
                new X500Principal(
                    "O=Root Certificate Authority, EMAILADDRESS=elytron@wildfly.org, C=UK,"
                        + " ST=Elytron, CN=Elytron CA"))
            .setKeyAlgorithmName("RSA")
            .setSignatureAlgorithmName("SHA256withRSA")
            .addExtension(false, "BasicConstraints", "CA:true,pathlen:2147483647")
            .build();
    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
    KeyPair generatedKeys = keyPairGenerator.generateKeyPair();
    PublicKey publicKey = generatedKeys.getPublic();
    X509Certificate subjectCertificate =
        new X509CertificateBuilder()
            .setIssuerDn(ca.getSelfSignedCertificate().getIssuerX500Principal())
            .setSubjectDn(new X500Principal("O=Elytron, OU=Elytron, C=UK, ST=Elytron, CN=Firefly"))
            .setSignatureAlgorithmName("SHA256withRSA")
            .setSigningKey(ca.getSigningKey())
            .setNotValidBefore(ZonedDateTime.now().minusYears(2))
            .setNotValidAfter(ZonedDateTime.now().minusYears(1))
            .setPublicKey(publicKey)
            .build();
    ByteStringBuilder target = new ByteStringBuilder();
    Pem.generatePemX509Certificate(target, ca.getSelfSignedCertificate());
    Pem.generatePemX509Certificate(target, subjectCertificate);
    Path pemFile =
        getPathFromResource("com/redhat/insights/tls/dummy.cert")
            .resolveSibling("out-of-date.cert");
    Path keyFile =
        getPathFromResource("com/redhat/insights/tls/dummy.key").resolveSibling("out-of-date.key");
    Files.write(pemFile, target.toArray());
    Files.write(keyFile, generatedKeys.getPrivate().getEncoded());
    InsightsException exception =
        Assertions.assertThrows(
            InsightsException.class,
            () ->
                new PEMSupport(new TestLogger(), new DefaultConfiguration())
                    .createTLSContext(pemFile, keyFile));

    Assertions.assertEquals("I4ASR0015: SSLContext creation error", exception.getMessage());
    Assertions.assertTrue(exception.getCause() instanceof CertificateExpiredException);
    Assertions.assertTrue(
        exception.getCause().getMessage().startsWith("NotAfter:"),
        exception.getCause().getMessage());
  }

  @Test
  public void testNoErrorForMissingKey() {
    InsightsException exception =
        Assertions.assertThrows(
            InsightsException.class,
            () ->
                new PEMSupport(new TestLogger(), new DefaultConfiguration())
                    .createTLSContext(
                        getPathFromResource("com/redhat/insights/tls/dummy.cert"),
                        Paths.get("missing.key")));

    Assertions.assertEquals("I4ASR0015: SSLContext creation error", exception.getMessage());
    Assertions.assertTrue(exception.getCause() instanceof NoSuchFileException);
    Assertions.assertEquals("missing.key", exception.getCause().getMessage());
  }

  @Test
  public void testNoErrorForMissingCert() {
    InsightsException exception =
        Assertions.assertThrows(
            InsightsException.class,
            () ->
                new PEMSupport(new TestLogger(), new DefaultConfiguration())
                    .createTLSContext(
                        Paths.get("missing.cert"),
                        getPathFromResource("com/redhat/insights/tls/dummy.key")));

    Assertions.assertEquals("I4ASR0015: SSLContext creation error", exception.getMessage());
    Assertions.assertTrue(exception.getCause() instanceof NoSuchFileException);
    Assertions.assertEquals("missing.cert", exception.getCause().getMessage());
  }

  @Test
  public void testNoErrorForNonReadableKey() throws IOException {
    InsightsException exception =
        Assertions.assertThrows(
            InsightsException.class,
            () ->
                new PEMSupport(new TestLogger(), new DefaultConfiguration())
                    .createTLSContext(
                        getPathFromResource("com/redhat/insights/tls/dummy.cert"),
                        createNonReadableFile()));

    Assertions.assertEquals("I4ASR0015: SSLContext creation error", exception.getMessage());
    Assertions.assertTrue(exception.getCause() instanceof AccessDeniedException);
  }

  @Test
  public void testNoErrorForNonReadableCert() throws IOException {
    InsightsException exception =
        Assertions.assertThrows(
            InsightsException.class,
            () ->
                new PEMSupport(new TestLogger(), new DefaultConfiguration())
                    .createTLSContext(
                        createNonReadableFile(),
                        getPathFromResource("com/redhat/insights/tls/dummy.key")));

    Assertions.assertEquals("I4ASR0015: SSLContext creation error", exception.getMessage());
    Assertions.assertTrue(exception.getCause() instanceof AccessDeniedException);
  }

  private Path createNonReadableFile() throws IOException {
    Path nonReadableCert = Files.createTempFile(null, null);
    Assertions.assertTrue(nonReadableCert.toFile().setReadable(false));
    return nonReadableCert;
  }

  private Path getPathFromResource(String path) {
    return Paths.get(ClassLoader.getSystemClassLoader().getResource(path).getPath());
  }
}
