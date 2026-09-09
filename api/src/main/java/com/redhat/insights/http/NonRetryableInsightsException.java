/* Copyright (C) Red Hat 2024-2026 */
package com.redhat.insights.http;

import com.redhat.insights.InsightsErrorCode;
import com.redhat.insights.InsightsException;
import org.jspecify.annotations.NullMarked;

/**
 * An {@link InsightsException} subclass that implements {@link BackoffWrapper.NonRetryable},
 * signalling to the backoff loop that the failure is permanent and must not be retried.
 *
 * <p>This cannot extend {@link InsightsException} directly because that class is {@code final}.
 * Instead it wraps an {@link InsightsException} and delegates {@link #getMessage()} to it.
 *
 * <p>Use this for HTTP 4xx responses (401, 403, 413, 415) where retrying will never succeed.
 */
// ! NonRetryableInsightsException exists because InsightsException is final and cannot be
// ! subclassed. BackoffWrapper checks instanceof NonRetryable to skip the retry loop for
// ! permanent failures (Fix 4 from architecture review).
@NullMarked
public final class NonRetryableInsightsException extends RuntimeException
    implements BackoffWrapper.NonRetryable {

  private final InsightsException wrapped;

  public NonRetryableInsightsException(InsightsErrorCode error, String message) {
    this(new InsightsException(error, message));
  }

  public NonRetryableInsightsException(InsightsErrorCode error, String message, Throwable cause) {
    this(new InsightsException(error, message, cause));
  }

  private NonRetryableInsightsException(InsightsException wrapped) {
    super(wrapped.getMessage(), wrapped.getCause());
    this.wrapped = wrapped;
  }

  /** Returns the underlying {@link InsightsException}. */
  public InsightsException getWrapped() {
    return wrapped;
  }

  @Override
  public String getMessage() {
    return wrapped.getMessage();
  }
}
