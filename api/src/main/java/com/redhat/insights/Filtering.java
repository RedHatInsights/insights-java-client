/* Copyright (C) Red Hat 2022-2023 */
package com.redhat.insights;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/** Insights data filtering function. */
public enum Filtering implements Function<Map<String, Object>, Map<String, Object>> {
  DEFAULT(Function.identity()),

  NOTHING(__ -> new HashMap<>());

  private final Function<Map<String, Object>, Map<String, Object>> mask;

  Filtering(Function<Map<String, Object>, Map<String, Object>> mask) {
    this.mask = mask;
  }

  public Map<String, Object> apply(Map<String, Object> unfiltered) {
    return mask.apply(unfiltered);
  }
}
