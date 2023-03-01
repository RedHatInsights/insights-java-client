/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.tls;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.Consumer;

public class CertStreamHandler implements Runnable {
  private InputStream inputStream;
  private Consumer<String> consumer;

  public CertStreamHandler(InputStream inputStream, Consumer<String> consumer) {
    this.inputStream = inputStream;
    this.consumer = consumer;
  }

  @Override
  public void run() {
    new BufferedReader(new InputStreamReader(inputStream)).lines().forEach(consumer);
  }
}
