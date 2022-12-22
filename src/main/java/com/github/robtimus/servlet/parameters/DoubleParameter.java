/*
 * DoubleParameter.java
 * Copyright 2022 Rob Spoor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.robtimus.servlet.parameters;

import java.util.Objects;
import java.util.function.UnaryOperator;
import javax.servlet.FilterConfig;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;

/**
 * Represents a parameter that should have a double value.
 *
 * @author Rob Spoor
 */
public final class DoubleParameter {

    private final String name;
    private final double value;
    private final boolean isSet;

    private DoubleParameter(String name, double value) {
        this.name = name;
        this.value = value;
        this.isSet = true;
    }

    private DoubleParameter(String name) {
        this.name = name;
        this.value = 0;
        this.isSet = false;
    }

    /**
     * Returns whether or not the parameter is set.
     *
     * @return {@code true} if the parameter is set, or {@code false} otherwise.
     */
    public boolean isSet() {
        return isSet;
    }

    /**
     * Returns the parameter value.
     *
     * @return The parameter value.
     * @throws IllegalStateException If the parameter is not set.
     */
    public double requiredValue() {
        if (!isSet) {
            throw new IllegalStateException(Messages.Parameter.missing(name));
        }
        return value;
    }

    /**
     * Returns the parameter if it is set.
     *
     * @param defaultValue The value to return if the parameter is not set.
     * @return The parameter value, or the given default value if the parameter is not set.
     */
    public double valueWithDefault(double defaultValue) {
        return isSet ? value : defaultValue;
    }

    /**
     * Validates that the parameter value is not too small.
     *
     * @param minValue The minimum value for the parameter, inclusive.
     * @return This object.
     * @throws IllegalStateException If the parameter is set but its value is smaller than the given minimum.
     */
    public DoubleParameter atLeast(double minValue) {
        if (isSet && value < minValue) {
            throw new IllegalStateException(Messages.Parameter.valueSmaller(name, minValue, value));
        }
        return this;
    }

    /**
     * Validates that the parameter value is not too large.
     *
     * @param maxValue The maximum value for the parameter, inclusive.
     * @return This object.
     * @throws IllegalStateException If the parameter is set but its value is larger than the given maximum.
     */
    public DoubleParameter atMost(double maxValue) {
        if (isSet && value > maxValue) {
            throw new IllegalStateException(Messages.Parameter.valueLarger(name, maxValue, value));
        }
        return this;
    }

    /**
     * Validates that the parameter value is not too small.
     *
     * @param minValue The minimum value for the parameter, exclusive.
     * @return This object.
     * @throws IllegalStateException If the parameter is set but its value is smaller than or equal to the given minimum.
     */
    public DoubleParameter greaterThan(double minValue) {
        if (isSet && value <= minValue) {
            throw new IllegalStateException(Messages.Parameter.valueNotLarger(name, minValue, value));
        }
        return this;
    }

    /**
     * Validates that the parameter value is not too large.
     *
     * @param maxValue The maximum value for the parameter, exclusive.
     * @return This object.
     * @throws IllegalStateException If the parameter is set but its value is larger than or equal to the given maximum.
     */
    public DoubleParameter smallerThan(double maxValue) {
        if (isSet && value >= maxValue) {
            throw new IllegalStateException(Messages.Parameter.valueNotSmaller(name, maxValue, value));
        }
        return this;
    }

    /**
     * Validates that the parameter value is not too small or large. This method combines {@link #atLeast(double)} and {@link #smallerThan(double)}.
     *
     * @param minValue The minimum value for the parameter, inclusive.
     * @param maxValue The maximum value for the parameter, exclusive.
     * @return This object.
     * @throws IllegalStateException If the parameter is set but its value is not between the given minimum and maximum.
     */
    public DoubleParameter between(double minValue, double maxValue) {
        return atLeast(minValue).smallerThan(maxValue);
    }

    @Override
    @SuppressWarnings("nls")
    public String toString() {
        return isSet
                ? name + "=" + value
                : name + " (not set)";
    }

    /**
     * Returns a double init parameter for a filter.
     *
     * @param config The filter config to read the init parameter from.
     * @param name The name of the init parameter.
     * @return An object representing the init parameter with the given name from the given filter config. It may or may not be set.
     * @throws NullPointerException If the given filter config or name is {@code null}.
     * @throws IllegalStateException If the init parameter is set but does not have a double value.
     */
    public static DoubleParameter of(FilterConfig config, String name) {
        Objects.requireNonNull(config);
        Objects.requireNonNull(name);
        return of(config::getInitParameter, name);
    }

    /**
     * Returns a double init parameter for a servlet.
     *
     * @param config The servlet config to read the init parameter from.
     * @param name The name of the init parameter.
     * @return An object representing the init parameter with the given name from the given servlet config. It may or may not be set.
     * @throws NullPointerException If the given servlet config or name is {@code null}.
     * @throws IllegalStateException If the init parameter is set but does not have a double value.
     */
    public static DoubleParameter of(ServletConfig config, String name) {
        Objects.requireNonNull(config);
        Objects.requireNonNull(name);
        return of(config::getInitParameter, name);
    }

    /**
     * Returns a double init parameter for a servlet context.
     *
     * @param context The servlet context config to read the init parameter from.
     * @param name The name of the init parameter.
     * @return An object representing the init parameter with the given name from the given servlet context. It may or may not be set.
     * @throws IllegalStateException If the init parameter is set but does not have a double value.
     */
    public static DoubleParameter of(ServletContext context, String name) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(name);
        return of(context::getInitParameter, name);
    }

    /**
     * Returns a double parameter for a servlet request.
     *
     * @param request The servlet request to read the parameter from.
     * @param name The name of the parameter.
     * @return An object representing the parameter with the given name from the given servlet request. It may or may not be set.
     * @throws IllegalStateException If the parameter is set but does not have a double value.
     */
    public static DoubleParameter of(ServletRequest request, String name) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(name);
        return of(request::getParameter, name);
    }

    private static DoubleParameter of(UnaryOperator<String> getter, String name) {
        String value = getter.apply(name);
        return value != null
                ? new DoubleParameter(name, toDouble(name, value))
                : new DoubleParameter(name);
    }

    private static double toDouble(String name, String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new IllegalStateException(Messages.DoubleParameter.invalidValue(name, value), e);
        }
    }
}
