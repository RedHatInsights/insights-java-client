/* Copyright (C) Red Hat 2023 */
package com.redhat.insights;

/**
 * General-purpose insights client exception type.
 *
 * <p>Lower-level exceptions (IO, etc.) shall be wrapped or attached to an {@link
 * InsightsException}.
 */
public final class InsightsException extends RuntimeException {
  public InsightsException(String message) {
    super(message);
  }

  public InsightsException(String message, Throwable cause) {
    super(message, cause);
  }

  public InsightsException(Throwable cause) {
    super(cause);
  }
}
