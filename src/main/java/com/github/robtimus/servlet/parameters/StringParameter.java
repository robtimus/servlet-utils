/*
 * StringParameter.java
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
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import javax.servlet.FilterConfig;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;

/**
 * Represents a parameter that should have a string value.
 *
 * @author Rob Spoor
 */
public final class StringParameter {

    private final String name;
    private final String value;

    private StringParameter(String name, String value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Returns whether or not the parameter is set.
     *
     * @return {@code true} if the parameter is set, or {@code false} otherwise.
     */
    public boolean isSet() {
        return value != null;
    }

    /**
     * Returns the parameter value.
     *
     * @return The parameter value.
     * @throws IllegalStateException If the parameter is not set.
     */
    public String requiredValue() {
        if (value == null) {
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
    public String valueWithDefault(String defaultValue) {
        return value != null ? value : defaultValue;
    }

    /**
     * Validates that the parameter value is not too small.
     *
     * @param minValue The minimum value for the parameter, inclusive.
     * @return This object.
     * @throws NullPointerException If the given minimum is {@code null}.
     * @throws IllegalStateException If the parameter is set but its value is smaller than the given minimum.
     */
    public StringParameter atLeast(String minValue) {
        Objects.requireNonNull(minValue);
        if (value != null && value.compareTo(minValue) < 0) {
            throw new IllegalStateException(Messages.Parameter.valueSmaller(name, minValue, value));
        }
        return this;
    }

    /**
     * Validates that the parameter value is not too large.
     *
     * @param maxValue The maximum value for the parameter, inclusive.
     * @return This object.
     * @throws NullPointerException If the given maximum is {@code null}.
     * @throws IllegalStateException If the parameter is set but its value is larger than the given maximum.
     */
    public StringParameter atMost(String maxValue) {
        Objects.requireNonNull(maxValue);
        if (value != null && value.compareTo(maxValue) > 0) {
            throw new IllegalStateException(Messages.Parameter.valueLarger(name, maxValue, value));
        }
        return this;
    }

    /**
     * Validates that the parameter value is not too small.
     *
     * @param minValue The minimum value for the parameter, exclusive.
     * @return This object.
     * @throws NullPointerException If the given minimum is {@code null}.
     * @throws IllegalStateException If the parameter is set but its value is smaller than or equal to the given minimum.
     */
    public StringParameter greaterThan(String minValue) {
        Objects.requireNonNull(minValue);
        if (value != null && value.compareTo(minValue) <= 0) {
            throw new IllegalStateException(Messages.Parameter.valueNotLarger(name, minValue, value));
        }
        return this;
    }

    /**
     * Validates that the parameter value is not too large.
     *
     * @param maxValue The maximum value for the parameter, exclusive.
     * @return This object.
     * @throws NullPointerException If the given maximum is {@code null}.
     * @throws IllegalStateException If the parameter is set but its value is larger than or equal to the given maximum.
     */
    public StringParameter smallerThan(String maxValue) {
        Objects.requireNonNull(maxValue);
        if (value != null && value.compareTo(maxValue) >= 0) {
            throw new IllegalStateException(Messages.Parameter.valueNotSmaller(name, maxValue, value));
        }
        return this;
    }

    /**
     * Validates that the parameter value is not too small or large. This method combines {@link #atLeast(String)} and {@link #smallerThan(String)}.
     *
     * @param minValue The minimum value for the parameter, inclusive.
     * @param maxValue The maximum value for the parameter, exclusive.
     * @return This object.
     * @throws NullPointerException If the given minimum or maximum is {@code null}.
     * @throws IllegalStateException If the parameter is set but its value is not between the given minimum and maximum.
     */
    public StringParameter between(String minValue, String maxValue) {
        Objects.requireNonNull(minValue);
        Objects.requireNonNull(maxValue);
        return atLeast(minValue).smallerThan(maxValue);
    }

    /**
     * Validates that the parameter value is not empty.
     *
     * @return This object.
     * @throws IllegalStateException If the parameter is set but its value is empty.
     */
    public StringParameter notEmpty() {
        if (value != null && value.isEmpty()) {
            throw new IllegalStateException(Messages.StringParameter.empty(name));
        }
        return this;
    }

    /**
     * Validates that the parameter value is not blank.
     *
     * @return This object.
     * @throws IllegalStateException If the parameter is set but its value is blank.
     */
    public StringParameter notBlank() {
        if (value != null && isBlank(value)) {
            throw new IllegalStateException(Messages.StringParameter.empty(name));
        }
        return this;
    }

    private boolean isBlank(String value) {
        for (int i = 0; i < value.length(); i++) {
            int c = value.codePointAt(i);
            if (c != ' ' && c != '\t' && !Character.isWhitespace(c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Validates that the parameter value matches a pattern.
     *
     * @param pattern The pattern to check against.
     * @return This object.
     * @throws NullPointerException If the given pattern is {@code null}.
     * @throws IllegalStateException If the parameter is set but its value does not match the given pattern.
     */
    public StringParameter matching(Pattern pattern) {
        Objects.requireNonNull(pattern);
        if (value != null && !pattern.matcher(value).matches()) {
            throw new IllegalStateException(Messages.StringParameter.noMatch(name, pattern, value));
        }
        return this;
    }

    /**
     * Applies a function to the parameter value. This allows applying operations like {@link String#trim()} or {@link String#toLowerCase()}.
     * <p>
     * If the parameter is not set, the function will not be called.
     * If the parameter is set but function returns {@code null}, the returned parameter will behave like a parameter that is not set.
     *
     * @param f The function to apply.
     * @return A string parameter with a value that is the result of applying the given function to the parameter value of this object.
     * @throws NullPointerException If the given function is {@code null}.
     */
    public StringParameter transform(Function<? super String, String> f) {
        Objects.requireNonNull(f);
        return value != null
                ? new StringParameter(name, f.apply(value))
                : this;
    }

    @Override
    @SuppressWarnings("nls")
    public String toString() {
        return value != null
                ? name + "=" + value
                : name + " (not set)";
    }

    /**
     * Returns a string init parameter for a filter.
     *
     * @param config The filter config to read the init parameter from.
     * @param name The name of the init parameter.
     * @return An object representing the init parameter with the given name from the given filter config. It may or may not be set.
     * @throws NullPointerException If the given filter config or name is {@code null}.
     */
    public static StringParameter of(FilterConfig config, String name) {
        Objects.requireNonNull(config);
        Objects.requireNonNull(name);
        return of(config::getInitParameter, name);
    }

    /**
     * Returns a string init parameter for a servlet.
     *
     * @param config The servlet config to read the init parameter from.
     * @param name The name of the init parameter.
     * @return An object representing the init parameter with the given name from the given servlet config. It may or may not be set.
     * @throws NullPointerException If the given servlet config or name is {@code null}.
     */
    public static StringParameter of(ServletConfig config, String name) {
        Objects.requireNonNull(config);
        Objects.requireNonNull(name);
        return of(config::getInitParameter, name);
    }

    /**
     * Returns a string init parameter for a servlet context.
     *
     * @param context The servlet context config to read the init parameter from.
     * @param name The name of the init parameter.
     * @return An object representing the init parameter with the given name from the given servlet context. It may or may not be set.
     */
    public static StringParameter of(ServletContext context, String name) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(name);
        return of(context::getInitParameter, name);
    }

    /**
     * Returns a string parameter for a servlet request.
     *
     * @param request The servlet request to read the parameter from.
     * @param name The name of the parameter.
     * @return An object representing the parameter with the given name from the given servlet request. It may or may not be set.
     */
    public static StringParameter of(ServletRequest request, String name) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(name);
        return of(request::getParameter, name);
    }

    private static StringParameter of(UnaryOperator<String> getter, String name) {
        String value = getter.apply(name);
        return new StringParameter(name, value);
    }
}
