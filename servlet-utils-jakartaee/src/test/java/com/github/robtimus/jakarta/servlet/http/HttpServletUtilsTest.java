/*
 * HttpServletUtilsTest.java
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

package com.github.robtimus.jakarta.servlet.http;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringRequestContent;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.github.robtimus.jakarta.servlet.ServletTestBase;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@SuppressWarnings("nls")
class HttpServletUtilsTest extends ServletTestBase {

    @Test
    @DisplayName("forEachHeader(HttpServletRequest, BiConsumer), forEachHeader(HttpServletResponse, BiConsumer)")
    void testForEachHeader() {
        CaptureHeaderFilter captureHeaderFilter = new CaptureHeaderFilter();

        startServer(context -> {
            FilterHolder filter = new FilterHolder(captureHeaderFilter);
            context.addFilter(filter, "/*", EnumSet.allOf(DispatcherType.class));

            @SuppressWarnings("serial")
            ServletHolder servlet = new ServletHolder(new HttpServlet() {
                @Override
                @SuppressWarnings("resource")
                protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
                    String text = "Hello world";

                    response.setContentType("text/plain");
                    response.setContentLength(text.length());
                    response.addHeader("x-custom", "3");
                    response.addHeader("x-custom", "4");
                    response.getWriter().write(text);
                }
            });
            context.addServlet(servlet, "/*");
        });

        withClientRequest(request -> {
            ContentResponse response = assertDoesNotThrow(() -> request
                    .method(HttpMethod.POST)
                    .path("/")
                    .headers(headers -> {
                        headers.add("x-custom", "1");
                        headers.add("x-custom", "2");
                    })
                    .body(new StringRequestContent("text/plain", "Hello world"))
                    .send());

            assertEquals(200, response.getStatus());
        });

        assertThat(captureHeaderFilter.requestHeaders, hasEntry(equalToIgnoringCase("Content-Type"), contains("text/plain")));
        assertThat(captureHeaderFilter.requestHeaders, hasEntry(equalToIgnoringCase("Content-Length"), contains("11")));
        assertThat(captureHeaderFilter.requestHeaders, hasEntry(equalTo("x-custom"), contains("1", "2")));
        assertThat(captureHeaderFilter.requestHeaders, hasKey(equalToIgnoringCase("User-Agent")));

        assertThat(captureHeaderFilter.responseHeaders, hasEntry(equalToIgnoringCase("Content-Type"), contains(startsWith("text/plain"))));
        assertThat(captureHeaderFilter.responseHeaders, hasEntry(equalToIgnoringCase("Content-Length"), contains("11")));
        assertThat(captureHeaderFilter.responseHeaders, hasEntry(equalTo("x-custom"), contains("3", "4")));
        assertThat(captureHeaderFilter.responseHeaders, hasKey(equalToIgnoringCase("Date")));
    }

    private static final class CaptureHeaderFilter implements Filter {

        private final Map<String, List<String>> requestHeaders = new HashMap<>();
        private final Map<String, List<String>> responseHeaders = new HashMap<>();

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            HttpServletUtils.forEachHeader(httpRequest, this::captureRequestHeader);
            try {
                chain.doFilter(httpRequest, httpResponse);
            } finally {
                HttpServletUtils.forEachHeader(httpResponse, this::captureResponseHeader);
            }
        }

        private void captureRequestHeader(String name, String value) {
            requestHeaders.computeIfAbsent(name, v -> new ArrayList<>()).add(value);
        }

        private void captureResponseHeader(String name, String value) {
            responseHeaders.computeIfAbsent(name, v -> new ArrayList<>()).add(value);
        }
    }
}
