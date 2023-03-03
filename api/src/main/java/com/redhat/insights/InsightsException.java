/* Copyright (C) Red Hat 2023 */
package com.redhat.insights;

import static com.redhat.insights.InsightsErrorCode.NONE;

/**
 * General-purpose insights client exception type.
 *
 * <p>Lower-level exceptions (IO, etc.) shall be wrapped or attached to an {@link
 * InsightsException}.
 */
public final class InsightsException extends RuntimeException {

  private final InsightsErrorCode error;

  public InsightsException(String message) {
    this(NONE, message);
  }

  public InsightsException(InsightsErrorCode error, String message) {
    super(message);
    this.error = error;
  }

  public InsightsException(String message, Throwable cause) {
    this(NONE, message, cause);
  }

  public InsightsException(InsightsErrorCode error, String message, Throwable cause) {
    super(message, cause);
    this.error = error;
  }

  public InsightsException(Throwable cause) {
    this(NONE, cause);
  }

  public InsightsException(InsightsErrorCode error, Throwable cause) {
    super(cause);
    this.error = error;
  }

  @Override
  public String getMessage() {
    return error.formatMessage(super.getMessage());
  }
}
