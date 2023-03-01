/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.logging;

import java.util.ArrayList;
import java.util.List;

public class TestLogger implements InsightsLogger {
  public enum LogLevel {
    DEBUG,
    INFO,
    WARNING,
    ERROR
  }

  List<TestLogRecord> logs = new ArrayList<>();

  @Override
  public void debug(String message) {
    addLogRecord(new TestLogRecord(LogLevel.DEBUG, message));
  }

  @Override
  public void debug(String message, Throwable err) {
    addLogRecord(new TestLogRecord(LogLevel.DEBUG, message, err));
  }

  @Override
  public void info(String message) {
    addLogRecord(new TestLogRecord(LogLevel.INFO, message));
  }

  @Override
  public void error(String message) {
    addLogRecord(new TestLogRecord(LogLevel.ERROR, message));
  }

  @Override
  public void error(String message, Throwable err) {
    addLogRecord(new TestLogRecord(LogLevel.ERROR, message, err));
  }

  @Override
  public void warning(String message) {
    addLogRecord(new TestLogRecord(LogLevel.WARNING, message));
  }

  @Override
  public void warning(String message, Throwable err) {
    addLogRecord(new TestLogRecord(LogLevel.WARNING, message, err));
  }

  private void addLogRecord(TestLogRecord logRecord) {
    logs.add(logRecord);
  }

  public List<TestLogRecord> getLogs() {
    return logs;
  }

  public class TestLogRecord {
    private LogLevel logLevel;
    private String message;
    private Throwable throwable;

    public TestLogRecord(LogLevel logLevel, String message, Throwable throwable) {
      this.logLevel = logLevel;
      this.message = message;
      this.throwable = throwable;
    }

    public TestLogRecord(LogLevel logLevel, String message) {
      this(logLevel, message, null);
    }

    public LogLevel getLogLevel() {
      return logLevel;
    }

    public String getMessage() {
      return message;
    }

    public Throwable getThrowable() {
      return throwable;
    }

    public String toString() {
      return "["
          + getLogLevel()
          + "] "
          + getMessage()
          + (getThrowable() != null ? "\n" + throwable : "");
    }
  }
}
