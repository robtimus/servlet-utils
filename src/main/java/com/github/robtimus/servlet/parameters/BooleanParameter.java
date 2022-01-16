/*
 * BooleanParameter.java
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
 * Represents a parameter that should have a boolean value.
 *
 * @author Rob Spoor
 */
public final class BooleanParameter {

    private final String name;
    private final Boolean value;

    private BooleanParameter(String name, Boolean value) {
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
    public boolean requiredValue() {
        if (value == null) {
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
    public boolean valueWithDefault(boolean defaultValue) {
        return value != null ? value : defaultValue;
    }

    @Override
    @SuppressWarnings("nls")
    public String toString() {
        return value != null
                ? name + "=" + value
                : name + " (not set)";
    }

    /**
     * Returns a boolean init parameter for a filter.
     *
     * @param config The filter config to read the init parameter from.
     * @param name The name of the init parameter.
     * @return An object representing the init parameter with the given name from the given filter config. It may or may not be set.
     * @throws NullPointerException If the given filter config or name is {@code null}.
     * @throws IllegalStateException If the init parameter is set but does not have a boolean value.
     */
    public static BooleanParameter of(FilterConfig config, String name) {
        Objects.requireNonNull(config);
        Objects.requireNonNull(name);
        return of(config::getInitParameter, name);
    }

    /**
     * Returns a boolean init parameter for a servlet.
     *
     * @param config The servlet config to read the init parameter from.
     * @param name The name of the init parameter.
     * @return An object representing the init parameter with the given name from the given servlet config. It may or may not be set.
     * @throws NullPointerException If the given servlet config or name is {@code null}.
     * @throws IllegalStateException If the init parameter is set but does not have a boolean value.
     */
    public static BooleanParameter of(ServletConfig config, String name) {
        Objects.requireNonNull(config);
        Objects.requireNonNull(name);
        return of(config::getInitParameter, name);
    }

    /**
     * Returns a boolean init parameter for a servlet context.
     *
     * @param context The servlet context config to read the init parameter from.
     * @param name The name of the init parameter.
     * @return An object representing the init parameter with the given name from the given servlet context. It may or may not be set.
     * @throws IllegalStateException If the init parameter is set but does not have a boolean value.
     */
    public static BooleanParameter of(ServletContext context, String name) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(name);
        return of(context::getInitParameter, name);
    }

    /**
     * Returns a boolean parameter for a servlet request.
     *
     * @param request The servlet request to read the parameter from.
     * @param name The name of the parameter.
     * @return An object representing the parameter with the given name from the given servlet request. It may or may not be set.
     * @throws IllegalStateException If the parameter is set but does not have a boolean value.
     */
    public static BooleanParameter of(ServletRequest request, String name) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(name);
        return of(request::getParameter, name);
    }

    private static BooleanParameter of(UnaryOperator<String> getter, String name) {
        String value = getter.apply(name);
        return new BooleanParameter(name, value != null ? toBoolean(name, value) : null);
    }

    @SuppressWarnings("nls")
    private static boolean toBoolean(String name, String value) {
        switch (value) {
            case "true":
                return true;
            case "false":
                return false;
            default:
                throw new IllegalStateException(Messages.BooleanParameter.invalidValue.get(name, value));
        }
    }
}
