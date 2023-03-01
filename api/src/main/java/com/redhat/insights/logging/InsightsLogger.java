/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.logging;

/** A logging facade to integrate with runtime-preferred logging mechanism. */
public interface InsightsLogger {

  void debug(String message);

  void debug(String message, Throwable err);

  void info(String message);

  void error(String message);

  void error(String message, Throwable err);

  void warning(String message);

  void warning(String message, Throwable err);
}
