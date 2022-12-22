/*
 * URLParameter.java
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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.function.UnaryOperator;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRequest;

/**
 * Represents a parameter that should have a URL value.
 *
 * @author Rob Spoor
 */
public final class URLParameter {

    private final String name;
    private final URL value;

    private URLParameter(String name, URL value) {
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
    public URL requiredValue() {
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
    public URL valueWithDefault(URL defaultValue) {
        return value != null ? value : defaultValue;
    }

    /**
     * Validates that the parameter value's protocol has a specific value.
     *
     * @param protocol The protocol to check for.
     * @return This object.
     * @throws IllegalStateException If the parameter is set but its value's protocol does not match the given protocol.
     */
    public URLParameter protocolIs(String protocol) {
        if (value != null && !value.getProtocol().equals(protocol)) {
            throw new IllegalStateException(Messages.URLParameter.protocolIsNot(name, protocol, value.getProtocol()));
        }
        return this;
    }

    /**
     * Validates that the parameter value's protocol has a specific value.
     *
     * @param protocols The protocols to check for.
     * @return This object.
     * @throws IllegalStateException If the parameter is set but its value's protocol does not match any of the given protocols.
     */
    public URLParameter protocolIn(String... protocols) {
        return protocolIn(Arrays.asList(protocols));
    }

    /**
     * Validates that the parameter value's protocol has a specific value.
     *
     * @param protocols The protocols to check for.
     * @return This object.
     * @throws NullPointerException If the given collection is {@code null}.
     * @throws IllegalStateException If the parameter is set but its value's protocol does not match any of the given protocols.
     */
    public URLParameter protocolIn(Collection<String> protocols) {
        Objects.requireNonNull(protocols);
        if (value != null && !protocols.contains(value.getProtocol())) {
            throw new IllegalStateException(Messages.URLParameter.protocolNotIn(name, protocols, value.getProtocol()));
        }
        return this;
    }

    @Override
    @SuppressWarnings("nls")
    public String toString() {
        return value != null
                ? name + "=" + value
                : name + " (not set)";
    }

    /**
     * Returns a URL init parameter for a filter.
     *
     * @param config The filter config to read the init parameter from.
     * @param name The name of the init parameter.
     * @return An object representing the init parameter with the given name from the given filter config. It may or may not be set.
     * @throws NullPointerException If the given filter config or name is {@code null}.
     * @throws IllegalStateException If the init parameter is set but does not have a URL value.
     */
    public static URLParameter of(FilterConfig config, String name) {
        Objects.requireNonNull(config);
        Objects.requireNonNull(name);
        return of(config::getInitParameter, name);
    }

    /**
     * Returns a URL init parameter for a servlet.
     *
     * @param config The servlet config to read the init parameter from.
     * @param name The name of the init parameter.
     * @return An object representing the init parameter with the given name from the given servlet config. It may or may not be set.
     * @throws NullPointerException If the given servlet config or name is {@code null}.
     * @throws IllegalStateException If the init parameter is set but does not have a URL value.
     */
    public static URLParameter of(ServletConfig config, String name) {
        Objects.requireNonNull(config);
        Objects.requireNonNull(name);
        return of(config::getInitParameter, name);
    }

    /**
     * Returns a URL init parameter for a servlet context.
     *
     * @param context The servlet context config to read the init parameter from.
     * @param name The name of the init parameter.
     * @return An object representing the init parameter with the given name from the given servlet context. It may or may not be set.
     * @throws IllegalStateException If the init parameter is set but does not have a URL value.
     */
    public static URLParameter of(ServletContext context, String name) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(name);
        return of(context::getInitParameter, name);
    }

    /**
     * Returns a URL parameter for a servlet request.
     *
     * @param request The servlet request to read the parameter from.
     * @param name The name of the parameter.
     * @return An object representing the parameter with the given name from the given servlet request. It may or may not be set.
     * @throws IllegalStateException If the parameter is set but does not have a URL value.
     */
    public static URLParameter of(ServletRequest request, String name) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(name);
        return of(request::getParameter, name);
    }

    private static URLParameter of(UnaryOperator<String> getter, String name) {
        String value = getter.apply(name);
        return new URLParameter(name, value != null ? toURL(name, value) : null);
    }

    private static URL toURL(String name, String value) {
        try {
            return new URL(value);
        } catch (MalformedURLException e) {
            throw new IllegalStateException(Messages.URLParameter.invalidValue(name, value), e);
        }
    }
}
