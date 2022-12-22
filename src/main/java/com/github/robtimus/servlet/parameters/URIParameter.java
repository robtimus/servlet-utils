/*
 * URIParameter.java
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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.function.UnaryOperator;
import javax.servlet.FilterConfig;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;

/**
 * Represents a parameter that should have a URI value.
 *
 * @author Rob Spoor
 */
public final class URIParameter {

    private final String name;
    private final URI value;

    private URIParameter(String name, URI value) {
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
    public URI requiredValue() {
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
    public URI valueWithDefault(URI defaultValue) {
        return value != null ? value : defaultValue;
    }

    /**
     * Validates that the parameter value's scheme has a specific value.
     *
     * @param scheme The scheme to check for.
     * @return This object.
     * @throws IllegalStateException If the parameter is set but its value's scheme does not match the given scheme.
     */
    public URIParameter schemeIs(String scheme) {
        if (value != null && !value.getScheme().equals(scheme)) {
            throw new IllegalStateException(Messages.URIParameter.schemeIsNot(name, scheme, value.getScheme()));
        }
        return this;
    }

    /**
     * Validates that the parameter value's scheme has a specific value.
     *
     * @param schemes The schemes to check for.
     * @return This object.
     * @throws IllegalStateException If the parameter is set but its value's scheme does not match any of the given schemes.
     */
    public URIParameter schemeIn(String... schemes) {
        return schemeIn(Arrays.asList(schemes));
    }

    /**
     * Validates that the parameter value's scheme has a specific value.
     *
     * @param schemes The schemes to check for.
     * @return This object.
     * @throws NullPointerException If the given collection is {@code null}.
     * @throws IllegalStateException If the parameter is set but its value's scheme does not match any of the given schemes.
     */
    public URIParameter schemeIn(Collection<String> schemes) {
        Objects.requireNonNull(schemes);
        if (value != null && !schemes.contains(value.getScheme())) {
            throw new IllegalStateException(Messages.URIParameter.schemeNotIn(name, schemes, value.getScheme()));
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
     * Returns a URI init parameter for a filter.
     *
     * @param config The filter config to read the init parameter from.
     * @param name The name of the init parameter.
     * @return An object representing the init parameter with the given name from the given filter config. It may or may not be set.
     * @throws NullPointerException If the given filter config or name is {@code null}.
     * @throws IllegalStateException If the init parameter is set but does not have a URI value.
     */
    public static URIParameter of(FilterConfig config, String name) {
        Objects.requireNonNull(config);
        Objects.requireNonNull(name);
        return of(config::getInitParameter, name);
    }

    /**
     * Returns a URI init parameter for a servlet.
     *
     * @param config The servlet config to read the init parameter from.
     * @param name The name of the init parameter.
     * @return An object representing the init parameter with the given name from the given servlet config. It may or may not be set.
     * @throws NullPointerException If the given servlet config or name is {@code null}.
     * @throws IllegalStateException If the init parameter is set but does not have a URI value.
     */
    public static URIParameter of(ServletConfig config, String name) {
        Objects.requireNonNull(config);
        Objects.requireNonNull(name);
        return of(config::getInitParameter, name);
    }

    /**
     * Returns a URI init parameter for a servlet context.
     *
     * @param context The servlet context config to read the init parameter from.
     * @param name The name of the init parameter.
     * @return An object representing the init parameter with the given name from the given servlet context. It may or may not be set.
     * @throws IllegalStateException If the init parameter is set but does not have a URI value.
     */
    public static URIParameter of(ServletContext context, String name) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(name);
        return of(context::getInitParameter, name);
    }

    /**
     * Returns a URI parameter for a servlet request.
     *
     * @param request The servlet request to read the parameter from.
     * @param name The name of the parameter.
     * @return An object representing the parameter with the given name from the given servlet request. It may or may not be set.
     * @throws IllegalStateException If the parameter is set but does not have a URI value.
     */
    public static URIParameter of(ServletRequest request, String name) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(name);
        return of(request::getParameter, name);
    }

    private static URIParameter of(UnaryOperator<String> getter, String name) {
        String value = getter.apply(name);
        return new URIParameter(name, value != null ? toURI(name, value) : null);
    }

    private static URI toURI(String name, String value) {
        try {
            return new URI(value);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(Messages.URIParameter.invalidValue(name, value), e);
        }
    }
}
