/*
 * ComparableParameter.java
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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import javax.servlet.FilterConfig;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;

/**
 * Represents a parameter that should have a value of a specific comparable type.
 * This class can be used for types like {@link BigInteger} or {@link BigDecimal}.
 *
 * @author Rob Spoor
 * @param <T> The comparable type.
 */
public final class ComparableParameter<T extends Comparable<? super T>> {

    private final String name;
    private final T value;

    private ComparableParameter(String name, T value) {
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
    public T requiredValue() {
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
    public T valueWithDefault(T defaultValue) {
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
    public ComparableParameter<T> atLeast(T minValue) {
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
    public ComparableParameter<T> atMost(T maxValue) {
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
    public ComparableParameter<T> greaterThan(T minValue) {
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
    public ComparableParameter<T> smallerThan(T maxValue) {
        Objects.requireNonNull(maxValue);
        if (value != null && value.compareTo(maxValue) >= 0) {
            throw new IllegalStateException(Messages.Parameter.valueNotSmaller(name, maxValue, value));
        }
        return this;
    }

    /**
     * Validates that the parameter value is not too small or large. This method combines {@link #atLeast(Comparable)} and
     * {@link #smallerThan(Comparable)}.
     *
     * @param minValue The minimum value for the parameter, inclusive.
     * @param maxValue The maximum value for the parameter, exclusive.
     * @return This object.
     * @throws NullPointerException If the given minimum or maximum is {@code null}.
     * @throws IllegalStateException If the parameter is set but its value is not between the given minimum and maximum.
     */
    public ComparableParameter<T> between(T minValue, T maxValue) {
        Objects.requireNonNull(minValue);
        Objects.requireNonNull(maxValue);
        return atLeast(minValue).smallerThan(maxValue);
    }

    @Override
    @SuppressWarnings("nls")
    public String toString() {
        return value != null
                ? name + "=" + value
                : name + " (not set)";
    }

    /**
     * Returns a comparable init parameter for a filter.
     *
     * @param <T> The comparable type.
     * @param config The filter config to read the init parameter from.
     * @param name The name of the init parameter.
     * @param converter A function for converting string values to comparable values. It will never be called with a {@code null} value.
     * @return An object representing the init parameter with the given name from the given filter config. It may or may not be set.
     * @throws NullPointerException If the given filter config, name or converter is {@code null}.
     * @throws IllegalStateException If the init parameter is set but the converter throws an exception.
     */
    public static <T extends Comparable<? super T>> ComparableParameter<T> of(FilterConfig config, String name,
                                                                              Function<String, ? extends T> converter) {

        Objects.requireNonNull(config);
        Objects.requireNonNull(name);
        Objects.requireNonNull(converter);
        return of(config::getInitParameter, name, converter);
    }

    /**
     * Returns a comparable init parameter for a servlet.
     *
     * @param <T> The comparable type.
     * @param config The servlet config to read the init parameter from.
     * @param name The name of the init parameter.
     * @param converter A function for converting string values to comparable values. It will never be called with a {@code null} value.
     * @return An object representing the init parameter with the given name from the given servlet config. It may or may not be set.
     * @throws NullPointerException If the given servlet config, name or converter is {@code null}.
     * @throws IllegalStateException If the init parameter is set but the converter throws an exception.
     */
    public static <T extends Comparable<? super T>> ComparableParameter<T> of(ServletConfig config, String name,
                                                                              Function<String, ? extends T> converter) {

        Objects.requireNonNull(config);
        Objects.requireNonNull(name);
        Objects.requireNonNull(converter);
        return of(config::getInitParameter, name, converter);
    }

    /**
     * Returns a comparable init parameter for a servlet context.
     *
     * @param <T> The comparable type.
     * @param context The servlet context config to read the init parameter from.
     * @param name The name of the init parameter.
     * @param converter A function for converting string values to comparable values. It will never be called with a {@code null} value.
     * @return An object representing the init parameter with the given name from the given servlet context. It may or may not be set.
     * @throws NullPointerException If the given servlet context, name or converter is {@code null}.
     * @throws IllegalStateException If the init parameter is set but the converter throws an exception.
     */
    public static <T extends Comparable<? super T>> ComparableParameter<T> of(ServletContext context, String name,
                                                                              Function<String, ? extends T> converter) {

        Objects.requireNonNull(context);
        Objects.requireNonNull(name);
        Objects.requireNonNull(converter);
        return of(context::getInitParameter, name, converter);
    }

    /**
     * Returns a comparable parameter for a servlet request.
     *
     * @param <T> The comparable type.
     * @param request The servlet request config to read the parameter from.
     * @param name The name of the parameter.
     * @param converter A function for converting string values to comparable values. It will never be called with a {@code null} value.
     * @return An object representing the parameter with the given name from the given servlet request. It may or may not be set.
     * @throws NullPointerException If the given servlet request, name or converter is {@code null}.
     * @throws IllegalStateException If the parameter is set but the converter throws an exception.
     */
    public static <T extends Comparable<? super T>> ComparableParameter<T> of(ServletRequest request, String name,
                                                                              Function<String, ? extends T> converter) {

        Objects.requireNonNull(request);
        Objects.requireNonNull(name);
        Objects.requireNonNull(converter);
        return of(request::getParameter, name, converter);
    }

    private static <T extends Comparable<? super T>> ComparableParameter<T> of(UnaryOperator<String> getter, String name,
                                                                               Function<String, ? extends T> converter) {

        String value = getter.apply(name);
        return new ComparableParameter<>(name, value != null ? toComparable(name, value, converter) : null);
    }

    private static <T extends Comparable<? super T>> T toComparable(String name, String value, Function<String, ? extends T> converter) {
        try {
            return converter.apply(value);
        } catch (RuntimeException e) {
            throw new IllegalStateException(Messages.Parameter.invalidValue(name, value), e);
        }
    }
}
