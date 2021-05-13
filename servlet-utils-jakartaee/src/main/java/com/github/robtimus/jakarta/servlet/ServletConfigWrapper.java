/*
 * ServletConfigWrapper.java
 * Copyright 2021 Rob Spoor
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

package com.github.robtimus.jakarta.servlet;

import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;

/**
 * A wrapper around an existing {@link ServletConfig} instance. By default it delegates all methods. However, it also allows initialization parameters
 * to be added or overwritten. This can be useful when a sub class of some servlet needs to provide hard-coded or calculated initialization parameter
 * values to its parent class. For instance:
 * <pre><code>
 * public void init(ServletConfig servletConfig) {
 *     super.init(new ServletConfigWrapper(servletConfig)
 *             .withInitParameter("booleanParameter", true)
 *             .withInitParameter("intParameter", 100));
 * }
 * </code></pre>
 *
 * @author Rob Spoor
 */
public class ServletConfigWrapper implements ServletConfig {

    private final ServletConfig servletConfig;

    private final Map<String, String> initParameters;

    /**
     * Creates a new {@link ServletConfig} wrapper.
     *
     * @param servletConfig The {@link ServletConfig} object to wrap.
     * @throws NullPointerException If the given {@link ServletConfig} object is {@code null}.
     */
    public ServletConfigWrapper(ServletConfig servletConfig) {
        this.servletConfig = Objects.requireNonNull(servletConfig);

        initParameters = new LinkedHashMap<>();
    }

    @Override
    public String getServletName() {
        return servletConfig.getServletName();
    }

    @Override
    public ServletContext getServletContext() {
        return servletConfig.getServletContext();
    }

    @Override
    public String getInitParameter(String name) {
        String value = initParameters.get(name);
        return value != null ? value : servletConfig.getInitParameter(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return initParameters.isEmpty()
                ? servletConfig.getInitParameterNames()
                : new CombinedEnumeration(servletConfig.getInitParameterNames(), initParameters.keySet());
    }

    /**
     * Adds or overwrites an initialization parameter.
     *
     * @param name The name of the initialization parameter.
     * @param value The value for the initialization parameter. Its {@link Object#toString() string representation} will be used.
     * @return This object.
     * @throws NullPointerException If the given name or value is {@code null}.
     */
    public ServletConfigWrapper withInitParameter(String name, Object value) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(value);

        initParameters.put(name, value.toString());

        return this;
    }
}
