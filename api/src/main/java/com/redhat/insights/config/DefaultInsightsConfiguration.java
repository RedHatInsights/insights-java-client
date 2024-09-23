/* Copyright (C) Red Hat 2023-2024 */
package com.redhat.insights.config;

import org.jspecify.annotations.NullMarked;

/**
 * Base configuration that uses the default methods from {@link InsightsConfiguration}, and where
 * only {@link InsightsConfiguration#getIdentificationName()} needs to be overridden.
 */
@NullMarked
public abstract class DefaultInsightsConfiguration implements InsightsConfiguration {}
