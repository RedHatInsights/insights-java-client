/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.tls;

public enum InsightsHelperStatus {
  OK(0, "OK"),
  ERR_CURR_USER(1, "Failed getting current user"),
  ERR_INCORRECT_USER(2, "Called by wrong user"),
  ERR_WRONG_ARGS(3, "Called with wrong arguments"),
  ERR_NOT_SETUID(4, "Helper not setuid"),
  ERR_CERT_OR_KEY(5, "Must specify cert or key"),
  ERR_FILE_READ(6, "Could not read file");

  private final int code;
  private final String message;

  InsightsHelperStatus(int code, String message) {
    this.code = code;
    this.message = message;
  }

  public int getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }

  public static InsightsHelperStatus fromExitCode(int code) {
    InsightsHelperStatus out;
    switch (code) {
      case 0:
        out = OK;
        break;
      case 1:
        out = ERR_CURR_USER;
        break;
      case 2:
        out = ERR_INCORRECT_USER;
        break;
      case 3:
        out = ERR_WRONG_ARGS;
        break;
      case 4:
        out = ERR_NOT_SETUID;
        break;
      case 5:
        out = ERR_CERT_OR_KEY;
        break;
      case 6:
        out = ERR_FILE_READ;
        break;
      default:
        throw new IllegalArgumentException("Illegal helper return code: " + code);
    }
    return out;
  }
}
