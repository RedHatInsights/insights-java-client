/* Copyright (C) Red Hat 2023 */
package com.redhat.insights.config;

/**
 * Base configuration that uses the default methods from {@link InsightsConfiguration}, and where
 * only {@link InsightsConfiguration#getIdentificationName()} needs to be overridden.
 */
public abstract class DefaultInsightsConfiguration implements InsightsConfiguration {}
