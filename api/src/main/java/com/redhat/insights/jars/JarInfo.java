/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.jars;

import java.util.Collections;
import java.util.Map;

public final class JarInfo {
  public static final JarInfo MISSING =
      new JarInfo("<unknown>", "<missing>", Collections.emptyMap());

  private final String name;
  private final String version;
  private final Map<String, String> attributes;

  public JarInfo(String name, String version, Map<String, String> attributes) {
    this.name = name;
    this.version = version;
    this.attributes = attributes;
  }

  public String version() {
    return version;
  }

  public String name() {
    return name;
  }

  public Map<String, String> attributes() {
    return attributes;
  }

  @Override
  public String toString() {
    final StringBuffer sb = new StringBuffer("JarInfo{");
    sb.append("name='").append(name).append('\'');
    sb.append(", version='").append(version).append('\'');
    sb.append(", attributes=").append(attributes);
    sb.append('}');
    return sb.toString();
  }
}
