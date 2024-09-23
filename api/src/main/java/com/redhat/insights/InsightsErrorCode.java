/* Copyright (C) Red Hat 2022-2024 */
package com.redhat.insights;

import org.jspecify.annotations.NullMarked;

/**
 * Client internal errors.
 *
 * @author Emmanuel Hugonnet (c) 2023 Red Hat, Inc.
 */
@NullMarked
public enum InsightsErrorCode {
  NONE(0),
  OPT_OUT(1),
  ERROR_WRITING_FILE(2),
  ERROR_GZIP_FILE(3),
  ERROR_SCHEDULED_SENT(4),
  ERROR_GENERATING_HASH(5),
  ERROR_GENERATING_ARCHIVE_HASH(6),
  ERROR_SERIALIZING_TO_JSON(7),
  ERROR_HTTP_SEND_SERVER_ERROR(8),
  ERROR_HTTP_SEND_INVALID_CONTENT_TYPE(9),
  ERROR_HTTP_SEND_PAYLOAD(10),
  ERROR_HTTP_SEND_AUTH_ERROR(11),
  ERROR_HTTP_SEND_(12),
  ERROR_SSL_READING_CERTS(13),
  ERROR_SSL_PARSING_CERTS(14),
  ERROR_SSL_CREATING_CONTEXT(15),
  ERROR_SSL_READING_CERTS_INVALID_MODE(16),
  ERROR_SSL_CERTS_PROBLEM(17),
  ERROR_IDENTIFICATION_NOT_DEFINED(18),
  ERROR_CLIENT_FAILED(19),
  ERROR_CLIENT_BACKOFF_RETRIES_FAILED(20),
  ERROR_INTERRUPTED_THREAD(21),
  ERROR_HTTP_SEND_FORBIDDEN(22),
  ERROR_HTTP_SEND_CLIENT_ERROR(23),
  ERROR_UPLOAD_DIR_CREATION(24);

  private static final String PREFIX = "I4ASR";
  private final int code;

  InsightsErrorCode(int code) {
    this.code = code;
  }

  public String formatMessage(String message) {
    if (code > 0) {
      return PREFIX + String.format("%04d", code) + ": " + message;
    }
    return message;
  }
}
