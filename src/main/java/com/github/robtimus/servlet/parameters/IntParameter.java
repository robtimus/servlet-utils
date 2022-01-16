/*
 * IntParameter.java
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
 * Represents a parameter that should have an int value.
 *
 * @author Rob Spoor
 */
public final class IntParameter {

    private final String name;
    private final int value;
    private final boolean isSet;

    private IntParameter(String name, int value) {
        this.name = name;
        this.value = value;
        this.isSet = true;
    }

    private IntParameter(String name) {
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
    public int requiredValue() {
        if (!isSet) {
            throw new IllegalStateException(Messages.Parameter.missing.get(name));
        }
        return value;
    }

    /**
     * Returns the parameter if it is set.
     *
     * @param defaultValue The value to return if the parameter is not set.
     * @return The parameter value, or the given default value if the parameter is not set.
     */
    public int valueWithDefault(int defaultValue) {
        return isSet ? value : defaultValue;
    }

    /**
     * Validates that the parameter value is not too small.
     *
     * @param minValue The minimum value for the parameter, inclusive.
     * @return This object.
     * @throws IllegalStateException If the parameter is set but its value is smaller than the given minimum.
     */
    public IntParameter atLeast(int minValue) {
        if (isSet && value < minValue) {
            throw new IllegalStateException(Messages.Parameter.valueSmaller.get(name, minValue, value));
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
    public IntParameter atMost(int maxValue) {
        if (isSet && value > maxValue) {
            throw new IllegalStateException(Messages.Parameter.valueLarger.get(name, maxValue, value));
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
    public IntParameter greaterThan(int minValue) {
        if (isSet && value <= minValue) {
            throw new IllegalStateException(Messages.Parameter.valueNotLarger.get(name, minValue, value));
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
    public IntParameter smallerThan(int maxValue) {
        if (isSet && value >= maxValue) {
            throw new IllegalStateException(Messages.Parameter.valueNotSmaller.get(name, maxValue, value));
        }
        return this;
    }

    /**
     * Validates that the parameter value is not too small or large. This method combines {@link #atLeast(int)} and {@link #smallerThan(int)}.
     *
     * @param minValue The minimum value for the parameter, inclusive.
     * @param maxValue The maximum value for the parameter, exclusive.
     * @return This object.
     * @throws IllegalStateException If the parameter is set but its value is not between the given minimum and maximum.
     */
    public IntParameter between(int minValue, int maxValue) {
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
     * Returns an int init parameter for a filter.
     *
     * @param config The filter config to read the init parameter from.
     * @param name The name of the init parameter.
     * @return An object representing the init parameter with the given name from the given filter config. It may or may not be set.
     * @throws NullPointerException If the given filter config or name is {@code null}.
     * @throws IllegalStateException If the init parameter is set but does not have an int value.
     */
    public static IntParameter of(FilterConfig config, String name) {
        Objects.requireNonNull(config);
        Objects.requireNonNull(name);
        return of(config::getInitParameter, name);
    }

    /**
     * Returns an int init parameter for a servlet.
     *
     * @param config The servlet config to read the init parameter from.
     * @param name The name of the init parameter.
     * @return An object representing the init parameter with the given name from the given servlet config. It may or may not be set.
     * @throws NullPointerException If the given servlet config or name is {@code null}.
     * @throws IllegalStateException If the init parameter is set but does not have an int value.
     */
    public static IntParameter of(ServletConfig config, String name) {
        Objects.requireNonNull(config);
        Objects.requireNonNull(name);
        return of(config::getInitParameter, name);
    }

    /**
     * Returns an int init parameter for a servlet context.
     *
     * @param context The servlet context config to read the init parameter from.
     * @param name The name of the init parameter.
     * @return An object representing the init parameter with the given name from the given servlet context. It may or may not be set.
     * @throws IllegalStateException If the init parameter is set but does not have an int value.
     */
    public static IntParameter of(ServletContext context, String name) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(name);
        return of(context::getInitParameter, name);
    }

    /**
     * Returns an int parameter for a servlet request.
     *
     * @param request The servlet request to read the parameter from.
     * @param name The name of the parameter.
     * @return An object representing the parameter with the given name from the given servlet request. It may or may not be set.
     * @throws IllegalStateException If the parameter is set but does not have an int value.
     */
    public static IntParameter of(ServletRequest request, String name) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(name);
        return of(request::getParameter, name);
    }

    private static IntParameter of(UnaryOperator<String> getter, String name) {
        String value = getter.apply(name);
        return value != null
                ? new IntParameter(name, toInt(name, value))
                : new IntParameter(name);
    }

    private static int toInt(String name, String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalStateException(Messages.IntParameter.invalidValue.get(name, value), e);
        }
    }
}
