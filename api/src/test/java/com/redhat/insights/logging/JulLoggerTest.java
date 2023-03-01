/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

class JulLoggerTest {

  @Test
  void smokeTests() {
    String loggerName = "org.acme.Yolo";
    JulLogger logger = new JulLogger(loggerName);
    TestHandler handler = new TestHandler();
    Logger target = Logger.getLogger(loggerName);
    target.setLevel(Level.FINEST);
    target.addHandler(handler);

    IOException boom = new IOException("boom");
    logger.info("yolo-1");
    logger.debug("yolo-2");
    logger.debug("yolo-3", boom);
    logger.error("woops-1");
    logger.error("woops-2", boom);
    logger.warning("yolo-4");
    logger.warning("yolo-5", boom);

    assertEquals(7, handler.records.size());

    LogRecord record = handler.records.get(0);
    assertEquals("yolo-1", record.getMessage());
    assertEquals(Level.INFO, record.getLevel());
    assertEquals(loggerName, record.getLoggerName());

    record = handler.records.get(1);
    assertEquals("yolo-2", record.getMessage());
    assertEquals(Level.FINER, record.getLevel());
    assertEquals(loggerName, record.getLoggerName());

    record = handler.records.get(2);
    assertEquals("yolo-3", record.getMessage());
    assertEquals(Level.FINER, record.getLevel());
    assertEquals(loggerName, record.getLoggerName());
    assertNotNull(record.getThrown());
    assertEquals("boom", record.getThrown().getMessage());
    assertEquals(
        "com.redhat.insights.logging.JulLoggerTest",
        record.getThrown().getStackTrace()[0].getClassName());

    record = handler.records.get(3);
    assertEquals("woops-1", record.getMessage());
    assertEquals(Level.SEVERE, record.getLevel());
    assertEquals(loggerName, record.getLoggerName());

    record = handler.records.get(4);
    assertEquals("woops-2", record.getMessage());
    assertEquals(Level.SEVERE, record.getLevel());
    assertEquals(loggerName, record.getLoggerName());
    assertNotNull(record.getThrown());
    assertEquals("boom", record.getThrown().getMessage());
    assertEquals(
        "com.redhat.insights.logging.JulLoggerTest",
        record.getThrown().getStackTrace()[0].getClassName());

    record = handler.records.get(5);
    assertEquals("yolo-4", record.getMessage());
    assertEquals(Level.WARNING, record.getLevel());
    assertEquals(loggerName, record.getLoggerName());

    record = handler.records.get(6);
    assertEquals("yolo-5", record.getMessage());
    assertEquals(Level.WARNING, record.getLevel());
    assertEquals(loggerName, record.getLoggerName());
    assertNotNull(record.getThrown());
    assertEquals("boom", record.getThrown().getMessage());
    assertEquals(
        "com.redhat.insights.logging.JulLoggerTest",
        record.getThrown().getStackTrace()[0].getClassName());
  }

  static class TestHandler extends Handler {

    final ArrayList<LogRecord> records = new ArrayList<>();

    @Override
    public void publish(LogRecord record) {
      records.add(record);
    }

    @Override
    public void flush() {
      // Nothing
    }

    @Override
    public void close() throws SecurityException {
      // Nothing
    }
  }
}
