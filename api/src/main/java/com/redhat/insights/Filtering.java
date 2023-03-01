/* Copyright (C) Red Hat 2022-2023 */
package com.redhat.insights;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/** Insights data filtering function. */
public enum Filtering implements Function<Map<String, String>, Map<String, String>> {
  DEFAULT(Function.identity()),

  NOTHING(__ -> new HashMap<>());

  private final Function<Map<String, String>, Map<String, String>> mask;

  Filtering(Function<Map<String, String>, Map<String, String>> mask) {
    this.mask = mask;
  }

  public Map<String, String> apply(Map<String, String> unfiltered) {
    return mask.apply(unfiltered);
  }
}
