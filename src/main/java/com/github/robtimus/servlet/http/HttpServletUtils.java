/*
 * HttpServletUtils.java
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

package com.github.robtimus.servlet.http;

import java.util.Enumeration;
import java.util.Objects;
import java.util.function.BiConsumer;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A utility class for HTTP servlets.
 *
 * @author Rob Spoor
 */
public final class HttpServletUtils {

    private HttpServletUtils() {
        throw new IllegalStateException("cannot create instances of " + getClass().getName()); //$NON-NLS-1$
    }

    /**
     * Executes an action for each header in a request.
     *
     * @param request The request.
     * @param action The action to execute.
     * @throws NullPointerException If the request or action is {@code null}.
     */
    public static void forEachHeader(HttpServletRequest request, BiConsumer<? super String, ? super String> action) {
        Objects.requireNonNull(action);

        for (Enumeration<String> names = request.getHeaderNames(); names.hasMoreElements(); ) {
            String name = names.nextElement();
            for (Enumeration<String> values = request.getHeaders(name); values.hasMoreElements(); ) {
                String value = values.nextElement();
                action.accept(name, value);
            }
        }
    }

    /**
     * Executes an action for each header in a response.
     *
     * @param response The response.
     * @param action The action to execute.
     * @throws NullPointerException If the response or action is {@code null}.
     */
    public static void forEachHeader(HttpServletResponse response, BiConsumer<? super String, ? super String> action) {
        Objects.requireNonNull(action);

        for (String name : response.getHeaderNames()) {
            for (String value : response.getHeaders(name)) {
                action.accept(name, value);
            }
        }
    }
}
