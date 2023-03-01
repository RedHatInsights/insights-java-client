/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.logging;

import java.io.PrintStream;

public class PrintLogger implements InsightsLogger {
  public static final PrintLogger STDOUT_LOGGER = new PrintLogger(System.out);
  public static final PrintLogger STDERR_LOGGER = new PrintLogger(System.err);

  private final PrintStream out;

  private PrintLogger(PrintStream out) {
    this.out = out;
  }

  public PrintLogger of(PrintStream out) {
    return new PrintLogger(out);
  }

  @Override
  public void debug(String message) {
    debug(message, null);
  }

  @Override
  public void debug(String message, Throwable err) {
    out.println("[DEBUG] " + message);
    if (err != null) {
      err.printStackTrace(out);
    }
  }

  @Override
  public void info(String message) {
    out.println("[INFO] " + message);
  }

  @Override
  public void error(String message) {
    error(message, null);
  }

  @Override
  public void error(String message, Throwable err) {
    out.println("[ERROR] " + message);
    if (err != null) {
      err.printStackTrace(out);
    }
  }

  @Override
  public void warning(String message) {
    warning(message, null);
  }

  @Override
  public void warning(String message, Throwable err) {
    out.println("[WARNING] " + message);
    if (err != null) {
      err.printStackTrace(out);
    }
  }
}
