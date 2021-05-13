/*
 * FilterConfigWrapper.java
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

package com.github.robtimus.javax.servlet;

import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;

/**
 * A wrapper around an existing {@link FilterConfig} instance. By default it delegates all methods. However, it also allows initialization parameters
 * to be added or overwritten. This can be useful when a sub class of some filter needs to provide hard-coded or calculated initialization parameter
 * values to its parent class. For instance:
 * <pre><code>
 * public void init(FilterConfig filterConfig) {
 *     super.init(new FilterConfigWrapper(filterConfig)
 *             .withInitParameter("booleanParameter", true)
 *             .withInitParameter("intParameter", 100));
 * }
 * </code></pre>
 *
 * @author Rob Spoor
 */
public class FilterConfigWrapper implements FilterConfig {

    private final FilterConfig filterConfig;

    private final Map<String, String> initParameters;

    /**
     * Creates a new {@link FilterConfig} wrapper.
     *
     * @param filterConfig The {@link FilterConfig} object to wrap.
     * @throws NullPointerException If the given {@link FilterConfig} object is {@code null}.
     */
    public FilterConfigWrapper(FilterConfig filterConfig) {
        this.filterConfig = Objects.requireNonNull(filterConfig);

        initParameters = new LinkedHashMap<>();
    }

    @Override
    public String getFilterName() {
        return filterConfig.getFilterName();
    }

    @Override
    public ServletContext getServletContext() {
        return filterConfig.getServletContext();
    }

    @Override
    public String getInitParameter(String name) {
        String value = initParameters.get(name);
        return value != null ? value : filterConfig.getInitParameter(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return initParameters.isEmpty()
                ? filterConfig.getInitParameterNames()
                : new CombinedEnumeration(filterConfig.getInitParameterNames(), initParameters.keySet());
    }

    /**
     * Adds or overwrites an initialization parameter.
     *
     * @param name The name of the initialization parameter.
     * @param value The value for the initialization parameter. Its {@link Object#toString() string representation} will be used.
     * @return This object.
     * @throws NullPointerException If the given name or value is {@code null}.
     */
    public FilterConfigWrapper withInitParameter(String name, Object value) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(value);

        initParameters.put(name, value.toString());

        return this;
    }
}
