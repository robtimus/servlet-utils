/*
 * BodyCapturingFilterTest.java
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

import static com.github.robtimus.jakarta.servlet.http.BodyCapturingFilter.CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH;
import static com.github.robtimus.jakarta.servlet.http.BodyCapturingFilter.DEFAULT_INITIAL_CAPACITY;
import static com.github.robtimus.jakarta.servlet.http.BodyCapturingFilter.ENSURE_REQUEST_BODY_CONSUMED;
import static com.github.robtimus.jakarta.servlet.http.BodyCapturingFilter.INITIAL_REQUEST_CAPACITY;
import static com.github.robtimus.jakarta.servlet.http.BodyCapturingFilter.INITIAL_REQUEST_CAPACITY_FROM_CONTENT_LENGTH;
import static com.github.robtimus.jakarta.servlet.http.BodyCapturingFilter.INITIAL_RESPONSE_CAPACITY;
import static com.github.robtimus.jakarta.servlet.http.BodyCapturingFilter.REQUEST_LIMIT;
import static com.github.robtimus.jakarta.servlet.http.BodyCapturingFilter.RESPONSE_LIMIT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringRequestContent;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.github.robtimus.jakarta.servlet.ServletTestBase;
import com.github.robtimus.jakarta.servlet.ServletUtils;
import com.github.robtimus.jakarta.servlet.http.BodyCapturingFilter.BodyCapturingInputStream;
import com.github.robtimus.jakarta.servlet.http.BodyCapturingFilter.BodyCapturingOutputStream;
import com.github.robtimus.jakarta.servlet.http.BodyCapturingFilter.BodyCapturingReader;
import com.github.robtimus.jakarta.servlet.http.BodyCapturingFilter.BodyCapturingRequest;
import com.github.robtimus.jakarta.servlet.http.BodyCapturingFilter.BodyCapturingResponse;
import com.github.robtimus.jakarta.servlet.http.BodyCapturingFilter.BodyCapturingWriter;
import com.github.robtimus.jakarta.servlet.http.BodyCapturingFilter.CaptureMode;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

@SuppressWarnings("nls")
class BodyCapturingFilterTest {

    private static final String TEXT = "Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. Aenean massa.";
    private static final byte[] BYTES = TEXT.getBytes(StandardCharsets.UTF_8);

    @Nested
    class InitParameters {

        private FilterConfig filterConfig;

        @BeforeEach
        void initFilterConfig() {
            filterConfig = mock(FilterConfig.class);
            when(filterConfig.getServletContext()).thenReturn(mock(ServletContext.class));
        }

        @Test
        void testAllDefaultValues() {
            BodyCapturingFilter filter = new BodyCapturingFilter() { /* no overrides */ };
            filter.init(filterConfig);

            assertEquals(DEFAULT_INITIAL_CAPACITY, filter.initialRequestCapacity());
            assertFalse(filter.initialRequestCapacityFromContentLength());
            assertEquals(Integer.MAX_VALUE, filter.requestLimit());
            assertFalse(filter.considerRequestReadAfterContentLength());
            assertFalse(filter.ensureRequestBodyConsumed());

            assertEquals(DEFAULT_INITIAL_CAPACITY, filter.initialResponseCapacity());
            assertEquals(Integer.MAX_VALUE, filter.responseLimit());
        }

        @Nested
        @DisplayName(INITIAL_REQUEST_CAPACITY)
        class InitialRequestCapacity {

            @ParameterizedTest
            @ValueSource(ints = { 0, 1, 10, Integer.MAX_VALUE })
            @DisplayName("valid")
            void testValid(int value) {
                when(filterConfig.getInitParameter(INITIAL_REQUEST_CAPACITY)).thenReturn(Integer.toString(value));

                BodyCapturingFilter filter = new BodyCapturingFilter() { /* no overrides */ };
                filter.init(filterConfig);

                assertEquals(value, filter.initialRequestCapacity());
                assertFalse(filter.initialRequestCapacityFromContentLength());
                assertEquals(Integer.MAX_VALUE, filter.requestLimit());
                assertFalse(filter.considerRequestReadAfterContentLength());
                assertFalse(filter.ensureRequestBodyConsumed());

                assertEquals(DEFAULT_INITIAL_CAPACITY, filter.initialResponseCapacity());
                assertEquals(Integer.MAX_VALUE, filter.responseLimit());
            }

            @Test
            @DisplayName("negative")
            void testNegative() {
                when(filterConfig.getInitParameter(INITIAL_REQUEST_CAPACITY)).thenReturn("-1");

                BodyCapturingFilter filter = new BodyCapturingFilter() { /* no overrides */ };
                assertThrows(IllegalArgumentException.class, () -> filter.init(filterConfig));
            }

            @Test
            @DisplayName("not a number")
            void testNotANumber() {
                when(filterConfig.getInitParameter(INITIAL_REQUEST_CAPACITY)).thenReturn("");

                BodyCapturingFilter filter = new BodyCapturingFilter() { /* no overrides */ };
                assertThrows(IllegalArgumentException.class, () -> filter.init(filterConfig));
            }
        }

        @Nested
        @DisplayName(INITIAL_REQUEST_CAPACITY_FROM_CONTENT_LENGTH)
        class InitialRequestCapacityFromContentLength {

            @ParameterizedTest
            @ValueSource(booleans = { true, false })
            @DisplayName("valid")
            void testValid(boolean value) {
                when(filterConfig.getInitParameter(INITIAL_REQUEST_CAPACITY_FROM_CONTENT_LENGTH)).thenReturn(Boolean.toString(value));

                BodyCapturingFilter filter = new BodyCapturingFilter() { /* no overrides */ };
                filter.init(filterConfig);

                assertEquals(DEFAULT_INITIAL_CAPACITY, filter.initialRequestCapacity());
                assertEquals(value, filter.initialRequestCapacityFromContentLength());
                assertEquals(Integer.MAX_VALUE, filter.requestLimit());
                assertFalse(filter.considerRequestReadAfterContentLength());
                assertFalse(filter.ensureRequestBodyConsumed());

                assertEquals(DEFAULT_INITIAL_CAPACITY, filter.initialResponseCapacity());
                assertEquals(Integer.MAX_VALUE, filter.responseLimit());
            }

            @ParameterizedTest
            @ValueSource(strings = { "TRUE", "FALSE", "1" })
            @DisplayName("not a boolean")
            void testNotANumber() {
                when(filterConfig.getInitParameter(INITIAL_REQUEST_CAPACITY_FROM_CONTENT_LENGTH)).thenReturn("");

                BodyCapturingFilter filter = new BodyCapturingFilter() { /* no overrides */ };
                assertThrows(IllegalArgumentException.class, () -> filter.init(filterConfig));
            }
        }

        @Nested
        @DisplayName(REQUEST_LIMIT)
        class RequestLimit {

            @ParameterizedTest
            @ValueSource(ints = { 0, 1, 10, Integer.MAX_VALUE })
            @DisplayName("valid")
            void testValid(int value) {
                when(filterConfig.getInitParameter(REQUEST_LIMIT)).thenReturn(Integer.toString(value));

                BodyCapturingFilter filter = new BodyCapturingFilter() { /* no overrides */ };
                filter.init(filterConfig);

                assertEquals(DEFAULT_INITIAL_CAPACITY, filter.initialRequestCapacity());
                assertFalse(filter.initialRequestCapacityFromContentLength());
                assertEquals(value, filter.requestLimit());
                assertFalse(filter.considerRequestReadAfterContentLength());
                assertFalse(filter.ensureRequestBodyConsumed());

                assertEquals(DEFAULT_INITIAL_CAPACITY, filter.initialResponseCapacity());
                assertEquals(Integer.MAX_VALUE, filter.responseLimit());
            }

            @Test
            @DisplayName("negative")
            void testNegative() {
                when(filterConfig.getInitParameter(REQUEST_LIMIT)).thenReturn("-1");

                BodyCapturingFilter filter = new BodyCapturingFilter() { /* no overrides */ };
                assertThrows(IllegalArgumentException.class, () -> filter.init(filterConfig));
            }

            @Test
            @DisplayName("not a number")
            void testNotANumber() {
                when(filterConfig.getInitParameter(REQUEST_LIMIT)).thenReturn("");

                BodyCapturingFilter filter = new BodyCapturingFilter() { /* no overrides */ };
                assertThrows(IllegalArgumentException.class, () -> filter.init(filterConfig));
            }
        }

        @Nested
        @DisplayName(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH)
        class ConsiderRequestReadAfterContentLength {

            @ParameterizedTest
            @ValueSource(booleans = { true, false })
            @DisplayName("valid")
            void testValid(boolean value) {
                when(filterConfig.getInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH)).thenReturn(Boolean.toString(value));

                BodyCapturingFilter filter = new BodyCapturingFilter() { /* no overrides */ };
                filter.init(filterConfig);

                assertEquals(DEFAULT_INITIAL_CAPACITY, filter.initialRequestCapacity());
                assertFalse(filter.initialRequestCapacityFromContentLength());
                assertEquals(Integer.MAX_VALUE, filter.requestLimit());
                assertEquals(value, filter.considerRequestReadAfterContentLength());
                assertFalse(filter.ensureRequestBodyConsumed());

                assertEquals(DEFAULT_INITIAL_CAPACITY, filter.initialResponseCapacity());
                assertEquals(Integer.MAX_VALUE, filter.responseLimit());
            }

            @ParameterizedTest
            @ValueSource(strings = { "TRUE", "FALSE", "1" })
            @DisplayName("not a boolean")
            void testNotANumber() {
                when(filterConfig.getInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH)).thenReturn("");

                BodyCapturingFilter filter = new BodyCapturingFilter() { /* no overrides */ };
                assertThrows(IllegalArgumentException.class, () -> filter.init(filterConfig));
            }
        }

        @Nested
        @DisplayName(ENSURE_REQUEST_BODY_CONSUMED)
        class EnsureRequestBodyConsumed {

            @ParameterizedTest
            @ValueSource(booleans = { true, false })
            @DisplayName("valid")
            void testValid(boolean value) {
                when(filterConfig.getInitParameter(ENSURE_REQUEST_BODY_CONSUMED)).thenReturn(Boolean.toString(value));

                BodyCapturingFilter filter = new BodyCapturingFilter() { /* no overrides */ };
                filter.init(filterConfig);

                assertEquals(DEFAULT_INITIAL_CAPACITY, filter.initialRequestCapacity());
                assertFalse(filter.initialRequestCapacityFromContentLength());
                assertEquals(Integer.MAX_VALUE, filter.requestLimit());
                assertFalse(filter.considerRequestReadAfterContentLength());
                assertEquals(value, filter.ensureRequestBodyConsumed());

                assertEquals(DEFAULT_INITIAL_CAPACITY, filter.initialResponseCapacity());
                assertEquals(Integer.MAX_VALUE, filter.responseLimit());
            }

            @ParameterizedTest
            @ValueSource(strings = { "TRUE", "FALSE", "1" })
            @DisplayName("not a boolean")
            void testNotANumber() {
                when(filterConfig.getInitParameter(ENSURE_REQUEST_BODY_CONSUMED)).thenReturn("");

                BodyCapturingFilter filter = new BodyCapturingFilter() { /* no overrides */ };
                assertThrows(IllegalArgumentException.class, () -> filter.init(filterConfig));
            }
        }

        @Nested
        @DisplayName(INITIAL_RESPONSE_CAPACITY)
        class InitialResponseCapacity {

            @ParameterizedTest
            @ValueSource(ints = { 0, 1, 10, Integer.MAX_VALUE })
            @DisplayName("valid")
            void testValid(int value) {
                when(filterConfig.getInitParameter(INITIAL_RESPONSE_CAPACITY)).thenReturn(Integer.toString(value));

                BodyCapturingFilter filter = new BodyCapturingFilter() { /* no overrides */ };
                filter.init(filterConfig);

                assertEquals(DEFAULT_INITIAL_CAPACITY, filter.initialRequestCapacity());
                assertFalse(filter.initialRequestCapacityFromContentLength());
                assertEquals(Integer.MAX_VALUE, filter.requestLimit());
                assertFalse(filter.considerRequestReadAfterContentLength());
                assertFalse(filter.ensureRequestBodyConsumed());

                assertEquals(value, filter.initialResponseCapacity());
                assertEquals(Integer.MAX_VALUE, filter.responseLimit());
            }

            @Test
            @DisplayName("negative")
            void testNegative() {
                when(filterConfig.getInitParameter(INITIAL_RESPONSE_CAPACITY)).thenReturn("-1");

                BodyCapturingFilter filter = new BodyCapturingFilter() { /* no overrides */ };
                assertThrows(IllegalArgumentException.class, () -> filter.init(filterConfig));
            }

            @Test
            @DisplayName("not a number")
            void testNotANumber() {
                when(filterConfig.getInitParameter(INITIAL_RESPONSE_CAPACITY)).thenReturn("");

                BodyCapturingFilter filter = new BodyCapturingFilter() { /* no overrides */ };
                assertThrows(IllegalArgumentException.class, () -> filter.init(filterConfig));
            }
        }

        @Nested
        @DisplayName(RESPONSE_LIMIT)
        class ResponseLimit {

            @ParameterizedTest
            @ValueSource(ints = { 0, 1, 10, Integer.MAX_VALUE })
            @DisplayName("valid")
            void testValid(int value) {
                when(filterConfig.getInitParameter(RESPONSE_LIMIT)).thenReturn(Integer.toString(value));

                BodyCapturingFilter filter = new BodyCapturingFilter() { /* no overrides */ };
                filter.init(filterConfig);

                assertEquals(DEFAULT_INITIAL_CAPACITY, filter.initialRequestCapacity());
                assertFalse(filter.initialRequestCapacityFromContentLength());
                assertEquals(Integer.MAX_VALUE, filter.requestLimit());
                assertFalse(filter.considerRequestReadAfterContentLength());
                assertFalse(filter.ensureRequestBodyConsumed());

                assertEquals(DEFAULT_INITIAL_CAPACITY, filter.initialResponseCapacity());
                assertEquals(value, filter.responseLimit());
            }

            @Test
            @DisplayName("negative")
            void testNegative() {
                when(filterConfig.getInitParameter(RESPONSE_LIMIT)).thenReturn("-1");

                BodyCapturingFilter filter = new BodyCapturingFilter() { /* no overrides */ };
                assertThrows(IllegalArgumentException.class, () -> filter.init(filterConfig));
            }

            @Test
            @DisplayName("not a number")
            void testNotANumber() {
                when(filterConfig.getInitParameter(RESPONSE_LIMIT)).thenReturn("");

                BodyCapturingFilter filter = new BodyCapturingFilter() { /* no overrides */ };
                assertThrows(IllegalArgumentException.class, () -> filter.init(filterConfig));
            }
        }
    }

    @Nested
    @DisplayName("doFilter(ServletRequest, ServletResponse, FilterChain)")
    class DoFilter extends ServletTestBase {

        private TestFilter testFilter;

        @BeforeEach
        void initTestFilter() {
            testFilter = new TestFilter();
        }

        private void startServer(Servlet servlet) {
            startServer(servlet, filter -> { /* do nothing */ });
        }

        private void startServer(Servlet servlet, int limit) {
            startServer(servlet, limit, filter -> { /* do nothing */ });
        }

        private void startServer(Servlet servlet, Consumer<FilterHolder> filterConfigurer) {
            startServer(context -> context.addServlet(new ServletHolder(servlet), "/*"), filterConfigurer);
        }

        private void startServer(Servlet servlet, int limit, Consumer<FilterHolder> filterConfigurer) {
            String limitString = Integer.toString(limit);
            startServer(servlet, filter -> {
                filter.setInitParameter(REQUEST_LIMIT, limitString);
                filter.setInitParameter(RESPONSE_LIMIT, limitString);
                filterConfigurer.accept(filter);
            });
        }

        private void startServer(Consumer<ServletContextHandler> containerConfigurer, Consumer<FilterHolder> filterConfigurer) {
            super.startServer(context -> {
                containerConfigurer.accept(context);

                FilterHolder filter = new FilterHolder(testFilter);
                filterConfigurer.accept(filter);
                context.addFilter(filter, "/*", EnumSet.allOf(DispatcherType.class));
            });
        }

        private void sendGetRequest(Matcher<String> responseBodyMatcher) {
            withClientRequest(request -> {
                ContentResponse response = assertDoesNotThrow(() -> request
                        .method(HttpMethod.GET)
                        .path("/")
                        .send());

                assertEquals(200, response.getStatus());
                assertThat(response.getContentAsString(), responseBodyMatcher);
            });
        }

        private void sendPostRequest() {
            sendPostRequest(equalTo(""));
        }

        private void sendPostRequest(Matcher<String> responseBodyMatcher) {
            withClientRequest(request -> {
                ContentResponse response = assertDoesNotThrow(() -> request
                        .method(HttpMethod.POST)
                        .path("/")
                        .send());

                assertEquals(200, response.getStatus());
                assertThat(response.getContentAsString(), responseBodyMatcher);
            });
        }

        private void sendPostRequest(String body) {
            sendPostRequest(body, equalTo(body));
        }

        private void sendPostRequest(String requestBody, Matcher<String> responseBodyMatcher) {
            withClientRequest(request -> {
                String contentType = "text/plain";

                ContentResponse response = assertDoesNotThrow(() -> request
                        .method(HttpMethod.POST)
                        .path("/")
                        .body(new StringRequestContent(contentType, requestBody))
                        .send());

                assertEquals(200, response.getStatus());
                assertThat(response.getContentAsString(), responseBodyMatcher);
            });
        }

        @Nested
        @DisplayName("with binary bodies")
        class BinaryBodies {

            @Nested
            @DisplayName("with no limit")
            class Unlimited {

                @Nested
                @DisplayName("consuming all")
                class ConsumingAll {

                    private void startServer() {
                        DoFilter.this.startServer(new BinaryEchoServlet(false));
                    }

                    private void startServer(Consumer<FilterHolder> filterConfigurer) {
                        DoFilter.this.startServer(new BinaryEchoServlet(false), filterConfigurer);
                    }

                    @Nested
                    @DisplayName("with considerRequestReadAfterContentLength set")
                    class WithConsiderRequestReadAfterContentLengthSet {

                        @Nested
                        @DisplayName("with ensureRequestBodyConsumed set")
                        class WithEnsureRequestBodyConsumedSet {

                            private void startServer() {
                                ConsumingAll.this.startServer(filter -> {
                                    filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true");
                                    filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true");
                                });
                            }

                            @Test
                            @DisplayName("with no body")
                            void testWithNoBody() {
                                startServer();

                                sendGetRequest(equalTo(""));

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyBytes(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with empty body")
                            void testWithEmptyBody() {
                                startServer();

                                sendPostRequest();

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyBytes(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with non-empty body")
                            void testWithNonEmptyBody() {
                                startServer();

                                sendPostRequest(TEXT);

                                CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(BYTES, false);
                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(BYTES, false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }
                        }

                        @Nested
                        @DisplayName("with ensureRequestBodyConsumed not set")
                        class WithEnsureRequestBodyConsumedNotSet {

                            private void startServer() {
                                ConsumingAll.this.startServer(filter -> filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true"));
                            }

                            @Test
                            @DisplayName("with no body")
                            void testWithNoBody() {
                                startServer();

                                sendGetRequest(equalTo(""));

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyBytes(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with empty body")
                            void testWithEmptyBody() {
                                startServer();

                                sendPostRequest();

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyBytes(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with non-empty body")
                            void testWithNonEmptyBody() {
                                startServer();

                                sendPostRequest(TEXT);

                                CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(BYTES, false);
                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(BYTES, false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }
                        }
                    }

                    @Nested
                    @DisplayName("with considerRequestReadAfterContentLength not set")
                    class WithConsiderRequestReadAfterContentLengthNotSet {

                        @Nested
                        @DisplayName("with ensureRequestBodyConsumed set")
                        class WithEnsureRequestBodyConsumedSet {

                            private void startServer() {
                                ConsumingAll.this.startServer(filter -> filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true"));
                            }

                            @Test
                            @DisplayName("with no body")
                            void testWithNoBody() {
                                startServer();

                                sendGetRequest(equalTo(""));

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyBytes(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with empty body")
                            void testWithEmptyBody() {
                                startServer();

                                sendPostRequest();

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.emptyBytes(true);
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyBytes(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with non-empty body")
                            void testWithNonEmptyBody() {
                                startServer();

                                sendPostRequest(TEXT);

                                CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(BYTES, true);
                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(BYTES, false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }
                        }

                        @Nested
                        @DisplayName("with ensureRequestBodyConsumed not set")
                        class WithEnsureRequestBodyConsumedNotSet {

                            private void startServer() {
                                ConsumingAll.this.startServer();
                            }

                            @Test
                            @DisplayName("with no body")
                            void testWithNoBody() {
                                startServer();

                                sendGetRequest(equalTo(""));

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyBytes(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with empty body")
                            void testWithEmptyBody() {
                                startServer();

                                sendPostRequest();

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.emptyBytes(true);
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyBytes(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with non-empty body")
                            void testWithNonEmptyBody() {
                                startServer();

                                sendPostRequest(TEXT);

                                CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(BYTES, true);
                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(BYTES, false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }
                        }
                    }
                }

                @Nested
                @DisplayName("consuming up to content length")
                class ConsumingUpToContentLength {

                    private void startServer() {
                        DoFilter.this.startServer(new BinaryEchoServlet(true));
                    }

                    private void startServer(Consumer<FilterHolder> filterConfigurer) {
                        DoFilter.this.startServer(new BinaryEchoServlet(true), filterConfigurer);
                    }

                    @Nested
                    @DisplayName("with considerRequestReadAfterContentLength set")
                    class WithConsiderRequestReadAfterContentLengthSet {

                        @Nested
                        @DisplayName("with ensureRequestBodyConsumed set")
                        class WithEnsureRequestBodyConsumedSet {

                            private void startServer() {
                                ConsumingUpToContentLength.this.startServer(filter -> {
                                    filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true");
                                    filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true");
                                });
                            }

                            @Test
                            @DisplayName("with no body")
                            void testWithNoBody() {
                                startServer();

                                sendGetRequest(equalTo(""));

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyBytes(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with empty body")
                            void testWithEmptyBody() {
                                startServer();

                                sendPostRequest();

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyBytes(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with non-empty body")
                            void testWithNonEmptyBody() {
                                startServer();

                                sendPostRequest(TEXT);

                                CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(BYTES, false);
                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(BYTES, false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }
                        }

                        @Nested
                        @DisplayName("with ensureRequestBodyConsumed not set")
                        class WithEnsureRequestBodyConsumedNotSet {

                            private void startServer() {
                                ConsumingUpToContentLength.this.startServer(
                                        filter -> filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true"));
                            }

                            @Test
                            @DisplayName("with no body")
                            void testWithNoBody() {
                                startServer();

                                sendGetRequest(equalTo(""));

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyBytes(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with empty body")
                            void testWithEmptyBody() {
                                startServer();

                                sendPostRequest();

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyBytes(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with non-empty body")
                            void testWithNonEmptyBody() {
                                startServer();

                                sendPostRequest(TEXT);

                                CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(BYTES, false);
                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(BYTES, false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }
                        }
                    }

                    @Nested
                    @DisplayName("with considerRequestReadAfterContentLength not set")
                    class WithConsiderRequestReadAfterContentLengthNotSet {

                        @Nested
                        @DisplayName("with ensureRequestBodyConsumed set")
                        class WithEnsureRequestBodyConsumedSet {

                            private void startServer() {
                                ConsumingUpToContentLength.this.startServer(
                                        filter -> filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true"));
                            }

                            @Test
                            @DisplayName("with no body")
                            void testWithNoBody() {
                                startServer();

                                sendGetRequest(equalTo(""));

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyBytes(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with empty body")
                            void testWithEmptyBody() {
                                startServer();

                                sendPostRequest();

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.emptyBytes(true);
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyBytes(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with non-empty body")
                            void testWithNonEmptyBody() {
                                startServer();

                                sendPostRequest(TEXT);

                                CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(BYTES, true);
                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(BYTES, false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }
                        }

                        @Nested
                        @DisplayName("with ensureRequestBodyConsumed not set")
                        class WithEnsureRequestBodyConsumedNotSet {

                            private void startServer() {
                                ConsumingUpToContentLength.this.startServer();
                            }

                            @Test
                            @DisplayName("with no body")
                            void testWithNoBody() {
                                startServer();

                                sendGetRequest(equalTo(""));

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyBytes(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with empty body")
                            void testWithEmptyBody() {
                                startServer();

                                sendPostRequest();

                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyBytes(false);

                                assertNull(testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with non-empty body")
                            void testWithNonEmptyBody() {
                                startServer();

                                sendPostRequest(TEXT);

                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(BYTES, false);

                                assertNull(testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }
                        }
                    }
                }
            }

            @Nested
            @DisplayName("with limit")
            class Limited {

                private int limit = BYTES.length / 2;

                @Nested
                @DisplayName("consuming all")
                class ConsumingAll {

                    private void startServer() {
                        DoFilter.this.startServer(new BinaryEchoServlet(false), limit);
                    }

                    private void startServer(Consumer<FilterHolder> filterConfigurer) {
                        DoFilter.this.startServer(new BinaryEchoServlet(false), limit, filterConfigurer);
                    }

                    @Nested
                    @DisplayName("with considerRequestReadAfterContentLength set")
                    class WithConsiderRequestReadAfterContentLengthSet {

                        @Nested
                        @DisplayName("with ensureRequestBodyConsumed set")
                        class WithEnsureRequestBodyConsumedSet {

                            private void startServer() {
                                ConsumingAll.this.startServer(filter -> {
                                    filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true");
                                    filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true");
                                });
                            }

                            @Test
                            @DisplayName("with no body")
                            void testWithNoBody() {
                                startServer();

                                sendGetRequest(equalTo(""));

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyBytes(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with empty body")
                            void testWithEmptyBody() {
                                startServer();

                                sendPostRequest();

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyBytes(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with non-empty body")
                            void testWithNonEmptyBody() {
                                startServer();

                                sendPostRequest(TEXT);

                                CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(BYTES, limit, false);
                                CapturedData expectedCapturedDataForRequestLimitReached = new CapturedData(BYTES, limit, false);
                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(BYTES, limit, false);
                                CapturedData expectedCapturedDataForResponseLimitReached = new CapturedData(BYTES, limit, false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertEquals(expectedCapturedDataForRequestLimitReached, testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertEquals(expectedCapturedDataForResponseLimitReached, testFilter.capturedDataForResponseLimitReached.get());
                            }
                        }

                        @Nested
                        @DisplayName("with ensureRequestBodyConsumed not set")
                        class WithEnsureRequestBodyConsumedNotSet {

                            private void startServer() {
                                ConsumingAll.this.startServer(filter -> filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true"));
                            }

                            @Test
                            @DisplayName("with no body")
                            void testWithNoBody() {
                                startServer();

                                sendGetRequest(equalTo(""));

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyBytes(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with empty body")
                            void testWithEmptyBody() {
                                startServer();

                                sendPostRequest();

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyBytes(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with non-empty body")
                            void testWithNonEmptyBody() {
                                startServer();

                                sendPostRequest(TEXT);

                                CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(BYTES, limit, false);
                                CapturedData expectedCapturedDataForRequestLimitReached = new CapturedData(BYTES, limit, false);
                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(BYTES, limit, false);
                                CapturedData expectedCapturedDataForResponseLimitReached = new CapturedData(BYTES, limit, false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertEquals(expectedCapturedDataForRequestLimitReached, testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertEquals(expectedCapturedDataForResponseLimitReached, testFilter.capturedDataForResponseLimitReached.get());
                            }
                        }
                    }

                    @Nested
                    @DisplayName("with considerRequestReadAfterContentLength not set")
                    class WithConsiderRequestReadAfterContentLengthNotSet {

                        @Nested
                        @DisplayName("with ensureRequestBodyConsumed set")
                        class WithEnsureRequestBodyConsumedSet {

                            private void startServer() {
                                ConsumingAll.this.startServer(filter -> filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true"));
                            }

                            @Test
                            @DisplayName("with no body")
                            void testWithNoBody() {
                                startServer();

                                sendGetRequest(equalTo(""));

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyBytes(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with empty body")
                            void testWithEmptyBody() {
                                startServer();

                                sendPostRequest();

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.emptyBytes(true);
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyBytes(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with non-empty body")
                            void testWithNonEmptyBody() {
                                startServer();

                                sendPostRequest(TEXT);

                                CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(BYTES, limit, true);
                                CapturedData expectedCapturedDataForRequestLimitReached = new CapturedData(BYTES, limit, false);
                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(BYTES, limit, false);
                                CapturedData expectedCapturedDataForResponseLimitReached = new CapturedData(BYTES, limit, false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertEquals(expectedCapturedDataForRequestLimitReached, testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertEquals(expectedCapturedDataForResponseLimitReached, testFilter.capturedDataForResponseLimitReached.get());
                            }
                        }

                        @Nested
                        @DisplayName("with ensureRequestBodyConsumed not set")
                        class WithEnsureRequestBodyConsumedNotSet {

                            private void startServer() {
                                ConsumingAll.this.startServer();
                            }

                            @Test
                            @DisplayName("with no body")
                            void testWithNoBody() {
                                startServer();

                                sendGetRequest(equalTo(""));

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyBytes(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with empty body")
                            void testWithEmptyBody() {
                                startServer();

                                sendPostRequest();

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.emptyBytes(true);
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyBytes(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with non-empty body")
                            void testWithNonEmptyBody() {
                                startServer();

                                sendPostRequest(TEXT);

                                CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(BYTES, limit, true);
                                CapturedData expectedCapturedDataForRequestLimitReached = new CapturedData(BYTES, limit, false);
                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(BYTES, limit, false);
                                CapturedData expectedCapturedDataForResponseLimitReached = new CapturedData(BYTES, limit, false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertEquals(expectedCapturedDataForRequestLimitReached, testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertEquals(expectedCapturedDataForResponseLimitReached, testFilter.capturedDataForResponseLimitReached.get());
                            }
                        }
                    }
                }

                @Nested
                @DisplayName("consuming up to content length")
                class ConsumingUpToContentLength {

                    private void startServer() {
                        DoFilter.this.startServer(new BinaryEchoServlet(true), limit);
                    }

                    private void startServer(Consumer<FilterHolder> filterConfigurer) {
                        DoFilter.this.startServer(new BinaryEchoServlet(true), limit, filterConfigurer);
                    }

                    @Nested
                    @DisplayName("with considerRequestReadAfterContentLength set")
                    class WithConsiderRequestReadAfterContentLengthSet {

                        @Nested
                        @DisplayName("with ensureRequestBodyConsumed set")
                        class WithEnsureRequestBodyConsumedSet {

                            private void startServer() {
                                ConsumingUpToContentLength.this.startServer(filter -> {
                                    filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true");
                                    filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true");
                                });
                            }

                            @Test
                            @DisplayName("with no body")
                            void testWithNoBody() {
                                startServer();

                                sendGetRequest(equalTo(""));

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyBytes(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with empty body")
                            void testWithEmptyBody() {
                                startServer();

                                sendPostRequest();

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyBytes(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with non-empty body")
                            void testWithNonEmptyBody() {
                                startServer();

                                sendPostRequest(TEXT);

                                CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(BYTES, limit, false);
                                CapturedData expectedCapturedDataForRequestLimitReached = new CapturedData(BYTES, limit, false);
                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(BYTES, limit, false);
                                CapturedData expectedCapturedDataForResponseLimitReached = new CapturedData(BYTES, limit, false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertEquals(expectedCapturedDataForRequestLimitReached, testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertEquals(expectedCapturedDataForResponseLimitReached, testFilter.capturedDataForResponseLimitReached.get());
                            }
                        }

                        @Nested
                        @DisplayName("with ensureRequestBodyConsumed not set")
                        class WithEnsureRequestBodyConsumedNotSet {

                            private void startServer() {
                                ConsumingUpToContentLength.this.startServer(
                                        filter -> filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true"));
                            }

                            @Test
                            @DisplayName("with no body")
                            void testWithNoBody() {
                                startServer();

                                sendGetRequest(equalTo(""));

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyBytes(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with empty body")
                            void testWithEmptyBody() {
                                startServer();

                                sendPostRequest();

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyBytes(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with non-empty body")
                            void testWithNonEmptyBody() {
                                startServer();

                                sendPostRequest(TEXT);

                                CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(BYTES, limit, false);
                                CapturedData expectedCapturedDataForRequestLimitReached = new CapturedData(BYTES, limit, false);
                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(BYTES, limit, false);
                                CapturedData expectedCapturedDataForResponseLimitReached = new CapturedData(BYTES, limit, false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertEquals(expectedCapturedDataForRequestLimitReached, testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertEquals(expectedCapturedDataForResponseLimitReached, testFilter.capturedDataForResponseLimitReached.get());
                            }
                        }
                    }

                    @Nested
                    @DisplayName("with considerRequestReadAfterContentLength not set")
                    class WithConsiderRequestReadAfterContentLengthNotSet {

                        @Nested
                        @DisplayName("with ensureRequestBodyConsumed set")
                        class WithEnsureRequestBodyConsumedSet {

                            private void startServer() {
                                ConsumingUpToContentLength.this.startServer(
                                        filter -> filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true"));
                            }

                            @Test
                            @DisplayName("with no body")
                            void testWithNoBody() {
                                startServer();

                                sendGetRequest(equalTo(""));

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyBytes(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with empty body")
                            void testWithEmptyBody() {
                                startServer();

                                sendPostRequest();

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.emptyBytes(true);
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyBytes(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with non-empty body")
                            void testWithNonEmptyBody() {
                                startServer();

                                sendPostRequest(TEXT);

                                CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(BYTES, limit, true);
                                CapturedData expectedCapturedDataForRequestLimitReached = new CapturedData(BYTES, limit, false);
                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(BYTES, limit, false);
                                CapturedData expectedCapturedDataForResponseLimitReached = new CapturedData(BYTES, limit, false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertEquals(expectedCapturedDataForRequestLimitReached, testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertEquals(expectedCapturedDataForResponseLimitReached, testFilter.capturedDataForResponseLimitReached.get());
                            }
                        }

                        @Nested
                        @DisplayName("with ensureRequestBodyConsumed not set")
                        class WithEnsureRequestBodyConsumedNotSet {

                            private void startServer() {
                                ConsumingUpToContentLength.this.startServer();
                            }

                            @Test
                            @DisplayName("with no body")
                            void testWithNoBody() {
                                startServer();

                                sendGetRequest(equalTo(""));

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyBytes(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with empty body")
                            void testWithEmptyBody() {
                                startServer();

                                sendPostRequest();

                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyBytes(false);

                                assertNull(testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with non-empty body")
                            void testWithNonEmptyBody() {
                                startServer();

                                sendPostRequest(TEXT);

                                CapturedData expectedCapturedDataForRequestLimitReached = new CapturedData(BYTES, limit, false);
                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(BYTES, limit, false);
                                CapturedData expectedCapturedDataForResponseLimitReached = new CapturedData(BYTES, limit, false);

                                assertNull(testFilter.capturedDataForRequestBodyRead.get());
                                assertEquals(expectedCapturedDataForRequestLimitReached, testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertEquals(expectedCapturedDataForResponseLimitReached, testFilter.capturedDataForResponseLimitReached.get());
                            }
                        }
                    }
                }
            }
        }

        @Nested
        @DisplayName("with text bodies")
        class TextBodies {

            @Nested
            @DisplayName("with no limit")
            class Unlimited {

                @Nested
                @DisplayName("consuming all")
                class ConsumingAll {

                    private void startServer() {
                        DoFilter.this.startServer(new TextEchoServlet(false));
                    }

                    private void startServer(Consumer<FilterHolder> filterConfigurer) {
                        DoFilter.this.startServer(new TextEchoServlet(false), filterConfigurer);
                    }

                    @Nested
                    @DisplayName("with considerRequestReadAfterContentLength set")
                    class WithConsiderRequestReadAfterContentLengthSet {

                        @Nested
                        @DisplayName("with ensureRequestBodyConsumed set")
                        class WithEnsureRequestBodyConsumedSet {

                            private void startServer() {
                                ConsumingAll.this.startServer(filter -> {
                                    filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true");
                                    filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true");
                                });
                            }

                            @Test
                            @DisplayName("with no body")
                            void testWithNoBody() {
                                startServer();

                                sendGetRequest(equalTo(""));

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyText(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with empty body")
                            void testWithEmptyBody() {
                                startServer();

                                sendPostRequest();

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyText(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with non-empty body")
                            void testWithNonEmptyBody() {
                                startServer();

                                sendPostRequest(TEXT);

                                CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, false);
                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(TEXT, false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }
                        }

                        @Nested
                        @DisplayName("with ensureRequestBodyConsumed not set")
                        class WithEnsureRequestBodyConsumedNotSet {

                            private void startServer() {
                                ConsumingAll.this.startServer(filter -> filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true"));
                            }

                            @Test
                            @DisplayName("with no body")
                            void testWithNoBody() {
                                startServer();

                                sendGetRequest(equalTo(""));

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyText(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with empty body")
                            void testWithEmptyBody() {
                                startServer();

                                sendPostRequest();

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyText(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with non-empty body")
                            void testWithNonEmptyBody() {
                                startServer();

                                sendPostRequest(TEXT);

                                CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, false);
                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(TEXT, false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }
                        }
                    }

                    @Nested
                    @DisplayName("with considerRequestReadAfterContentLength not set")
                    class WithConsiderRequestReadAfterContentLengthNotSet {

                        @Nested
                        @DisplayName("with ensureRequestBodyConsumed set")
                        class WithEnsureRequestBodyConsumedSet {

                            private void startServer() {
                                ConsumingAll.this.startServer(filter -> filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true"));
                            }

                            @Test
                            @DisplayName("with no body")
                            void testWithNoBody() {
                                startServer();

                                sendGetRequest(equalTo(""));

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyText(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with empty body")
                            void testWithEmptyBody() {
                                startServer();

                                sendPostRequest();

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.emptyText(true);
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyText(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with non-empty body")
                            void testWithNonEmptyBody() {
                                startServer();

                                sendPostRequest(TEXT);

                                CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, true);
                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(TEXT, false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }
                        }

                        @Nested
                        @DisplayName("with ensureRequestBodyConsumed not set")
                        class WithEnsureRequestBodyConsumedNotSet {

                            private void startServer() {
                                ConsumingAll.this.startServer();
                            }

                            @Test
                            @DisplayName("with no body")
                            void testWithNoBody() {
                                startServer();

                                sendGetRequest(equalTo(""));

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyText(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with empty body")
                            void testWithEmptyBody() {
                                startServer();

                                sendPostRequest();

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.emptyText(true);
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyText(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with non-empty body")
                            void testWithNonEmptyBody() {
                                startServer();

                                sendPostRequest(TEXT);

                                CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, true);
                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(TEXT, false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }
                        }
                    }
                }

                @Nested
                @DisplayName("consuming up to content length")
                class ConsumingUpToContentLength {

                    private void startServer() {
                        DoFilter.this.startServer(new TextEchoServlet(true));
                    }

                    private void startServer(Consumer<FilterHolder> filterConfigurer) {
                        DoFilter.this.startServer(new TextEchoServlet(true), filterConfigurer);
                    }

                    @Nested
                    @DisplayName("with considerRequestReadAfterContentLength set")
                    class WithConsiderRequestReadAfterContentLengthSet {

                        @Nested
                        @DisplayName("with ensureRequestBodyConsumed set")
                        class WithEnsureRequestBodyConsumedSet {

                            private void startServer() {
                                ConsumingUpToContentLength.this.startServer(filter -> {
                                    filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true");
                                    filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true");
                                });
                            }

                            @Test
                            @DisplayName("with no body")
                            void testWithNoBody() {
                                startServer();

                                sendGetRequest(equalTo(""));

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyText(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with empty body")
                            void testWithEmptyBody() {
                                startServer();

                                sendPostRequest();

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyText(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with non-empty body")
                            void testWithNonEmptyBody() {
                                startServer();

                                sendPostRequest(TEXT);

                                CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, false);
                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(TEXT, false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }
                        }

                        @Nested
                        @DisplayName("with ensureRequestBodyConsumed not set")
                        class WithEnsureRequestBodyConsumedNotSet {

                            private void startServer() {
                                ConsumingUpToContentLength.this.startServer(
                                        filter -> filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true"));
                            }

                            @Test
                            @DisplayName("with no body")
                            void testWithNoBody() {
                                startServer();

                                sendGetRequest(equalTo(""));

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyText(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with empty body")
                            void testWithEmptyBody() {
                                startServer();

                                sendPostRequest();

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyText(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with non-empty body")
                            void testWithNonEmptyBody() {
                                startServer();

                                sendPostRequest(TEXT);

                                CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, false);
                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(TEXT, false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }
                        }
                    }

                    @Nested
                    @DisplayName("with considerRequestReadAfterContentLength not set")
                    class WithConsiderRequestReadAfterContentLengthNotSet {

                        @Nested
                        @DisplayName("with ensureRequestBodyConsumed set")
                        class WithEnsureRequestBodyConsumedSet {

                            private void startServer() {
                                ConsumingUpToContentLength.this.startServer(
                                        filter -> filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true"));
                            }

                            @Test
                            @DisplayName("with no body")
                            void testWithNoBody() {
                                startServer();

                                sendGetRequest(equalTo(""));

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyText(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with empty body")
                            void testWithEmptyBody() {
                                startServer();

                                sendPostRequest();

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.emptyText(true);
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyText(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with non-empty body")
                            void testWithNonEmptyBody() {
                                startServer();

                                sendPostRequest(TEXT);

                                CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, true);
                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(TEXT, false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }
                        }

                        @Nested
                        @DisplayName("with ensureRequestBodyConsumed not set")
                        class WithEnsureRequestBodyConsumedNotSet {

                            private void startServer() {
                                ConsumingUpToContentLength.this.startServer();
                            }

                            @Test
                            @DisplayName("with no body")
                            void testWithNoBody() {
                                startServer();

                                sendGetRequest(equalTo(""));

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyText(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with empty body")
                            void testWithEmptyBody() {
                                startServer();

                                sendPostRequest();

                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyText(false);

                                assertNull(testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with non-empty body")
                            void testWithNonEmptyBody() {
                                startServer();

                                sendPostRequest(TEXT);

                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(TEXT, false);

                                assertNull(testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }
                        }
                    }
                }
            }

            @Nested
            @DisplayName("with limit")
            class Limited {

                private int limit = TEXT.length() / 2;

                @Nested
                @DisplayName("consuming all")
                class ConsumingAll {

                    private void startServer() {
                        DoFilter.this.startServer(new TextEchoServlet(false), limit);
                    }

                    private void startServer(Consumer<FilterHolder> filterConfigurer) {
                        DoFilter.this.startServer(new TextEchoServlet(false), limit, filterConfigurer);
                    }

                    @Nested
                    @DisplayName("with considerRequestReadAfterContentLength set")
                    class WithConsiderRequestReadAfterContentLengthSet {

                        @Nested
                        @DisplayName("with ensureRequestBodyConsumed set")
                        class WithEnsureRequestBodyConsumedSet {

                            private void startServer() {
                                ConsumingAll.this.startServer(filter -> {
                                    filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true");
                                    filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true");
                                });
                            }

                            @Test
                            @DisplayName("with no body")
                            void testWithNoBody() {
                                startServer();

                                sendGetRequest(equalTo(""));

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyText(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with empty body")
                            void testWithEmptyBody() {
                                startServer();

                                sendPostRequest();

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyText(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with non-empty body")
                            void testWithNonEmptyBody() {
                                startServer();

                                sendPostRequest(TEXT);

                                CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, limit, false);
                                CapturedData expectedCapturedDataForRequestLimitReached = new CapturedData(TEXT, limit, false);
                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(TEXT, limit, false);
                                CapturedData expectedCapturedDataForResponseLimitReached = new CapturedData(TEXT, limit, false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertEquals(expectedCapturedDataForRequestLimitReached, testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertEquals(expectedCapturedDataForResponseLimitReached, testFilter.capturedDataForResponseLimitReached.get());
                            }
                        }

                        @Nested
                        @DisplayName("with ensureRequestBodyConsumed not set")
                        class WithEnsureRequestBodyConsumedNotSet {

                            private void startServer() {
                                ConsumingAll.this.startServer(filter -> filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true"));
                            }

                            @Test
                            @DisplayName("with no body")
                            void testWithNoBody() {
                                startServer();

                                sendGetRequest(equalTo(""));

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyText(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with empty body")
                            void testWithEmptyBody() {
                                startServer();

                                sendPostRequest();

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyText(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with non-empty body")
                            void testWithNonEmptyBody() {
                                startServer();

                                sendPostRequest(TEXT);

                                CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, limit, false);
                                CapturedData expectedCapturedDataForRequestLimitReached = new CapturedData(TEXT, limit, false);
                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(TEXT, limit, false);
                                CapturedData expectedCapturedDataForResponseLimitReached = new CapturedData(TEXT, limit, false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertEquals(expectedCapturedDataForRequestLimitReached, testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertEquals(expectedCapturedDataForResponseLimitReached, testFilter.capturedDataForResponseLimitReached.get());
                            }
                        }
                    }

                    @Nested
                    @DisplayName("with considerRequestReadAfterContentLength not set")
                    class WithConsiderRequestReadAfterContentLengthNotSet {

                        @Nested
                        @DisplayName("with ensureRequestBodyConsumed set")
                        class WithEnsureRequestBodyConsumedSet {

                            private void startServer() {
                                ConsumingAll.this.startServer(filter -> filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true"));
                            }

                            @Test
                            @DisplayName("with no body")
                            void testWithNoBody() {
                                startServer();

                                sendGetRequest(equalTo(""));

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyText(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with empty body")
                            void testWithEmptyBody() {
                                startServer();

                                sendPostRequest();

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.emptyText(true);
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyText(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with non-empty body")
                            void testWithNonEmptyBody() {
                                startServer();

                                sendPostRequest(TEXT);

                                CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, limit, true);
                                CapturedData expectedCapturedDataForRequestLimitReached = new CapturedData(TEXT, limit, false);
                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(TEXT, limit, false);
                                CapturedData expectedCapturedDataForResponseLimitReached = new CapturedData(TEXT, limit, false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertEquals(expectedCapturedDataForRequestLimitReached, testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertEquals(expectedCapturedDataForResponseLimitReached, testFilter.capturedDataForResponseLimitReached.get());
                            }
                        }

                        @Nested
                        @DisplayName("with ensureRequestBodyConsumed not set")
                        class WithEnsureRequestBodyConsumedNotSet {

                            private void startServer() {
                                ConsumingAll.this.startServer();
                            }

                            @Test
                            @DisplayName("with no body")
                            void testWithNoBody() {
                                startServer();

                                sendGetRequest(equalTo(""));

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyText(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with empty body")
                            void testWithEmptyBody() {
                                startServer();

                                sendPostRequest();

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.emptyText(true);
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyText(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with non-empty body")
                            void testWithNonEmptyBody() {
                                startServer();

                                sendPostRequest(TEXT);

                                CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, limit, true);
                                CapturedData expectedCapturedDataForRequestLimitReached = new CapturedData(TEXT, limit, false);
                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(TEXT, limit, false);
                                CapturedData expectedCapturedDataForResponseLimitReached = new CapturedData(TEXT, limit, false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertEquals(expectedCapturedDataForRequestLimitReached, testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertEquals(expectedCapturedDataForResponseLimitReached, testFilter.capturedDataForResponseLimitReached.get());
                            }
                        }
                    }
                }

                @Nested
                @DisplayName("consuming up to content length")
                class ConsumingUpToContentLength {

                    private void startServer() {
                        DoFilter.this.startServer(new TextEchoServlet(true), limit);
                    }

                    private void startServer(Consumer<FilterHolder> filterConfigurer) {
                        DoFilter.this.startServer(new TextEchoServlet(true), limit, filterConfigurer);
                    }

                    @Nested
                    @DisplayName("with considerRequestReadAfterContentLength set")
                    class WithConsiderRequestReadAfterContentLengthSet {

                        @Nested
                        @DisplayName("with ensureRequestBodyConsumed set")
                        class WithEnsureRequestBodyConsumedSet {

                            private void startServer() {
                                ConsumingUpToContentLength.this.startServer(filter -> {
                                    filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true");
                                    filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true");
                                });
                            }

                            @Test
                            @DisplayName("with no body")
                            void testWithNoBody() {
                                startServer();

                                sendGetRequest(equalTo(""));

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyText(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with empty body")
                            void testWithEmptyBody() {
                                startServer();

                                sendPostRequest();

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyText(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with non-empty body")
                            void testWithNonEmptyBody() {
                                startServer();

                                sendPostRequest(TEXT);

                                CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, limit, false);
                                CapturedData expectedCapturedDataForRequestLimitReached = new CapturedData(TEXT, limit, false);
                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(TEXT, limit, false);
                                CapturedData expectedCapturedDataForResponseLimitReached = new CapturedData(TEXT, limit, false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertEquals(expectedCapturedDataForRequestLimitReached, testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertEquals(expectedCapturedDataForResponseLimitReached, testFilter.capturedDataForResponseLimitReached.get());
                            }
                        }

                        @Nested
                        @DisplayName("with ensureRequestBodyConsumed not set")
                        class WithEnsureRequestBodyConsumedNotSet {

                            private void startServer() {
                                ConsumingUpToContentLength.this.startServer(
                                        filter -> filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true"));
                            }

                            @Test
                            @DisplayName("with no body")
                            void testWithNoBody() {
                                startServer();

                                sendGetRequest(equalTo(""));

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyText(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with empty body")
                            void testWithEmptyBody() {
                                startServer();

                                sendPostRequest();

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyText(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with non-empty body")
                            void testWithNonEmptyBody() {
                                startServer();

                                sendPostRequest(TEXT);

                                CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, limit, false);
                                CapturedData expectedCapturedDataForRequestLimitReached = new CapturedData(TEXT, limit, false);
                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(TEXT, limit, false);
                                CapturedData expectedCapturedDataForResponseLimitReached = new CapturedData(TEXT, limit, false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertEquals(expectedCapturedDataForRequestLimitReached, testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertEquals(expectedCapturedDataForResponseLimitReached, testFilter.capturedDataForResponseLimitReached.get());
                            }
                        }
                    }

                    @Nested
                    @DisplayName("with considerRequestReadAfterContentLength not set")
                    class WithConsiderRequestReadAfterContentLengthNotSet {

                        @Nested
                        @DisplayName("with ensureRequestBodyConsumed set")
                        class WithEnsureRequestBodyConsumedSet {

                            private void startServer() {
                                ConsumingUpToContentLength.this.startServer(
                                        filter -> filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true"));
                            }

                            @Test
                            @DisplayName("with no body")
                            void testWithNoBody() {
                                startServer();

                                sendGetRequest(equalTo(""));

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyText(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with empty body")
                            void testWithEmptyBody() {
                                startServer();

                                sendPostRequest();

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.emptyText(true);
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyText(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with non-empty body")
                            void testWithNonEmptyBody() {
                                startServer();

                                sendPostRequest(TEXT);

                                CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, limit, true);
                                CapturedData expectedCapturedDataForRequestLimitReached = new CapturedData(TEXT, limit, false);
                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(TEXT, limit, false);
                                CapturedData expectedCapturedDataForResponseLimitReached = new CapturedData(TEXT, limit, false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertEquals(expectedCapturedDataForRequestLimitReached, testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertEquals(expectedCapturedDataForResponseLimitReached, testFilter.capturedDataForResponseLimitReached.get());
                            }
                        }

                        @Nested
                        @DisplayName("with ensureRequestBodyConsumed not set")
                        class WithEnsureRequestBodyConsumedNotSet {

                            private void startServer() {
                                ConsumingUpToContentLength.this.startServer();
                            }

                            @Test
                            @DisplayName("with no body")
                            void testWithNoBody() {
                                startServer();

                                sendGetRequest(equalTo(""));

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyText(false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with empty body")
                            void testWithEmptyBody() {
                                startServer();

                                sendPostRequest();

                                CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.emptyText(false);

                                assertNull(testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with non-empty body")
                            void testWithNonEmptyBody() {
                                startServer();

                                sendPostRequest(TEXT);

                                CapturedData expectedCapturedDataForRequestLimitReached = new CapturedData(TEXT, limit, false);
                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(TEXT, limit, false);
                                CapturedData expectedCapturedDataForResponseLimitReached = new CapturedData(TEXT, limit, false);

                                assertNull(testFilter.capturedDataForRequestBodyRead.get());
                                assertEquals(expectedCapturedDataForRequestLimitReached, testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertEquals(expectedCapturedDataForResponseLimitReached, testFilter.capturedDataForResponseLimitReached.get());
                            }
                        }
                    }
                }
            }
        }

        @Nested
        @DisplayName("with request body not read")
        class WithRequestBodyNotRead {

            private void sendRequest() {
                DoFilter.this.sendPostRequest(equalTo(ConstantServlet.TEXT));
            }

            private void sendRequest(String body) {
                DoFilter.this.sendPostRequest(body, equalTo(ConstantServlet.TEXT));
            }

            @Nested
            @DisplayName("with no limit")
            class Unlimited {

                @Nested
                @DisplayName("ensuring body consumed")
                class EnsuringBodyConsumed {

                    @Nested
                    @DisplayName("using reader")
                    class UsingReader {

                        private void startServer() {
                            DoFilter.this.startServer(new ConstantServlet(true, true));
                        }

                        private void startServer(Consumer<FilterHolder> filterConfigurer) {
                            DoFilter.this.startServer(new ConstantServlet(true, true), filterConfigurer);
                        }

                        @Nested
                        @DisplayName("with considerRequestReadAfterContentLength set")
                        class WithConsiderRequestReadAfterContentLengthSet {

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed set")
                            class WithEnsureRequestBodyConsumedSet {

                                private void startServer() {
                                    UsingReader.this.startServer(filter -> {
                                        filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true");
                                        filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true");
                                    });
                                }

                                @Test
                                @DisplayName("with no body")
                                void testWithNoBody() {
                                    startServer();

                                    sendGetRequest(equalTo(ConstantServlet.TEXT));

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                    CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                    CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, false);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed not set")
                            class WithEnsureRequestBodyConsumedNotSet {

                                private void startServer() {
                                    UsingReader.this.startServer(
                                            filter -> filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true"));
                                }

                                @Test
                                @DisplayName("with no body")
                                void testWithNoBody() {
                                    startServer();

                                    sendGetRequest(equalTo(ConstantServlet.TEXT));

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                    CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                    CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, false);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }
                        }

                        @Nested
                        @DisplayName("with considerRequestReadAfterContentLength not set")
                        class WithConsiderRequestReadAfterContentLengthNotSet {

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed set")
                            class WithEnsureRequestBodyConsumedSet {

                                private void startServer() {
                                    UsingReader.this.startServer(filter -> filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true"));
                                }

                                @Test
                                @DisplayName("with no body")
                                void testWithNoBody() {
                                    startServer();

                                    sendGetRequest(equalTo(ConstantServlet.TEXT));

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                    CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.emptyText(true);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, true);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed not set")
                            class WithEnsureRequestBodyConsumedNotSet {

                                private void startServer() {
                                    UsingReader.this.startServer();
                                }

                                @Test
                                @DisplayName("with no body")
                                void testWithNoBody() {
                                    startServer();

                                    sendGetRequest(equalTo(ConstantServlet.TEXT));

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                    CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.emptyText(true);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, true);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }
                        }
                    }

                    @Nested
                    @DisplayName("not using reader")
                    class NotUsingReader {

                        private void startServer() {
                            DoFilter.this.startServer(new ConstantServlet(true, false));
                        }

                        private void startServer(Consumer<FilterHolder> filterConfigurer) {
                            DoFilter.this.startServer(new ConstantServlet(true, false), filterConfigurer);
                        }

                        @Nested
                        @DisplayName("with considerRequestReadAfterContentLength set")
                        class WithConsiderRequestReadAfterContentLengthSet {

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed set")
                            class WithEnsureRequestBodyConsumedSet {

                                private void startServer() {
                                    NotUsingReader.this.startServer(filter -> {
                                        filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true");
                                        filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true");
                                    });
                                }

                                @Test
                                @DisplayName("with no body")
                                void testWithNoBody() {
                                    startServer();

                                    sendGetRequest(equalTo(ConstantServlet.TEXT));

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                    CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                    CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(BYTES, false);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed not set")
                            class WithEnsureRequestBodyConsumedNotSet {

                                private void startServer() {
                                    NotUsingReader.this.startServer(
                                            filter -> filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true"));
                                }

                                @Test
                                @DisplayName("with no body")
                                void testWithNoBody() {
                                    startServer();

                                    sendGetRequest(equalTo(ConstantServlet.TEXT));

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                    CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                    CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(BYTES, false);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }
                        }

                        @Nested
                        @DisplayName("with considerRequestReadAfterContentLength not set")
                        class WithConsiderRequestReadAfterContentLengthNotSet {

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed set")
                            class WithEnsureRequestBodyConsumedSet {

                                private void startServer() {
                                    NotUsingReader.this.startServer(filter -> filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true"));
                                }

                                @Test
                                @DisplayName("with no body")
                                void testWithNoBody() {
                                    startServer();

                                    sendGetRequest(equalTo(ConstantServlet.TEXT));

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                    CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.emptyBytes(true);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(BYTES, true);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed not set")
                            class WithEnsureRequestBodyConsumedNotSet {

                                private void startServer() {
                                    NotUsingReader.this.startServer();
                                }

                                @Test
                                @DisplayName("with no body")
                                void testWithNoBody() {
                                    startServer();

                                    sendGetRequest(equalTo(ConstantServlet.TEXT));

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                    CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.emptyBytes(true);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(BYTES, true);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }
                        }
                    }
                }

                @Nested
                @DisplayName("not ensuring body consumed")
                class NotEnsuringBodyConsumed {

                    private void startServer() {
                        DoFilter.this.startServer(new ConstantServlet(false, false));
                    }

                    private void startServer(Consumer<FilterHolder> filterConfigurer) {
                        DoFilter.this.startServer(new ConstantServlet(false, false), filterConfigurer);
                    }

                    @Nested
                    @DisplayName("with considerRequestReadAfterContentLength set")
                    class WithConsiderRequestReadAfterContentLengthSet {

                        @Nested
                        @DisplayName("with ensureRequestBodyConsumed set")
                        class WithEnsureRequestBodyConsumedSet {

                            private void startServer() {
                                NotEnsuringBodyConsumed.this.startServer(filter -> {
                                    filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true");
                                    filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true");
                                });
                            }

                            @Test
                            @DisplayName("with no body")
                            void testWithNoBody() {
                                startServer();

                                sendGetRequest(equalTo(ConstantServlet.TEXT));

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with empty body")
                            void testWithEmptyBody() {
                                startServer();

                                sendRequest();

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with non-empty body")
                            void testWithNonEmptyBody() {
                                startServer();

                                sendRequest(TEXT);

                                CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, false);
                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }
                        }

                        @Nested
                        @DisplayName("with ensureRequestBodyConsumed not set")
                        class WithEnsureRequestBodyConsumedNotSet {

                            private void startServer() {
                                NotEnsuringBodyConsumed.this.startServer(
                                        filter -> filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true"));
                            }

                            @Test
                            @DisplayName("with no body")
                            void testWithNoBody() {
                                startServer();

                                sendGetRequest(equalTo(ConstantServlet.TEXT));

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with empty body")
                            void testWithEmptyBody() {
                                startServer();

                                sendRequest();

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with non-empty body")
                            void testWithNonEmptyBody() {
                                startServer();

                                sendRequest(TEXT);

                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                assertNull(testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }
                        }
                    }

                    @Nested
                    @DisplayName("with considerRequestReadAfterContentLength not set")
                    class WithConsiderRequestReadAfterContentLengthNotSet {

                        @Nested
                        @DisplayName("with ensureRequestBodyConsumed set")
                        class WithEnsureRequestBodyConsumedSet {

                            private void startServer() {
                                NotEnsuringBodyConsumed.this.startServer(
                                        filter -> filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true"));
                            }

                            @Test
                            @DisplayName("with no body")
                            void testWithNoBody() {
                                startServer();

                                sendGetRequest(equalTo(ConstantServlet.TEXT));

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with empty body")
                            void testWithEmptyBody() {
                                startServer();

                                sendRequest();

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.emptyText(true);
                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with non-empty body")
                            void testWithNonEmptyBody() {
                                startServer();

                                sendRequest(TEXT);

                                CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, true);
                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }
                        }

                        @Nested
                        @DisplayName("with ensureRequestBodyConsumed not set")
                        class WithEnsureRequestBodyConsumedNotSet {

                            private void startServer() {
                                NotEnsuringBodyConsumed.this.startServer();
                            }

                            @Test
                            @DisplayName("with no body")
                            void testWithNoBody() {
                                startServer();

                                sendGetRequest(equalTo(ConstantServlet.TEXT));

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with empty body")
                            void testWithEmptyBody() {
                                startServer();

                                sendRequest();

                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                assertNull(testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with non-empty body")
                            void testWithNonEmptyBody() {
                                startServer();

                                sendRequest(TEXT);

                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                assertNull(testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }
                        }
                    }
                }
            }

            @Nested
            @DisplayName("with limit")
            class Limited {

                private int limit = TEXT.length() / 2;

                @Nested
                @DisplayName("ensuring body consumed")
                class EnsuringBodyConsumed {

                    @Nested
                    @DisplayName("using reader")
                    class UsingReader {

                        private void startServer() {
                            DoFilter.this.startServer(new ConstantServlet(true, true), limit);
                        }

                        private void startServer(Consumer<FilterHolder> filterConfigurer) {
                            DoFilter.this.startServer(new ConstantServlet(true, true), limit, filterConfigurer);
                        }

                        @Nested
                        @DisplayName("with considerRequestReadAfterContentLength set")
                        class WithConsiderRequestReadAfterContentLengthSet {

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed set")
                            class WithEnsureRequestBodyConsumedSet {

                                private void startServer() {
                                    UsingReader.this.startServer(filter -> {
                                        filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true");
                                        filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true");
                                    });
                                }

                                @Test
                                @DisplayName("with no body")
                                void testWithNoBody() {
                                    startServer();

                                    sendGetRequest(equalTo(ConstantServlet.TEXT));

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                    CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                    CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, limit, false);
                                    CapturedData expectedCapturedDataForRequestLimitReached = new CapturedData(TEXT, limit, false);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertEquals(expectedCapturedDataForRequestLimitReached, testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed not set")
                            class WithEnsureRequestBodyConsumedNotSet {

                                private void startServer() {
                                    UsingReader.this.startServer(
                                            filter -> filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true"));
                                }

                                @Test
                                @DisplayName("with no body")
                                void testWithNoBody() {
                                    startServer();

                                    sendGetRequest(equalTo(ConstantServlet.TEXT));

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                    CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                    CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, limit, false);
                                    CapturedData expectedCapturedDataForRequestLimitReached = new CapturedData(TEXT, limit, false);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertEquals(expectedCapturedDataForRequestLimitReached, testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }
                        }

                        @Nested
                        @DisplayName("with considerRequestReadAfterContentLength not set")
                        class WithConsiderRequestReadAfterContentLengthNotSet {

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed set")
                            class WithEnsureRequestBodyConsumedSet {

                                private void startServer() {
                                    UsingReader.this.startServer(filter -> filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true"));
                                }

                                @Test
                                @DisplayName("with no body")
                                void testWithNoBody() {
                                    startServer();

                                    sendGetRequest(equalTo(ConstantServlet.TEXT));

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                    CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.emptyText(true);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, limit, true);
                                    CapturedData expectedCapturedDataForRequestLimitReached = new CapturedData(TEXT, limit, false);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertEquals(expectedCapturedDataForRequestLimitReached, testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed not set")
                            class WithEnsureRequestBodyConsumedNotSet {

                                private void startServer() {
                                    UsingReader.this.startServer();
                                }

                                @Test
                                @DisplayName("with no body")
                                void testWithNoBody() {
                                    startServer();

                                    sendGetRequest(equalTo(ConstantServlet.TEXT));

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                    CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.emptyText(true);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, limit, true);
                                    CapturedData expectedCapturedDataForRequestLimitReached = new CapturedData(TEXT, limit, false);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertEquals(expectedCapturedDataForRequestLimitReached, testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }
                        }
                    }

                    @Nested
                    @DisplayName("not using reader")
                    class NotUsingReader {

                        private void startServer() {
                            DoFilter.this.startServer(new ConstantServlet(true, false), limit);
                        }

                        private void startServer(Consumer<FilterHolder> filterConfigurer) {
                            DoFilter.this.startServer(new ConstantServlet(true, false), limit, filterConfigurer);
                        }

                        @Nested
                        @DisplayName("with considerRequestReadAfterContentLength set")
                        class WithConsiderRequestReadAfterContentLengthSet {

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed set")
                            class WithEnsureRequestBodyConsumedSet {

                                private void startServer() {
                                    NotUsingReader.this.startServer(filter -> {
                                        filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true");
                                        filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true");
                                    });
                                }

                                @Test
                                @DisplayName("with no body")
                                void testWithNoBody() {
                                    startServer();

                                    sendGetRequest(equalTo(ConstantServlet.TEXT));

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                    CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                    CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(BYTES, limit, false);
                                    CapturedData expectedCapturedDataForRequestLimitReached = new CapturedData(BYTES, limit, false);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertEquals(expectedCapturedDataForRequestLimitReached, testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed not set")
                            class WithEnsureRequestBodyConsumedNotSet {

                                private void startServer() {
                                    NotUsingReader.this.startServer(
                                            filter -> filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true"));
                                }

                                @Test
                                @DisplayName("with no body")
                                void testWithNoBody() {
                                    startServer();

                                    sendGetRequest(equalTo(ConstantServlet.TEXT));

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                    CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                    CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(BYTES, limit, false);
                                    CapturedData expectedCapturedDataForRequestLimitReached = new CapturedData(BYTES, limit, false);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertEquals(expectedCapturedDataForRequestLimitReached, testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }
                        }

                        @Nested
                        @DisplayName("with considerRequestReadAfterContentLength not set")
                        class WithConsiderRequestReadAfterContentLengthNotSet {

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed set")
                            class WithEnsureRequestBodyConsumedSet {

                                private void startServer() {
                                    NotUsingReader.this.startServer(filter -> filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true"));
                                }

                                @Test
                                @DisplayName("with no body")
                                void testWithNoBody() {
                                    startServer();

                                    sendGetRequest(equalTo(ConstantServlet.TEXT));

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                    CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.emptyBytes(true);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(BYTES, limit, true);
                                    CapturedData expectedCapturedDataForRequestLimitReached = new CapturedData(BYTES, limit, false);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertEquals(expectedCapturedDataForRequestLimitReached, testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed not set")
                            class WithEnsureRequestBodyConsumedNotSet {

                                private void startServer() {
                                    NotUsingReader.this.startServer();
                                }

                                @Test
                                @DisplayName("with no body")
                                void testWithNoBody() {
                                    startServer();

                                    sendGetRequest(equalTo(ConstantServlet.TEXT));

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                    CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.emptyBytes(true);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(BYTES, limit, true);
                                    CapturedData expectedCapturedDataForRequestLimitReached = new CapturedData(BYTES, limit, false);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertEquals(expectedCapturedDataForRequestLimitReached, testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }
                        }
                    }
                }

                @Nested
                @DisplayName("not ensuring body consumed")
                class NotEnsuringBodyConsumed {

                    private void startServer() {
                        DoFilter.this.startServer(new ConstantServlet(false, false), limit);
                    }

                    private void startServer(Consumer<FilterHolder> filterConfigurer) {
                        DoFilter.this.startServer(new ConstantServlet(false, false), limit, filterConfigurer);
                    }

                    @Nested
                    @DisplayName("with considerRequestReadAfterContentLength set")
                    class WithConsiderRequestReadAfterContentLengthSet {

                        @Nested
                        @DisplayName("with ensureRequestBodyConsumed set")
                        class WithEnsureRequestBodyConsumedSet {

                            private void startServer() {
                                NotEnsuringBodyConsumed.this.startServer(filter -> {
                                    filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true");
                                    filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true");
                                });
                            }

                            @Test
                            @DisplayName("with no body")
                            void testWithNoBody() {
                                startServer();

                                sendGetRequest(equalTo(ConstantServlet.TEXT));

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with empty body")
                            void testWithEmptyBody() {
                                startServer();

                                sendRequest();

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with non-empty body")
                            void testWithNonEmptyBody() {
                                startServer();

                                sendRequest(TEXT);

                                CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, limit, false);
                                CapturedData expectedCapturedDataForRequestLimitReached = new CapturedData(TEXT, limit, false);
                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertEquals(expectedCapturedDataForRequestLimitReached, testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }
                        }

                        @Nested
                        @DisplayName("with ensureRequestBodyConsumed not set")
                        class WithEnsureRequestBodyConsumedNotSet {

                            private void startServer() {
                                NotEnsuringBodyConsumed.this.startServer(
                                        filter -> filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true"));
                            }

                            @Test
                            @DisplayName("with no body")
                            void testWithNoBody() {
                                startServer();

                                sendGetRequest(equalTo(ConstantServlet.TEXT));

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with empty body")
                            void testWithEmptyBody() {
                                startServer();

                                sendRequest();

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with non-empty body")
                            void testWithNonEmptyBody() {
                                startServer();

                                sendRequest(TEXT);

                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                assertNull(testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }
                        }
                    }

                    @Nested
                    @DisplayName("with considerRequestReadAfterContentLength not set")
                    class WithConsiderRequestReadAfterContentLengthNotSet {

                        @Nested
                        @DisplayName("with ensureRequestBodyConsumed set")
                        class WithEnsureRequestBodyConsumedSet {

                            private void startServer() {
                                NotEnsuringBodyConsumed.this.startServer(
                                        filter -> filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true"));
                            }

                            @Test
                            @DisplayName("with no body")
                            void testWithNoBody() {
                                startServer();

                                sendGetRequest(equalTo(ConstantServlet.TEXT));

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with empty body")
                            void testWithEmptyBody() {
                                startServer();

                                sendRequest();

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.emptyText(true);
                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with non-empty body")
                            void testWithNonEmptyBody() {
                                startServer();

                                sendRequest(TEXT);

                                CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, limit, true);
                                CapturedData expectedCapturedDataForRequestLimitReached = new CapturedData(TEXT, limit, false);
                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertEquals(expectedCapturedDataForRequestLimitReached, testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }
                        }

                        @Nested
                        @DisplayName("with ensureRequestBodyConsumed not set")
                        class WithEnsureRequestBodyConsumedNotSet {

                            private void startServer() {
                                NotEnsuringBodyConsumed.this.startServer();
                            }

                            @Test
                            @DisplayName("with no body")
                            void testWithNoBody() {
                                startServer();

                                sendGetRequest(equalTo(ConstantServlet.TEXT));

                                CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with empty body")
                            void testWithEmptyBody() {
                                startServer();

                                sendRequest();

                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                assertNull(testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }

                            @Test
                            @DisplayName("with non-empty body")
                            void testWithNonEmptyBody() {
                                startServer();

                                sendRequest(TEXT);

                                CapturedData expectedCapturedDataForResponseBodyProduced = new CapturedData(ConstantServlet.TEXT, false);

                                assertNull(testFilter.capturedDataForRequestBodyRead.get());
                                assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                assertNull(testFilter.capturedDataForResponseLimitReached.get());
                            }
                        }
                    }
                }
            }
        }

        @Nested
        @DisplayName("with response body not written")
        class WithResponseBodyNotWritten {

            private void sendRequest() {
                DoFilter.this.sendPostRequest(equalTo(""));
            }

            private void sendRequest(String body) {
                DoFilter.this.sendPostRequest(body, equalTo(""));
            }

            @Nested
            @DisplayName("not using reset() or resetBuffer()")
            class NotUsingResetOrResetBuffer {

                private void startServer() {
                    DoFilter.this.startServer(new OnlyConsumingServlet(false, false));
                }

                private void startServer(Consumer<FilterHolder> filterConfigurer) {
                    DoFilter.this.startServer(new OnlyConsumingServlet(false, false), filterConfigurer);
                }

                @Nested
                @DisplayName("with no limit")
                class Unlimited {

                    @Nested
                    @DisplayName("ensuring body consumed")
                    class EnsuringBodyConsumed {

                        @Nested
                        @DisplayName("with considerRequestReadAfterContentLength set")
                        class WithConsiderRequestReadAfterContentLengthSet {

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed set")
                            class WithEnsureRequestBodyConsumedSet {

                                private void startServer() {
                                    NotUsingResetOrResetBuffer.this.startServer(filter -> {
                                        filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true");
                                        filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true");
                                    });
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, false);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed not set")
                            class WithEnsureRequestBodyConsumedNotSet {

                                private void startServer() {
                                    NotUsingResetOrResetBuffer.this.startServer(
                                            filter -> filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true"));
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, false);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }
                        }

                        @Nested
                        @DisplayName("with considerRequestReadAfterContentLength not set")
                        class WithConsiderRequestReadAfterContentLengthNotSet {

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed set")
                            class WithEnsureRequestBodyConsumedSet {

                                private void startServer() {
                                    NotUsingResetOrResetBuffer.this.startServer(
                                            filter -> filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true"));
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.emptyText(true);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, true);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed not set")
                            class WithEnsureRequestBodyConsumedNotSet {

                                private void startServer() {
                                    NotUsingResetOrResetBuffer.this.startServer();
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.emptyText(true);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, true);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }
                        }
                    }

                    @Nested
                    @DisplayName("not ensuring body consumed")
                    class NotEnsuringBodyConsumed {

                        @Nested
                        @DisplayName("with considerRequestReadAfterContentLength set")
                        class WithConsiderRequestReadAfterContentLengthSet {

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed set")
                            class WithEnsureRequestBodyConsumedSet {

                                private void startServer() {
                                    NotUsingResetOrResetBuffer.this.startServer(filter -> {
                                        filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true");
                                        filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true");
                                    });
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, false);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed not set")
                            class WithEnsureRequestBodyConsumedNotSet {

                                private void startServer() {
                                    NotUsingResetOrResetBuffer.this.startServer(
                                            filter -> filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true"));
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, false);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }
                        }

                        @Nested
                        @DisplayName("with considerRequestReadAfterContentLength not set")
                        class WithConsiderRequestReadAfterContentLengthNotSet {

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed set")
                            class WithEnsureRequestBodyConsumedSet {

                                private void startServer() {
                                    NotUsingResetOrResetBuffer.this.startServer(
                                            filter -> filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true"));
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.emptyText(true);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, true);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed not set")
                            class WithEnsureRequestBodyConsumedNotSet {

                                private void startServer() {
                                    NotUsingResetOrResetBuffer.this.startServer();
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.emptyText(true);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, true);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }
                        }
                    }
                }

                @Nested
                @DisplayName("with limit")
                class Limited {

                    private int limit = TEXT.length() / 2;

                    private void startServer() {
                        DoFilter.this.startServer(new OnlyConsumingServlet(false, false), limit);
                    }

                    private void startServer(Consumer<FilterHolder> filterConfigurer) {
                        DoFilter.this.startServer(new OnlyConsumingServlet(false, false), limit, filterConfigurer);
                    }

                    @Nested
                    @DisplayName("ensuring body consumed")
                    class EnsuringBodyConsumed {

                        @Nested
                        @DisplayName("with considerRequestReadAfterContentLength set")
                        class WithConsiderRequestReadAfterContentLengthSet {

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed set")
                            class WithEnsureRequestBodyConsumedSet {

                                private void startServer() {
                                    Limited.this.startServer(filter -> {
                                        filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true");
                                        filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true");
                                    });
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, limit, false);
                                    CapturedData expectedCapturedDataForRequestLimitReached = new CapturedData(TEXT, limit, false);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertEquals(expectedCapturedDataForRequestLimitReached, testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed not set")
                            class WithEnsureRequestBodyConsumedNotSet {

                                private void startServer() {
                                    Limited.this.startServer(filter -> filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true"));
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, limit, false);
                                    CapturedData expectedCapturedDataForRequestLimitReached = new CapturedData(TEXT, limit, false);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertEquals(expectedCapturedDataForRequestLimitReached, testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }
                        }

                        @Nested
                        @DisplayName("with considerRequestReadAfterContentLength not set")
                        class WithConsiderRequestReadAfterContentLengthNotSet {

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed set")
                            class WithEnsureRequestBodyConsumedSet {

                                private void startServer() {
                                    Limited.this.startServer(filter -> filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true"));
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.emptyText(true);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, limit, true);
                                    CapturedData expectedCapturedDataForRequestLimitReached = new CapturedData(TEXT, limit, false);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertEquals(expectedCapturedDataForRequestLimitReached, testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed not set")
                            class WithEnsureRequestBodyConsumedNotSet {

                                private void startServer() {
                                    Limited.this.startServer();
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.emptyText(true);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, limit, true);
                                    CapturedData expectedCapturedDataForRequestLimitReached = new CapturedData(TEXT, limit, false);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertEquals(expectedCapturedDataForRequestLimitReached, testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }
                        }
                    }

                    @Nested
                    @DisplayName("not ensuring body consumed")
                    class NotEnsuringBodyConsumed {

                        @Nested
                        @DisplayName("with considerRequestReadAfterContentLength set")
                        class WithConsiderRequestReadAfterContentLengthSet {

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed set")
                            class WithEnsureRequestBodyConsumedSet {

                                private void startServer() {
                                    Limited.this.startServer(filter -> {
                                        filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true");
                                        filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true");
                                    });
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, limit, false);
                                    CapturedData expectedCapturedDataForRequestLimitReached = new CapturedData(TEXT, limit, false);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertEquals(expectedCapturedDataForRequestLimitReached, testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed not set")
                            class WithEnsureRequestBodyConsumedNotSet {

                                private void startServer() {
                                    Limited.this.startServer(filter -> filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true"));
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, limit, false);
                                    CapturedData expectedCapturedDataForRequestLimitReached = new CapturedData(TEXT, limit, false);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertEquals(expectedCapturedDataForRequestLimitReached, testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }
                        }

                        @Nested
                        @DisplayName("with considerRequestReadAfterContentLength not set")
                        class WithConsiderRequestReadAfterContentLengthNotSet {

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed set")
                            class WithEnsureRequestBodyConsumedSet {

                                private void startServer() {
                                    Limited.this.startServer(filter -> filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true"));
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.emptyText(true);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, limit, true);
                                    CapturedData expectedCapturedDataForRequestLimitReached = new CapturedData(TEXT, limit, false);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertEquals(expectedCapturedDataForRequestLimitReached, testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed not set")
                            class WithEnsureRequestBodyConsumedNotSet {

                                private void startServer() {
                                    Limited.this.startServer();
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.emptyText(true);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, limit, true);
                                    CapturedData expectedCapturedDataForRequestLimitReached = new CapturedData(TEXT, limit, false);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertEquals(expectedCapturedDataForRequestLimitReached, testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }
                        }
                    }
                }
            }

            @Nested
            @DisplayName("using resetBuffer()")
            class UsingResetBuffer {

                private void startServer() {
                    DoFilter.this.startServer(new OnlyConsumingServlet(false, true));
                }

                private void startServer(Consumer<FilterHolder> filterConfigurer) {
                    DoFilter.this.startServer(new OnlyConsumingServlet(false, true), filterConfigurer);
                }

                @Nested
                @DisplayName("with no limit")
                class Unlimited {

                    @Nested
                    @DisplayName("ensuring body consumed")
                    class EnsuringBodyConsumed {

                        @Nested
                        @DisplayName("with considerRequestReadAfterContentLength set")
                        class WithConsiderRequestReadAfterContentLengthSet {

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed set")
                            class WithEnsureRequestBodyConsumedSet {

                                private void startServer() {
                                    UsingResetBuffer.this.startServer(filter -> {
                                        filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true");
                                        filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true");
                                    });
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, false);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed not set")
                            class WithEnsureRequestBodyConsumedNotSet {

                                private void startServer() {
                                    UsingResetBuffer.this.startServer(
                                            filter -> filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true"));
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, false);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }
                        }

                        @Nested
                        @DisplayName("with considerRequestReadAfterContentLength not set")
                        class WithConsiderRequestReadAfterContentLengthNotSet {

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed set")
                            class WithEnsureRequestBodyConsumedSet {

                                private void startServer() {
                                    UsingResetBuffer.this.startServer(filter -> filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true"));
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.emptyText(true);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, true);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed not set")
                            class WithEnsureRequestBodyConsumedNotSet {

                                private void startServer() {
                                    UsingResetBuffer.this.startServer();
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.emptyText(true);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, true);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }
                        }
                    }

                    @Nested
                    @DisplayName("not ensuring body consumed")
                    class NotEnsuringBodyConsumed {

                        @Nested
                        @DisplayName("with considerRequestReadAfterContentLength set")
                        class WithConsiderRequestReadAfterContentLengthSet {

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed set")
                            class WithEnsureRequestBodyConsumedSet {

                                private void startServer() {
                                    UsingResetBuffer.this.startServer(filter -> {
                                        filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true");
                                        filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true");
                                    });
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, false);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed not set")
                            class WithEnsureRequestBodyConsumedNotSet {

                                private void startServer() {
                                    UsingResetBuffer.this.startServer(
                                            filter -> filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true"));
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, false);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }
                        }

                        @Nested
                        @DisplayName("with considerRequestReadAfterContentLength not set")
                        class WithConsiderRequestReadAfterContentLengthNotSet {

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed set")
                            class WithEnsureRequestBodyConsumedSet {

                                private void startServer() {
                                    UsingResetBuffer.this.startServer(filter -> filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true"));
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.emptyText(true);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, true);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed not set")
                            class WithEnsureRequestBodyConsumedNotSet {

                                private void startServer() {
                                    UsingResetBuffer.this.startServer();
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.emptyText(true);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, true);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }
                        }
                    }
                }

                @Nested
                @DisplayName("with limit")
                class Limited {

                    private int limit = TEXT.length() / 2;

                    private void startServer() {
                        DoFilter.this.startServer(new OnlyConsumingServlet(false, true), limit);
                    }

                    private void startServer(Consumer<FilterHolder> filterConfigurer) {
                        DoFilter.this.startServer(new OnlyConsumingServlet(false, true), limit, filterConfigurer);
                    }

                    @Nested
                    @DisplayName("ensuring body consumed")
                    class EnsuringBodyConsumed {

                        @Nested
                        @DisplayName("with considerRequestReadAfterContentLength set")
                        class WithConsiderRequestReadAfterContentLengthSet {

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed set")
                            class WithEnsureRequestBodyConsumedSet {

                                private void startServer() {
                                    Limited.this.startServer(filter -> {
                                        filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true");
                                        filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true");
                                    });
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, limit, false);
                                    CapturedData expectedCapturedDataForRequestLimitReached = new CapturedData(TEXT, limit, false);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertEquals(expectedCapturedDataForRequestLimitReached, testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed not set")
                            class WithEnsureRequestBodyConsumedNotSet {

                                private void startServer() {
                                    Limited.this.startServer(filter -> filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true"));
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, limit, false);
                                    CapturedData expectedCapturedDataForRequestLimitReached = new CapturedData(TEXT, limit, false);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertEquals(expectedCapturedDataForRequestLimitReached, testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }
                        }

                        @Nested
                        @DisplayName("with considerRequestReadAfterContentLength not set")
                        class WithConsiderRequestReadAfterContentLengthNotSet {

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed set")
                            class WithEnsureRequestBodyConsumedSet {

                                private void startServer() {
                                    Limited.this.startServer(filter -> filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true"));
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.emptyText(true);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, limit, true);
                                    CapturedData expectedCapturedDataForRequestLimitReached = new CapturedData(TEXT, limit, false);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertEquals(expectedCapturedDataForRequestLimitReached, testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed not set")
                            class WithEnsureRequestBodyConsumedNotSet {

                                private void startServer() {
                                    Limited.this.startServer();
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.emptyText(true);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, limit, true);
                                    CapturedData expectedCapturedDataForRequestLimitReached = new CapturedData(TEXT, limit, false);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertEquals(expectedCapturedDataForRequestLimitReached, testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }
                        }
                    }

                    @Nested
                    @DisplayName("not ensuring body consumed")
                    class NotEnsuringBodyConsumed {

                        @Nested
                        @DisplayName("with considerRequestReadAfterContentLength set")
                        class WithConsiderRequestReadAfterContentLengthSet {

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed set")
                            class WithEnsureRequestBodyConsumedSet {

                                private void startServer() {
                                    Limited.this.startServer(filter -> {
                                        filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true");
                                        filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true");
                                    });
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, limit, false);
                                    CapturedData expectedCapturedDataForRequestLimitReached = new CapturedData(TEXT, limit, false);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertEquals(expectedCapturedDataForRequestLimitReached, testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed not set")
                            class WithEnsureRequestBodyConsumedNotSet {

                                private void startServer() {
                                    Limited.this.startServer(filter -> filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true"));
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, limit, false);
                                    CapturedData expectedCapturedDataForRequestLimitReached = new CapturedData(TEXT, limit, false);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertEquals(expectedCapturedDataForRequestLimitReached, testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }
                        }

                        @Nested
                        @DisplayName("with considerRequestReadAfterContentLength not set")
                        class WithConsiderRequestReadAfterContentLengthNotSet {

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed set")
                            class WithEnsureRequestBodyConsumedSet {

                                private void startServer() {
                                    Limited.this.startServer(filter -> filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true"));
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.emptyText(true);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, limit, true);
                                    CapturedData expectedCapturedDataForRequestLimitReached = new CapturedData(TEXT, limit, false);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertEquals(expectedCapturedDataForRequestLimitReached, testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed not set")
                            class WithEnsureRequestBodyConsumedNotSet {

                                private void startServer() {
                                    Limited.this.startServer();
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.emptyText(true);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, limit, true);
                                    CapturedData expectedCapturedDataForRequestLimitReached = new CapturedData(TEXT, limit, false);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertEquals(expectedCapturedDataForRequestLimitReached, testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }
                        }
                    }
                }
            }

            @Nested
            @DisplayName("using reset()")
            class UsingReset {

                private void startServer() {
                    DoFilter.this.startServer(new OnlyConsumingServlet(true, false));
                }

                private void startServer(Consumer<FilterHolder> filterConfigurer) {
                    DoFilter.this.startServer(new OnlyConsumingServlet(true, false), filterConfigurer);
                }

                @Nested
                @DisplayName("with no limit")
                class Unlimited {

                    @Nested
                    @DisplayName("ensuring body consumed")
                    class EnsuringBodyConsumed {

                        @Nested
                        @DisplayName("with considerRequestReadAfterContentLength set")
                        class WithConsiderRequestReadAfterContentLengthSet {

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed set")
                            class WithEnsureRequestBodyConsumedSet {

                                private void startServer() {
                                    UsingReset.this.startServer(filter -> {
                                        filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true");
                                        filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true");
                                    });
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, false);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed not set")
                            class WithEnsureRequestBodyConsumedNotSet {

                                private void startServer() {
                                    UsingReset.this.startServer(
                                            filter -> filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true"));
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, false);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }
                        }

                        @Nested
                        @DisplayName("with considerRequestReadAfterContentLength not set")
                        class WithConsiderRequestReadAfterContentLengthNotSet {

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed set")
                            class WithEnsureRequestBodyConsumedSet {

                                private void startServer() {
                                    UsingReset.this.startServer(filter -> filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true"));
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.emptyText(true);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, true);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed not set")
                            class WithEnsureRequestBodyConsumedNotSet {

                                private void startServer() {
                                    UsingReset.this.startServer();
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.emptyText(true);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, true);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }
                        }
                    }

                    @Nested
                    @DisplayName("not ensuring body consumed")
                    class NotEnsuringBodyConsumed {

                        @Nested
                        @DisplayName("with considerRequestReadAfterContentLength set")
                        class WithConsiderRequestReadAfterContentLengthSet {

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed set")
                            class WithEnsureRequestBodyConsumedSet {

                                private void startServer() {
                                    UsingReset.this.startServer(filter -> {
                                        filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true");
                                        filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true");
                                    });
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, false);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed not set")
                            class WithEnsureRequestBodyConsumedNotSet {

                                private void startServer() {
                                    UsingReset.this.startServer(
                                            filter -> filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true"));
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, false);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }
                        }

                        @Nested
                        @DisplayName("with considerRequestReadAfterContentLength not set")
                        class WithConsiderRequestReadAfterContentLengthNotSet {

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed set")
                            class WithEnsureRequestBodyConsumedSet {

                                private void startServer() {
                                    UsingReset.this.startServer(filter -> filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true"));
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.emptyText(true);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, true);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed not set")
                            class WithEnsureRequestBodyConsumedNotSet {

                                private void startServer() {
                                    UsingReset.this.startServer();
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.emptyText(true);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, true);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }
                        }
                    }
                }

                @Nested
                @DisplayName("with limit")
                class Limited {

                    private int limit = TEXT.length() / 2;

                    private void startServer() {
                        DoFilter.this.startServer(new OnlyConsumingServlet(true, false), limit);
                    }

                    private void startServer(Consumer<FilterHolder> filterConfigurer) {
                        DoFilter.this.startServer(new OnlyConsumingServlet(true, false), limit, filterConfigurer);
                    }

                    @Nested
                    @DisplayName("ensuring body consumed")
                    class EnsuringBodyConsumed {

                        @Nested
                        @DisplayName("with considerRequestReadAfterContentLength set")
                        class WithConsiderRequestReadAfterContentLengthSet {

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed set")
                            class WithEnsureRequestBodyConsumedSet {

                                private void startServer() {
                                    Limited.this.startServer(filter -> {
                                        filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true");
                                        filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true");
                                    });
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, limit, false);
                                    CapturedData expectedCapturedDataForRequestLimitReached = new CapturedData(TEXT, limit, false);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertEquals(expectedCapturedDataForRequestLimitReached, testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed not set")
                            class WithEnsureRequestBodyConsumedNotSet {

                                private void startServer() {
                                    Limited.this.startServer(filter -> filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true"));
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, limit, false);
                                    CapturedData expectedCapturedDataForRequestLimitReached = new CapturedData(TEXT, limit, false);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertEquals(expectedCapturedDataForRequestLimitReached, testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }
                        }

                        @Nested
                        @DisplayName("with considerRequestReadAfterContentLength not set")
                        class WithConsiderRequestReadAfterContentLengthNotSet {

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed set")
                            class WithEnsureRequestBodyConsumedSet {

                                private void startServer() {
                                    Limited.this.startServer(filter -> filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true"));
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.emptyText(true);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, limit, true);
                                    CapturedData expectedCapturedDataForRequestLimitReached = new CapturedData(TEXT, limit, false);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertEquals(expectedCapturedDataForRequestLimitReached, testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed not set")
                            class WithEnsureRequestBodyConsumedNotSet {

                                private void startServer() {
                                    Limited.this.startServer();
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.emptyText(true);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, limit, true);
                                    CapturedData expectedCapturedDataForRequestLimitReached = new CapturedData(TEXT, limit, false);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertEquals(expectedCapturedDataForRequestLimitReached, testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }
                        }
                    }

                    @Nested
                    @DisplayName("not ensuring body consumed")
                    class NotEnsuringBodyConsumed {

                        @Nested
                        @DisplayName("with considerRequestReadAfterContentLength set")
                        class WithConsiderRequestReadAfterContentLengthSet {

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed set")
                            class WithEnsureRequestBodyConsumedSet {

                                private void startServer() {
                                    Limited.this.startServer(filter -> {
                                        filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true");
                                        filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true");
                                    });
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, limit, false);
                                    CapturedData expectedCapturedDataForRequestLimitReached = new CapturedData(TEXT, limit, false);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertEquals(expectedCapturedDataForRequestLimitReached, testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed not set")
                            class WithEnsureRequestBodyConsumedNotSet {

                                private void startServer() {
                                    Limited.this.startServer(filter -> filter.setInitParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, "true"));
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.none();
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, limit, false);
                                    CapturedData expectedCapturedDataForRequestLimitReached = new CapturedData(TEXT, limit, false);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertEquals(expectedCapturedDataForRequestLimitReached, testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }
                        }

                        @Nested
                        @DisplayName("with considerRequestReadAfterContentLength not set")
                        class WithConsiderRequestReadAfterContentLengthNotSet {

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed set")
                            class WithEnsureRequestBodyConsumedSet {

                                private void startServer() {
                                    Limited.this.startServer(filter -> filter.setInitParameter(ENSURE_REQUEST_BODY_CONSUMED, "true"));
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.emptyText(true);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, limit, true);
                                    CapturedData expectedCapturedDataForRequestLimitReached = new CapturedData(TEXT, limit, false);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertEquals(expectedCapturedDataForRequestLimitReached, testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }

                            @Nested
                            @DisplayName("with ensureRequestBodyConsumed not set")
                            class WithEnsureRequestBodyConsumedNotSet {

                                private void startServer() {
                                    Limited.this.startServer();
                                }

                                @Test
                                @DisplayName("with empty body")
                                void testWithEmptyBody() {
                                    startServer();

                                    sendRequest();

                                    CapturedData expectedCapturedDataForRequestBodyRead = CapturedData.emptyText(true);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertNull(testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }

                                @Test
                                @DisplayName("with non-empty body")
                                void testWithNonEmptyBody() {
                                    startServer();

                                    sendRequest(TEXT);

                                    CapturedData expectedCapturedDataForRequestBodyRead = new CapturedData(TEXT, limit, true);
                                    CapturedData expectedCapturedDataForRequestLimitReached = new CapturedData(TEXT, limit, false);
                                    CapturedData expectedCapturedDataForResponseBodyProduced = CapturedData.none();

                                    assertEquals(expectedCapturedDataForRequestBodyRead, testFilter.capturedDataForRequestBodyRead.get());
                                    assertEquals(expectedCapturedDataForRequestLimitReached, testFilter.capturedDataForRequestLimitReached.get());
                                    assertEquals(expectedCapturedDataForResponseBodyProduced, testFilter.capturedDataForResponseBodyProduced.get());
                                    assertNull(testFilter.capturedDataForResponseLimitReached.get());
                                }
                            }
                        }
                    }
                }
            }
        }

        @Nested
        @DisplayName("IllegalStateExceptions")
        class IllegalStateExceptions {

            @Test
            @DisplayName("capturedTextBody for binary capture")
            void testCapturedTextBodyForBinaryCapture() {
                DoFilter.super.startServer(context -> {
                    ServletHolder servlet = new ServletHolder(new BinaryEchoServlet(false));
                    context.addServlet(servlet, "/*");

                    FilterHolder filter = new FilterHolder(new IllegalStateTestingFilter(
                            request -> assertThrows(IllegalStateException.class, request::capturedTextBody),
                            response -> assertThrows(IllegalStateException.class, response::capturedTextBody)));
                    context.addFilter(filter, "/*", EnumSet.allOf(DispatcherType.class));
                });

                sendPostRequest(TEXT);
            }

            @Test
            @DisplayName("capturedBinaryBody for text capture")
            void testCapturedBinaryBodyForTextCapture() {
                DoFilter.super.startServer(context -> {
                    ServletHolder servlet = new ServletHolder(new TextEchoServlet(false));
                    context.addServlet(servlet, "/*");

                    FilterHolder filter = new FilterHolder(new IllegalStateTestingFilter(
                            request -> assertThrows(IllegalStateException.class, request::capturedBinaryBody),
                            response -> assertThrows(IllegalStateException.class, response::capturedBinaryBody)));
                    context.addFilter(filter, "/*", EnumSet.allOf(DispatcherType.class));
                });

                sendPostRequest(TEXT);
            }

            @Test
            @DisplayName("capturedBinaryBodyAsString for text capture")
            void testCapturedBinaryBodyAsStringForTextCapture() {
                DoFilter.super.startServer(context -> {
                    ServletHolder servlet = new ServletHolder(new TextEchoServlet(false));
                    context.addServlet(servlet, "/*");

                    FilterHolder filter = new FilterHolder(new IllegalStateTestingFilter(
                            request -> assertThrows(IllegalStateException.class, request::capturedBinaryBodyAsString),
                            response -> assertThrows(IllegalStateException.class, response::capturedBinaryBodyAsString)));
                    context.addFilter(filter, "/*", EnumSet.allOf(DispatcherType.class));
                });

                sendPostRequest(TEXT);
            }
        }
    }

    @Nested
    @DisplayName("initialRequestCapacity(HttpServletRequest)")
    class InitialCapacity {

        private FilterConfig filterConfig;

        @BeforeEach
        void initFilterConfig() {
            filterConfig = mock(FilterConfig.class);
            when(filterConfig.getServletContext()).thenReturn(mock(ServletContext.class));
        }

        @Test
        void testInitialRequestCapacityFromContentLength() {
            when(filterConfig.getInitParameter(INITIAL_REQUEST_CAPACITY_FROM_CONTENT_LENGTH)).thenReturn("true");

            BodyCapturingFilter filter = new TestFilter();
            filter.init(filterConfig);

            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getContentLengthLong()).thenReturn(13L);

            assertEquals(13, filter.initialRequestCapacity(request));

            verify(request).getContentLengthLong();
            verifyNoMoreInteractions(request);
        }

        @Test
        void testInitialRequestCapacityFromMissingContentLength() {
            when(filterConfig.getInitParameter(INITIAL_REQUEST_CAPACITY_FROM_CONTENT_LENGTH)).thenReturn("true");

            BodyCapturingFilter filter = new TestFilter();
            filter.init(filterConfig);

            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getContentLengthLong()).thenReturn(-1L);

            assertEquals(DEFAULT_INITIAL_CAPACITY, filter.initialRequestCapacity(request));

            verify(request).getContentLengthLong();
            verifyNoMoreInteractions(request);
        }

        @Test
        void testInitialRequestCapacityFromInitParameter() {
            when(filterConfig.getInitParameter(INITIAL_REQUEST_CAPACITY)).thenReturn("13");

            BodyCapturingFilter filter = new TestFilter();
            filter.init(filterConfig);

            HttpServletRequest request = mock(HttpServletRequest.class);

            assertEquals(13, filter.initialRequestCapacity(request));

            verifyNoMoreInteractions(request);
        }
    }

    @Nested
    @DisplayName("ensureBodyConsumed(HttpServletRequest, boolean)")
    class EnsureBodyConsumed {

        private BodyCapturingFilter filter;

        @BeforeEach
        void initFilter() {
            FilterConfig filterConfig = mock(FilterConfig.class);
            when(filterConfig.getServletContext()).thenReturn(mock(ServletContext.class));

            filter = new TestFilter();
            filter.init(filterConfig);
        }

        @Nested
        @DisplayName("using reader")
        class UsingReader {

            private BufferedReader reader;
            private HttpServletRequest request;

            @BeforeEach
            @SuppressWarnings("resource")
            void initRequest() throws IOException {
                reader = new BufferedReader(new StringReader(TEXT));

                request = mock(HttpServletRequest.class);
                doThrow(IllegalStateException.class).when(request).getInputStream();
                doReturn(reader).when(request).getReader();
            }

            @Test
            @DisplayName("with direct BodyCapturingRequest")
            void testWithDirectBodyCapturingRequest() throws IOException {
                BodyCapturingRequest bodyCapturingRequest = filter.new BodyCapturingRequest(request, false);

                BodyCapturingFilter.ensureBodyConsumed(bodyCapturingRequest, true);

                assertEquals(TEXT, bodyCapturingRequest.capturedTextBody());
                assertEquals(-1, reader.read());
            }

            @Test
            @DisplayName("with nested BodyCapturingRequest")
            void testWithNestedBodyCapturingRequest() throws IOException {
                BodyCapturingRequest bodyCapturingRequest = filter.new BodyCapturingRequest(request, false);

                HttpServletRequest wrapper = new HttpServletRequestWrapper(new HttpServletRequestWrapper(bodyCapturingRequest));

                BodyCapturingFilter.ensureBodyConsumed(wrapper, true);

                assertEquals(TEXT, bodyCapturingRequest.capturedTextBody());
                assertEquals(-1, reader.read());
            }

            @Nested
            @DisplayName("with non-BodyCapturingRequest")
            class WithNonBodyCapturingRequest {

                @Test
                @DisplayName("getInputStream() not yet called")
                void testGetInputStreamNotYetCalled() throws IOException {
                    HttpServletRequest wrapper = new HttpServletRequestWrapper(new HttpServletRequestWrapper(request));

                    BodyCapturingFilter.ensureBodyConsumed(wrapper, true);

                    assertEquals(-1, reader.read());
                }

                @Test
                @DisplayName("getInputStream() already called")
                @SuppressWarnings("resource")
                void testGetInputStreamAlreadyCalled() throws IOException {
                    ByteArrayInputStream inputStream = new ByteArrayInputStream(BYTES);

                    doThrow(IllegalStateException.class).when(request).getReader();
                    doReturn(ServletUtils.transform(mock(ServletInputStream.class), i -> inputStream)).when(request).getInputStream();

                    HttpServletRequest wrapper = new HttpServletRequestWrapper(new HttpServletRequestWrapper(request));

                    BodyCapturingFilter.ensureBodyConsumed(wrapper, true);

                    assertEquals(-1, inputStream.read());
                }
            }
        }

        @Nested
        @DisplayName("not using reader")
        class NotUsingReader {

            private InputStream inputStream;
            private HttpServletRequest request;

            @BeforeEach
            @SuppressWarnings("resource")
            void initRequest() throws IOException {
                inputStream = new ByteArrayInputStream(BYTES);

                request = mock(HttpServletRequest.class);
                doReturn(ServletUtils.transform(mock(ServletInputStream.class), i -> inputStream)).when(request).getInputStream();
                doThrow(IllegalStateException.class).when(request).getReader();
            }

            @Test
            @DisplayName("with direct BodyCapturingRequest")
            void testWithDirectBodyCapturingRequest() throws IOException {
                BodyCapturingRequest bodyCapturingRequest = filter.new BodyCapturingRequest(request, false);

                BodyCapturingFilter.ensureBodyConsumed(bodyCapturingRequest, false);

                assertArrayEquals(BYTES, bodyCapturingRequest.capturedBinaryBody());
                assertEquals(-1, inputStream.read());
            }

            @Test
            @DisplayName("with nested BodyCapturingRequest")
            void testWithNestedBodyCapturingRequest() throws IOException {
                BodyCapturingRequest bodyCapturingRequest = filter.new BodyCapturingRequest(request, false);

                HttpServletRequest wrapper = new HttpServletRequestWrapper(new HttpServletRequestWrapper(bodyCapturingRequest));

                BodyCapturingFilter.ensureBodyConsumed(wrapper, false);

                assertArrayEquals(BYTES, bodyCapturingRequest.capturedBinaryBody());
                assertEquals(-1, inputStream.read());
            }

            @Nested
            @DisplayName("with non-BodyCapturingRequest")
            class WithNonBodyCapturingRequest {

                @Test
                @DisplayName("getReader() not yet called")
                void testGetReaderNotYetCalled() throws IOException {
                    HttpServletRequest wrapper = new HttpServletRequestWrapper(new HttpServletRequestWrapper(request));

                    BodyCapturingFilter.ensureBodyConsumed(wrapper, false);

                    assertEquals(-1, inputStream.read());
                }

                @Test
                @DisplayName("getReader() already called")
                @SuppressWarnings("resource")
                void testGetReaderAlreadyCalled() throws IOException {
                    BufferedReader reader = new BufferedReader(new StringReader(TEXT));

                    doThrow(IllegalStateException.class).when(request).getInputStream();
                    doReturn(reader).when(request).getReader();

                    HttpServletRequest wrapper = new HttpServletRequestWrapper(new HttpServletRequestWrapper(request));

                    BodyCapturingFilter.ensureBodyConsumed(wrapper, false);

                    assertEquals(-1, reader.read());
                }
            }
        }
    }

    @Nested
    class CapturingInputStreamTest {

        @Nested
        class Unlimited {

            @Test
            @DisplayName("read()")
            void testReadByte() throws IOException {
                try (InputStream input = createInputStream()) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream(BYTES.length);

                    int b;
                    while ((b = input.read()) != -1) {
                        baos.write(b);
                    }
                    assertEquals(-1, input.read());
                    assertArrayEquals(BYTES, baos.toByteArray());
                }
            }

            @Test
            @DisplayName("read(byte[])")
            void testReadIntoByteArray() throws IOException {
                try (InputStream input = createInputStream()) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream(BYTES.length);

                    byte[] buffer = new byte[10];
                    int len;
                    while ((len = input.read(buffer)) != -1) {
                        baos.write(buffer, 0, len);
                    }
                    assertEquals(-1, input.read(buffer));
                    assertArrayEquals(BYTES, baos.toByteArray());
                }
            }

            @Test
            @DisplayName("read(byte[], int, int)")
            void testReadIntoByteArrayPortion() throws IOException {
                try (InputStream input = createInputStream()) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream(BYTES.length);

                    byte[] buffer = new byte[20];
                    int len;
                    while ((len = input.read(buffer, 5, 10)) != -1) {
                        baos.write(buffer, 5, len);
                    }
                    assertEquals(-1, input.read(buffer, 5, 10));
                    assertArrayEquals(BYTES, baos.toByteArray());
                }
            }

            @Test
            @DisplayName("mark and reset")
            void testMarkAndReset() throws IOException {
                try (InputStream input = createInputStream()) {
                    assertTrue(input.markSupported());

                    // mark, read 5, reset, read 10, repeat
                    final int readSize = 5;

                    ByteArrayOutputStream expectedContent = new ByteArrayOutputStream(BYTES.length * 3 / 2);
                    for (int i = 0; i < BYTES.length; i += readSize * 2) {
                        expectedContent.write(BYTES, i, Math.min(readSize, BYTES.length - i));
                        expectedContent.write(BYTES, i, Math.min(readSize * 2, BYTES.length - i));
                    }

                    ByteArrayOutputStream baos = new ByteArrayOutputStream(expectedContent.size());

                    byte[] markedBuffer = new byte[readSize];
                    byte[] buffer = new byte[readSize * 2];
                    int len;
                    input.mark(readSize);
                    while ((len = input.read(markedBuffer)) != -1) {
                        baos.write(markedBuffer, 0, len);
                        input.reset();

                        len = input.read(buffer);
                        if (len != -1) {
                            baos.write(buffer, 0, len);
                            input.mark(readSize);
                        }
                    }
                    assertArrayEquals(expectedContent.toByteArray(), baos.toByteArray());
                }
            }

            @Test
            @DisplayName("close twice")
            void testCloseTwice() throws IOException {
                AtomicInteger counter = new AtomicInteger(0);
                Consumer<BodyCapturingInputStream> callback = input -> {
                    counter.incrementAndGet();
                    assertFalse(input.isConsumed());
                };

                try (InputStream input = createInputStream(callback)) {
                    input.close();
                }
                assertEquals(1, counter.get());
            }

            private InputStream createInputStream() {
                return createInputStream(input -> {
                    assertArrayEquals(BYTES, input.captured());
                    assertEquals(TEXT, input.captured(StandardCharsets.UTF_8));
                    assertEquals(BYTES.length, input.totalBytes());
                    assertTrue(input.isConsumed());
                });
            }

            private InputStream createInputStream(Consumer<BodyCapturingInputStream> doneCallback) {
                AtomicInteger doneCount = new AtomicInteger(0);
                AtomicInteger limitReachedCount = new AtomicInteger(0);

                return new BodyCapturingInputStream(new ByteArrayInputStream(BYTES),
                        DEFAULT_INITIAL_CAPACITY, Integer.MAX_VALUE, Long.MAX_VALUE,
                        input -> {
                            assertEquals(0, doneCount.getAndIncrement());
                            assertEquals(0, limitReachedCount.get());
                            doneCallback.accept(input);
                        },
                        input -> {
                            assertEquals(0, doneCount.get());
                            assertEquals(0, limitReachedCount.getAndIncrement());
                        });
            }
        }

        @Nested
        class Limited {

            @Test
            @DisplayName("read()")
            void testReadByte() throws IOException {
                try (InputStream input = createInputStream()) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream(BYTES.length);

                    int b;
                    while ((b = input.read()) != -1) {
                        baos.write(b);
                    }
                    assertEquals(-1, input.read());
                    assertArrayEquals(BYTES, baos.toByteArray());
                }
            }

            @Test
            @DisplayName("read(byte[])")
            void testReadIntoByteArray() throws IOException {
                try (InputStream input = createInputStream()) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream(BYTES.length);

                    byte[] buffer = new byte[10];
                    int len;
                    while ((len = input.read(buffer)) != -1) {
                        baos.write(buffer, 0, len);
                    }
                    assertEquals(-1, input.read(buffer));
                    assertArrayEquals(BYTES, baos.toByteArray());
                }
            }

            @Test
            @DisplayName("read(byte[], int, int)")
            void testReadIntoByteArrayPortion() throws IOException {
                try (InputStream input = createInputStream()) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream(BYTES.length);

                    byte[] buffer = new byte[20];
                    int len;
                    while ((len = input.read(buffer, 5, 10)) != -1) {
                        baos.write(buffer, 5, len);
                    }
                    assertEquals(-1, input.read(buffer, 5, 10));
                    assertArrayEquals(BYTES, baos.toByteArray());
                }
            }

            @Test
            @DisplayName("mark and reset")
            void testMarkAndReset() throws IOException {
                try (InputStream input = createInputStream()) {
                    assertTrue(input.markSupported());

                    // mark, read 5, reset, read 10, repeat
                    final int readSize = 5;

                    ByteArrayOutputStream expectedContent = new ByteArrayOutputStream(BYTES.length * 3 / 2);
                    for (int i = 0; i < BYTES.length; i += readSize * 2) {
                        expectedContent.write(BYTES, i, Math.min(readSize, BYTES.length - i));
                        expectedContent.write(BYTES, i, Math.min(readSize * 2, BYTES.length - i));
                    }

                    ByteArrayOutputStream baos = new ByteArrayOutputStream(expectedContent.size());

                    byte[] markedBuffer = new byte[readSize];
                    byte[] buffer = new byte[readSize * 2];
                    int len;
                    input.mark(readSize);
                    while ((len = input.read(markedBuffer)) != -1) {
                        baos.write(markedBuffer, 0, len);
                        input.reset();

                        len = input.read(buffer);
                        if (len != -1) {
                            baos.write(buffer, 0, len);
                            input.mark(readSize);
                        }
                    }
                    assertArrayEquals(expectedContent.toByteArray(), baos.toByteArray());
                }
            }

            @Test
            @DisplayName("close twice")
            void testCloseTwice() throws IOException {
                AtomicInteger counter = new AtomicInteger(0);
                Consumer<BodyCapturingInputStream> callback = input -> {
                    counter.incrementAndGet();
                    assertFalse(input.isConsumed());
                };

                try (InputStream input = createInputStream(13, callback)) {
                    input.close();
                }
                assertEquals(1, counter.get());
            }

            private InputStream createInputStream() {
                int limit = 13;
                AtomicInteger limitReachedCount = new AtomicInteger(0);

                Consumer<BodyCapturingInputStream> doneCallback = input -> {
                    assertEquals(1, limitReachedCount.get());
                    assertArrayEquals(Arrays.copyOfRange(BYTES, 0, limit), input.captured());
                    assertEquals(TEXT.substring(0, limit), input.captured(StandardCharsets.UTF_8));
                    assertEquals(BYTES.length, input.totalBytes());
                };
                Consumer<BodyCapturingInputStream> limitReachedCallback = input -> assertEquals(0, limitReachedCount.getAndIncrement());
                return createInputStream(limit, doneCallback, limitReachedCallback);
            }

            private InputStream createInputStream(int limit, Consumer<BodyCapturingInputStream> doneCallback) {
                Consumer<BodyCapturingInputStream> limitReachedCallback = input -> { /* do nothing */ };
                return createInputStream(limit, doneCallback, limitReachedCallback);
            }

            private InputStream createInputStream(int limit, Consumer<BodyCapturingInputStream> doneCallback,
                    Consumer<BodyCapturingInputStream> limitReachedCallback) {

                AtomicInteger doneCount = new AtomicInteger(0);

                return new BodyCapturingInputStream(new ByteArrayInputStream(BYTES),
                        DEFAULT_INITIAL_CAPACITY, limit, Long.MAX_VALUE,
                        input -> {
                            assertEquals(0, doneCount.getAndIncrement());
                            doneCallback.accept(input);
                        },
                        input -> {
                            assertEquals(0, doneCount.get());
                            limitReachedCallback.accept(input);
                        });
            }
        }

        @Nested
        class DoneAfter {

            @Test
            @DisplayName("read()")
            void testReadByte() throws IOException {
                try (InputStream input = createInputStream()) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream(BYTES.length);

                    int b;
                    while ((b = input.read()) != -1) {
                        baos.write(b);
                    }
                    assertEquals(-1, input.read());
                    assertArrayEquals(BYTES, baos.toByteArray());
                }
            }

            @Test
            @DisplayName("read(byte[])")
            void testReadIntoByteArray() throws IOException {
                try (InputStream input = createInputStream()) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream(BYTES.length);

                    byte[] buffer = new byte[10];
                    int len;
                    while ((len = input.read(buffer)) != -1) {
                        baos.write(buffer, 0, len);
                    }
                    assertEquals(-1, input.read(buffer));
                    assertArrayEquals(BYTES, baos.toByteArray());
                }
            }

            @Test
            @DisplayName("read(byte[], int, int)")
            void testReadIntoByteArrayPortion() throws IOException {
                try (InputStream input = createInputStream()) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream(BYTES.length);

                    byte[] buffer = new byte[20];
                    int len;
                    while ((len = input.read(buffer, 5, 10)) != -1) {
                        baos.write(buffer, 5, len);
                    }
                    assertEquals(-1, input.read(buffer, 5, 10));
                    assertArrayEquals(BYTES, baos.toByteArray());
                }
            }

            @Test
            @DisplayName("mark and reset")
            void testMarkAndReset() throws IOException {
                try (InputStream input = createInputStream()) {
                    assertTrue(input.markSupported());

                    // mark, read 5, reset, read 10, repeat
                    final int readSize = 5;

                    ByteArrayOutputStream expectedContent = new ByteArrayOutputStream(BYTES.length * 3 / 2);
                    for (int i = 0; i < BYTES.length; i += readSize * 2) {
                        expectedContent.write(BYTES, i, Math.min(readSize, BYTES.length - i));
                        expectedContent.write(BYTES, i, Math.min(readSize * 2, BYTES.length - i));
                    }

                    ByteArrayOutputStream baos = new ByteArrayOutputStream(expectedContent.size());

                    byte[] markedBuffer = new byte[readSize];
                    byte[] buffer = new byte[readSize * 2];
                    int len;
                    input.mark(readSize);
                    while ((len = input.read(markedBuffer)) != -1) {
                        baos.write(markedBuffer, 0, len);
                        input.reset();

                        len = input.read(buffer);
                        if (len != -1) {
                            baos.write(buffer, 0, len);
                            input.mark(readSize);
                        }
                    }
                    assertArrayEquals(expectedContent.toByteArray(), baos.toByteArray());
                }
            }

            @Test
            @DisplayName("close twice")
            void testCloseTwice() throws IOException {
                AtomicInteger counter = new AtomicInteger(0);
                Consumer<BodyCapturingInputStream> callback = input -> {
                    counter.incrementAndGet();
                    assertFalse(input.isConsumed());
                };

                try (InputStream input = createInputStream(callback)) {
                    input.close();
                }
                assertEquals(1, counter.get());
            }

            private InputStream createInputStream() {
                return createInputStream(input -> {
                    assertArrayEquals(Arrays.copyOfRange(BYTES, 0, BYTES.length - 5), input.captured());
                    assertEquals(TEXT.substring(0, BYTES.length - 5), input.captured(StandardCharsets.UTF_8));
                    assertEquals(BYTES.length - 5, input.totalBytes());
                    assertFalse(input.isConsumed());
                });
            }

            private InputStream createInputStream(Consumer<BodyCapturingInputStream> doneCallback) {
                AtomicInteger doneCount = new AtomicInteger(0);
                AtomicInteger limitReachedCount = new AtomicInteger(0);

                return new BodyCapturingInputStream(new ByteArrayInputStream(BYTES),
                        DEFAULT_INITIAL_CAPACITY, Integer.MAX_VALUE, BYTES.length - 5,
                        input -> {
                            assertEquals(0, doneCount.getAndIncrement());
                            assertEquals(0, limitReachedCount.get());
                            doneCallback.accept(input);
                        },
                        input -> {
                            assertEquals(0, doneCount.get());
                            assertEquals(0, limitReachedCount.getAndIncrement());
                        });
            }
        }
    }

    @Nested
    class CapturingReaderTest {

        @Nested
        class Unlimited {

            @Test
            @DisplayName("read()")
            void testReadChar() throws IOException {
                try (Reader reader = createReader()) {
                    StringBuilder sb = new StringBuilder(TEXT.length());

                    int c;
                    while ((c = reader.read()) != -1) {
                        sb.append((char) c);
                    }
                    assertEquals(TEXT, sb.toString());
                }
            }

            @Test
            @DisplayName("read(char[])")
            void testReadIntoCharArray() throws IOException {
                try (Reader reader = createReader()) {
                    StringBuilder sb = new StringBuilder(TEXT.length());

                    char[] buffer = new char[10];
                    int len;
                    while ((len = reader.read(buffer)) != -1) {
                        sb.append(buffer, 0, len);
                    }
                    assertEquals(TEXT, sb.toString());
                }
            }

            @Test
            @DisplayName("read(char[], int, int)")
            void testReadIntoCharArrayPortion() throws IOException {
                try (Reader reader = createReader()) {
                    StringBuilder sb = new StringBuilder(TEXT.length());

                    char[] buffer = new char[20];
                    int len;
                    while ((len = reader.read(buffer, 5, 10)) != -1) {
                        sb.append(buffer, 5, len);
                    }
                    assertEquals(TEXT, sb.toString());
                }
            }

            @Test
            @DisplayName("mark and reset")
            void testMarkAndReset() throws IOException {
                try (Reader reader = createReader()) {
                    assertTrue(reader.markSupported());

                    // mark, read 5, reset, read 10, repeat
                    final int readSize = 5;

                    StringBuilder expectedContent = new StringBuilder(TEXT.length() * 3 / 2);
                    for (int i = 0; i < TEXT.length(); i += readSize * 2) {
                        expectedContent.append(TEXT, i, Math.min(i + readSize, TEXT.length()));
                        expectedContent.append(TEXT, i, Math.min(i + readSize * 2, TEXT.length()));
                    }

                    StringBuilder sb = new StringBuilder(expectedContent.length());

                    char[] markedBuffer = new char[readSize];
                    char[] buffer = new char[readSize * 2];
                    int len;
                    reader.mark(readSize);
                    while ((len = reader.read(markedBuffer)) != -1) {
                        sb.append(markedBuffer, 0, len);
                        reader.reset();

                        len = reader.read(buffer);
                        if (len != -1) {
                            sb.append(buffer, 0, len);
                            reader.mark(readSize);
                        }
                    }
                    assertEquals(expectedContent.toString(), sb.toString());
                }
            }

            @Test
            @DisplayName("close twice")
            void testCloseTwice() throws IOException {
                AtomicInteger counter = new AtomicInteger(0);
                Consumer<BodyCapturingReader> callback = reader -> {
                    counter.incrementAndGet();
                    assertFalse(reader.isConsumed());
                };

                try (Reader reader = createReader(callback)) {
                    reader.close();
                }
                assertEquals(1, counter.get());
            }

            private Reader createReader() {
                return createReader(reader -> {
                    assertEquals(TEXT, reader.captured());
                    assertEquals(TEXT.length(), reader.totalChars());
                    assertTrue(reader.isConsumed());
                });
            }

            private Reader createReader(Consumer<BodyCapturingReader> doneCallback) {
                AtomicInteger doneCount = new AtomicInteger(0);
                AtomicInteger limitReachedCount = new AtomicInteger(0);

                return new BodyCapturingReader(new StringReader(TEXT),
                        DEFAULT_INITIAL_CAPACITY, Integer.MAX_VALUE, Long.MAX_VALUE,
                        reader -> {
                            assertEquals(0, doneCount.getAndIncrement());
                            assertEquals(0, limitReachedCount.get());
                            doneCallback.accept(reader);
                        },
                        reader -> {
                            assertEquals(0, doneCount.get());
                            assertEquals(0, limitReachedCount.getAndIncrement());
                        });
            }
        }

        @Nested
        class Limited {

            @Test
            @DisplayName("read()")
            void testReadChar() throws IOException {
                try (Reader reader = createReader()) {
                    StringBuilder sb = new StringBuilder(TEXT.length());

                    int c;
                    while ((c = reader.read()) != -1) {
                        sb.append((char) c);
                    }
                    assertEquals(-1, reader.read());
                    assertEquals(TEXT, sb.toString());
                }
            }

            @Test
            @DisplayName("read(char[])")
            void testReadIntoCharArray() throws IOException {
                try (Reader reader = createReader()) {
                    StringBuilder sb = new StringBuilder(TEXT.length());

                    char[] buffer = new char[10];
                    int len;
                    while ((len = reader.read(buffer)) != -1) {
                        sb.append(buffer, 0, len);
                    }
                    assertEquals(-1, reader.read(buffer));
                    assertEquals(TEXT, sb.toString());
                }
            }

            @Test
            @DisplayName("read(char[], int, int)")
            void testReadIntoCharArrayPortion() throws IOException {
                try (Reader reader = createReader()) {
                    StringBuilder sb = new StringBuilder(TEXT.length());

                    char[] buffer = new char[20];
                    int len;
                    while ((len = reader.read(buffer, 5, 10)) != -1) {
                        sb.append(buffer, 5, len);
                    }
                    assertEquals(-1, reader.read(buffer, 5, 10));
                    assertEquals(TEXT, sb.toString());
                }
            }

            @Test
            @DisplayName("mark and reset")
            void testMarkAndReset() throws IOException {
                try (Reader reader = createReader()) {
                    assertTrue(reader.markSupported());

                    // mark, read 5, reset, read 10, repeat
                    final int readSize = 5;

                    StringBuilder expectedContent = new StringBuilder(TEXT.length() * 3 / 2);
                    for (int i = 0; i < TEXT.length(); i += readSize * 2) {
                        expectedContent.append(TEXT, i, Math.min(i + readSize, TEXT.length()));
                        expectedContent.append(TEXT, i, Math.min(i + readSize * 2, TEXT.length()));
                    }

                    StringBuilder sb = new StringBuilder(expectedContent.length());

                    char[] markedBuffer = new char[readSize];
                    char[] buffer = new char[readSize * 2];
                    int len;
                    reader.mark(readSize);
                    while ((len = reader.read(markedBuffer)) != -1) {
                        sb.append(markedBuffer, 0, len);
                        reader.reset();

                        len = reader.read(buffer);
                        if (len != -1) {
                            sb.append(buffer, 0, len);
                            reader.mark(readSize);
                        }
                    }
                    assertEquals(expectedContent.toString(), sb.toString());
                }
            }

            @Test
            @DisplayName("close twice")
            void testCloseTwice() throws IOException {
                AtomicInteger counter = new AtomicInteger(0);
                Consumer<BodyCapturingReader> callback = reader -> {
                    counter.incrementAndGet();
                    assertFalse(reader.isConsumed());
                };

                try (Reader reader = createReader(13, callback)) {
                    reader.close();
                }
                assertEquals(1, counter.get());
            }

            private Reader createReader() {
                int limit = 13;
                AtomicInteger limitReachedCount = new AtomicInteger(0);

                Consumer<BodyCapturingReader> doneCallback = reader -> {
                    assertEquals(1, limitReachedCount.get());
                    assertEquals(TEXT.substring(0, limit), reader.captured());
                    assertEquals(TEXT.length(), reader.totalChars());
                };
                Consumer<BodyCapturingReader> limitReachedCallback = reader -> {
                    assertEquals(0, limitReachedCount.getAndIncrement());
                };
                return createReader(limit, doneCallback, limitReachedCallback);
            }

            private Reader createReader(int limit, Consumer<BodyCapturingReader> doneCallback) {
                Consumer<BodyCapturingReader> limitReachedCallback = reader -> { /* do nothing */ };
                return createReader(limit, doneCallback, limitReachedCallback);
            }

            private Reader createReader(int limit, Consumer<BodyCapturingReader> doneCallback, Consumer<BodyCapturingReader> limitReachedCallback) {
                AtomicInteger doneCount = new AtomicInteger(0);

                return new BodyCapturingReader(new StringReader(TEXT),
                        DEFAULT_INITIAL_CAPACITY, limit, Long.MAX_VALUE,
                        reader -> {
                            assertEquals(0, doneCount.getAndIncrement());
                            doneCallback.accept(reader);
                        },
                        reader -> {
                            assertEquals(0, doneCount.get());
                            limitReachedCallback.accept(reader);
                        });
            }
        }

        @Nested
        class DoneAfter {

            @Test
            @DisplayName("read()")
            void testReadChar() throws IOException {
                try (Reader reader = createReader()) {
                    StringBuilder sb = new StringBuilder(TEXT.length());

                    int c;
                    while ((c = reader.read()) != -1) {
                        sb.append((char) c);
                    }
                    assertEquals(TEXT, sb.toString());
                }
            }

            @Test
            @DisplayName("read(char[])")
            void testReadIntoCharArray() throws IOException {
                try (Reader reader = createReader()) {
                    StringBuilder sb = new StringBuilder(TEXT.length());

                    char[] buffer = new char[10];
                    int len;
                    while ((len = reader.read(buffer)) != -1) {
                        sb.append(buffer, 0, len);
                    }
                    assertEquals(TEXT, sb.toString());
                }
            }

            @Test
            @DisplayName("read(char[], int, int)")
            void testReadIntoCharArrayPortion() throws IOException {
                try (Reader reader = createReader()) {
                    StringBuilder sb = new StringBuilder(TEXT.length());

                    char[] buffer = new char[20];
                    int len;
                    while ((len = reader.read(buffer, 5, 10)) != -1) {
                        sb.append(buffer, 5, len);
                    }
                    assertEquals(TEXT, sb.toString());
                }
            }

            @Test
            @DisplayName("mark and reset")
            void testMarkAndReset() throws IOException {
                try (Reader reader = createReader()) {
                    assertTrue(reader.markSupported());

                    // mark, read 5, reset, read 10, repeat
                    final int readSize = 5;

                    StringBuilder expectedContent = new StringBuilder(TEXT.length() * 3 / 2);
                    for (int i = 0; i < TEXT.length(); i += readSize * 2) {
                        expectedContent.append(TEXT, i, Math.min(i + readSize, TEXT.length()));
                        expectedContent.append(TEXT, i, Math.min(i + readSize * 2, TEXT.length()));
                    }

                    StringBuilder sb = new StringBuilder(expectedContent.length());

                    char[] markedBuffer = new char[readSize];
                    char[] buffer = new char[readSize * 2];
                    int len;
                    reader.mark(readSize);
                    while ((len = reader.read(markedBuffer)) != -1) {
                        sb.append(markedBuffer, 0, len);
                        reader.reset();

                        len = reader.read(buffer);
                        if (len != -1) {
                            sb.append(buffer, 0, len);
                            reader.mark(readSize);
                        }
                    }
                    assertEquals(expectedContent.toString(), sb.toString());
                }
            }

            @Test
            @DisplayName("close twice")
            void testCloseTwice() throws IOException {
                AtomicInteger counter = new AtomicInteger(0);
                Consumer<BodyCapturingReader> callback = reader -> {
                    counter.incrementAndGet();
                    assertFalse(reader.isConsumed());
                };

                try (Reader reader = createReader(callback)) {
                    reader.close();
                }
                assertEquals(1, counter.get());
            }

            private Reader createReader() {
                return createReader(reader -> {
                    assertEquals(TEXT.substring(0, TEXT.length() - 5), reader.captured());
                    assertEquals(TEXT.length() - 5, reader.totalChars());
                    assertFalse(reader.isConsumed());
                });
            }

            private Reader createReader(Consumer<BodyCapturingReader> doneCallback) {
                AtomicInteger doneCount = new AtomicInteger(0);
                AtomicInteger limitReachedCount = new AtomicInteger(0);

                return new BodyCapturingReader(new StringReader(TEXT),
                        DEFAULT_INITIAL_CAPACITY, Integer.MAX_VALUE, TEXT.length() - 5,
                        reader -> {
                            assertEquals(0, doneCount.getAndIncrement());
                            assertEquals(0, limitReachedCount.get());
                            doneCallback.accept(reader);
                        },
                        reader -> {
                            assertEquals(0, doneCount.get());
                            assertEquals(0, limitReachedCount.getAndIncrement());
                        });
            }
        }
    }

    @Nested
    class CapturingOutputStreamTest {

        @Nested
        class Unlimited {

            @Test
            @DisplayName("write(int)")
            void testWriteByte() throws IOException {
                ByteArrayOutputStream baos = new ByteArrayOutputStream(BYTES.length);
                try (BodyCapturingOutputStream output = createOutputStream(baos)) {
                    for (byte b : BYTES) {
                        output.write(b);
                    }
                    output.flush();
                    assertArrayEquals(BYTES, baos.toByteArray());

                    assertCaptureDefaults(output);
                }
            }

            @Test
            @DisplayName("write(byte[])")
            void testWriteByteArray() throws IOException {
                ByteArrayOutputStream baos = new ByteArrayOutputStream(BYTES.length);
                try (BodyCapturingOutputStream output = createOutputStream(baos)) {
                    for (int i = 0; i < BYTES.length; i += 10) {
                        output.write(Arrays.copyOfRange(BYTES, i, Math.min(i + 10, BYTES.length)));
                    }
                    output.flush();
                    assertArrayEquals(BYTES, baos.toByteArray());

                    assertCaptureDefaults(output);
                }
            }

            @Test
            @DisplayName("write(byte[], int, int)")
            void testWriteByteArrayPortion() throws IOException {
                ByteArrayOutputStream baos = new ByteArrayOutputStream(BYTES.length);
                try (BodyCapturingOutputStream output = createOutputStream(baos)) {
                    for (int i = 0; i < BYTES.length; i += 10) {
                        int len = Math.min(10, BYTES.length - i);
                        output.write(BYTES, i, len);
                    }
                    output.flush();
                    assertArrayEquals(BYTES, baos.toByteArray());

                    assertCaptureDefaults(output);
                }
            }

            private BodyCapturingOutputStream createOutputStream(OutputStream delegate) {
                AtomicInteger limitReachedCount = new AtomicInteger(0);

                return new BodyCapturingOutputStream(delegate,
                        DEFAULT_INITIAL_CAPACITY, Integer.MAX_VALUE,
                        output -> assertEquals(0, limitReachedCount.getAndIncrement())
                        );
            }

            private void assertCaptureDefaults(BodyCapturingOutputStream output) {
                assertArrayEquals(BYTES, output.captured());
                assertEquals(TEXT, output.captured(StandardCharsets.UTF_8));
                assertEquals(BYTES.length, output.totalBytes());
            }
        }

        @Nested
        class Limited {

            @Test
            @DisplayName("write(int)")
            void testWriteByte() throws IOException {
                int limit = 13;
                AtomicInteger limitReachedCount = new AtomicInteger(0);

                ByteArrayOutputStream baos = new ByteArrayOutputStream(BYTES.length);
                try (BodyCapturingOutputStream output = createOutputStream(baos, limit, limitReachedCount)) {
                    for (byte b : BYTES) {
                        output.write(b);
                    }
                    output.flush();
                    assertArrayEquals(BYTES, baos.toByteArray());

                    assertCaptureDefaults(output, limit, limitReachedCount);
                }
            }

            @Test
            @DisplayName("write(byte[])")
            void testWriteByteArray() throws IOException {
                int limit = 13;
                AtomicInteger limitReachedCount = new AtomicInteger(0);

                ByteArrayOutputStream baos = new ByteArrayOutputStream(BYTES.length);
                try (BodyCapturingOutputStream output = createOutputStream(baos, limit, limitReachedCount)) {
                    for (int i = 0; i < BYTES.length; i += 10) {
                        output.write(Arrays.copyOfRange(BYTES, i, Math.min(i + 10, BYTES.length)));
                    }
                    output.flush();
                    assertArrayEquals(BYTES, baos.toByteArray());

                    assertCaptureDefaults(output, limit, limitReachedCount);
                }
            }

            @Test
            @DisplayName("write(byte[], int, int)")
            void testWriteByteArrayPortion() throws IOException {
                int limit = 13;
                AtomicInteger limitReachedCount = new AtomicInteger(0);

                ByteArrayOutputStream baos = new ByteArrayOutputStream(BYTES.length);
                try (BodyCapturingOutputStream output = createOutputStream(baos, limit, limitReachedCount)) {
                    for (int i = 0; i < BYTES.length; i += 10) {
                        int len = Math.min(10, BYTES.length - i);
                        output.write(BYTES, i, len);
                    }
                    output.flush();
                    assertArrayEquals(BYTES, baos.toByteArray());

                    assertCaptureDefaults(output, limit, limitReachedCount);
                }
            }

            private BodyCapturingOutputStream createOutputStream(OutputStream delegate, int limit, AtomicInteger limitReachedCount) {
                Consumer<BodyCapturingOutputStream> limitReachedCallback = output -> assertEquals(0, limitReachedCount.getAndIncrement());
                return new BodyCapturingOutputStream(delegate, DEFAULT_INITIAL_CAPACITY, limit, limitReachedCallback);
            }

            private void assertCaptureDefaults(BodyCapturingOutputStream output, int limit, AtomicInteger limitReachedCount) {
                assertEquals(1, limitReachedCount.get());
                assertArrayEquals(Arrays.copyOfRange(BYTES, 0, limit), output.captured());
                assertEquals(TEXT.substring(0, limit), output.captured(StandardCharsets.UTF_8));
                assertEquals(BYTES.length, output.totalBytes());
            }
        }
    }

    @Nested
    class CapturingWriterTest {

        @Nested
        class Unlimited {

            @Test
            @DisplayName("write(int)")
            void testWriteChar() throws IOException {
                StringWriter sw = new StringWriter(TEXT.length());
                try (BodyCapturingWriter writer = createWriter(sw)) {
                    for (char c : TEXT.toCharArray()) {
                        writer.write(c);
                    }
                    writer.flush();
                    assertEquals(TEXT, sw.toString());

                    assertCaptureDefaults(writer);
                }
            }

            @Test
            @DisplayName("write(char[])")
            void testWriteCharArray() throws IOException {
                StringWriter sw = new StringWriter(TEXT.length());
                try (BodyCapturingWriter writer = createWriter(sw)) {
                    for (int i = 0; i < TEXT.length(); i += 10) {
                        writer.write(TEXT.substring(i, Math.min(i + 10, TEXT.length())).toCharArray());
                    }
                    writer.flush();
                    assertEquals(TEXT, sw.toString());

                    assertCaptureDefaults(writer);
                }
            }

            @Test
            @DisplayName("write(char[], int, int)")
            void testWriteCharArrayPortion() throws IOException {
                StringWriter sw = new StringWriter(TEXT.length());
                try (BodyCapturingWriter writer = createWriter(sw)) {
                    for (int i = 0; i < TEXT.length(); i += 10) {
                        int len = Math.min(10, TEXT.length() - i);
                        writer.write(TEXT.toCharArray(), i, len);
                    }
                    writer.flush();
                    assertEquals(TEXT, sw.toString());

                    assertCaptureDefaults(writer);
                }
            }

            @Test
            @DisplayName("write(String)")
            void testWriteString() throws IOException {
                StringWriter sw = new StringWriter(TEXT.length());
                try (BodyCapturingWriter writer = createWriter(sw)) {
                    for (int i = 0; i < TEXT.length(); i += 10) {
                        writer.write(TEXT.substring(i, Math.min(i + 10, TEXT.length())));
                    }
                    writer.flush();
                    assertEquals(TEXT, sw.toString());

                    assertCaptureDefaults(writer);
                }
            }

            @Test
            @DisplayName("write(String, int, int)")
            void testWriteStringPortion() throws IOException {
                StringWriter sw = new StringWriter(TEXT.length());
                try (BodyCapturingWriter writer = createWriter(sw)) {
                    for (int i = 0; i < TEXT.length(); i += 10) {
                        int len = Math.min(10, TEXT.length() - i);
                        writer.write(TEXT, i, len);
                    }
                    writer.flush();
                    assertEquals(TEXT, sw.toString());

                    assertCaptureDefaults(writer);
                }
            }

            @Nested
            @DisplayName("append(CharSequence)")
            class AppendCharSequence {

                @Test
                @DisplayName("non-null")
                @SuppressWarnings("resource")
                void testAppendNonNullCharSequence() throws IOException {
                    StringWriter sw = new StringWriter(TEXT.length());
                    try (BodyCapturingWriter writer = createWriter(sw)) {
                        for (int i = 0; i < TEXT.length(); i += 10) {
                            writer.append(TEXT.substring(i, Math.min(i + 10, TEXT.length())));
                        }
                        writer.flush();
                        assertEquals(TEXT, sw.toString());

                        assertCaptureDefaults(writer);
                    }
                }

                @Test
                @DisplayName("null")
                @SuppressWarnings("resource")
                void testAppendNullCharSequence() throws IOException {
                    StringWriter sw = new StringWriter(TEXT.length());
                    try (BodyCapturingWriter writer = createWriter(sw)) {
                        writer.append(null);
                        writer.append(null);
                        writer.append(null);

                        writer.flush();
                        assertEquals("nullnullnull", sw.toString());

                        assertEquals("nullnullnull", writer.captured());
                        assertEquals(12, writer.totalChars());
                    }
                }
            }

            @Nested
            @DisplayName("append(CharSequence, int, int)")
            class AppendCharSequencePortion {

                @Test
                @DisplayName("non-null")
                @SuppressWarnings("resource")
                void testAppendNonNullCharSequencePortion() throws IOException {
                    StringWriter sw = new StringWriter(TEXT.length());
                    try (BodyCapturingWriter writer = createWriter(sw)) {
                        for (int i = 0; i < TEXT.length(); i += 10) {
                            int len = Math.min(10, TEXT.length() - i);
                            writer.append(TEXT, i, i + len);
                        }
                        writer.flush();
                        assertEquals(TEXT, sw.toString());

                        assertCaptureDefaults(writer);
                    }
                }

                @Test
                @DisplayName("null")
                @SuppressWarnings("resource")
                void testAppendNullCharSequence() throws IOException {
                    StringWriter sw = new StringWriter(TEXT.length());
                    try (BodyCapturingWriter writer = createWriter(sw)) {
                        writer.append(null, 1, 3);
                        writer.append(null, 1, 3);
                        writer.append(null, 1, 3);

                        writer.flush();
                        assertEquals("ululul", sw.toString());

                        assertEquals("ululul", writer.captured());
                        assertEquals(6, writer.totalChars());
                    }
                }
            }

            @Test
            @DisplayName("append(char)")
            @SuppressWarnings("resource")
            void testAppendChar() throws IOException {
                StringWriter sw = new StringWriter(TEXT.length());
                try (BodyCapturingWriter writer = createWriter(sw)) {
                    for (char c : TEXT.toCharArray()) {
                        writer.append(c);
                    }
                    writer.flush();
                    assertEquals(TEXT, sw.toString());

                    assertCaptureDefaults(writer);
                }
            }

            private BodyCapturingWriter createWriter(Writer delegate) {
                AtomicInteger limitReachedCount = new AtomicInteger(0);

                return new BodyCapturingWriter(delegate,
                        DEFAULT_INITIAL_CAPACITY, Integer.MAX_VALUE, writer -> assertEquals(0, limitReachedCount.getAndIncrement())
                        );
            }

            private void assertCaptureDefaults(BodyCapturingWriter writer) {
                assertEquals(TEXT, writer.captured());
                assertEquals(TEXT.length(), writer.totalChars());
            }
        }

        @Nested
        class Limited {

            @Test
            @DisplayName("write(int)")
            void testWriteChar() throws IOException {
                int limit = 13;
                AtomicInteger limitReachedCount = new AtomicInteger(0);

                StringWriter sw = new StringWriter(TEXT.length());
                try (BodyCapturingWriter writer = createWriter(sw, limit, limitReachedCount)) {
                    for (char c : TEXT.toCharArray()) {
                        writer.write(c);
                    }
                    writer.flush();
                    assertEquals(TEXT, sw.toString());
                }
            }

            @Test
            @DisplayName("write(char[])")
            void testWriteCharArray() throws IOException {
                int limit = 13;
                AtomicInteger limitReachedCount = new AtomicInteger(0);

                StringWriter sw = new StringWriter(TEXT.length());
                try (BodyCapturingWriter writer = createWriter(sw, limit, limitReachedCount)) {
                    for (int i = 0; i < TEXT.length(); i += 10) {
                        writer.write(TEXT.substring(i, Math.min(i + 10, TEXT.length())).toCharArray());
                    }
                    writer.flush();
                    assertEquals(TEXT, sw.toString());

                    assertCaptureDefaults(writer, limit, limitReachedCount);
                }
            }

            @Test
            @DisplayName("write(char[], int, int)")
            void testWriteCharArrayPortion() throws IOException {
                int limit = 13;
                AtomicInteger limitReachedCount = new AtomicInteger(0);

                StringWriter sw = new StringWriter(TEXT.length());
                try (BodyCapturingWriter writer = createWriter(sw, limit, limitReachedCount)) {
                    for (int i = 0; i < TEXT.length(); i += 10) {
                        int len = Math.min(10, TEXT.length() - i);
                        writer.write(TEXT.toCharArray(), i, len);
                    }
                    writer.flush();
                    assertEquals(TEXT, sw.toString());

                    assertCaptureDefaults(writer, limit, limitReachedCount);
                }
            }

            @Test
            @DisplayName("write(String)")
            void testWriteString() throws IOException {
                int limit = 13;
                AtomicInteger limitReachedCount = new AtomicInteger(0);

                StringWriter sw = new StringWriter(TEXT.length());
                try (BodyCapturingWriter writer = createWriter(sw, limit, limitReachedCount)) {
                    for (int i = 0; i < TEXT.length(); i += 10) {
                        writer.write(TEXT.substring(i, Math.min(i + 10, TEXT.length())));
                    }
                    writer.flush();
                    assertEquals(TEXT, sw.toString());

                    assertCaptureDefaults(writer, limit, limitReachedCount);
                }
            }

            @Test
            @DisplayName("write(String, int, int)")
            void testWriteStringPortion() throws IOException {
                int limit = 13;
                AtomicInteger limitReachedCount = new AtomicInteger(0);

                StringWriter sw = new StringWriter(TEXT.length());
                try (BodyCapturingWriter writer = createWriter(sw, limit, limitReachedCount)) {
                    for (int i = 0; i < TEXT.length(); i += 10) {
                        int len = Math.min(10, TEXT.length() - i);
                        writer.write(TEXT, i, len);
                    }
                    writer.flush();
                    assertEquals(TEXT, sw.toString());

                    assertCaptureDefaults(writer, limit, limitReachedCount);
                }
            }

            @Nested
            @DisplayName("append(CharSequence)")
            class AppendCharSequence {

                @Test
                @DisplayName("non-null")
                @SuppressWarnings("resource")
                void testAppendNonNullCharSequence() throws IOException {
                    int limit = 13;
                    AtomicInteger limitReachedCount = new AtomicInteger(0);

                    StringWriter sw = new StringWriter(TEXT.length());
                    try (BodyCapturingWriter writer = createWriter(sw, limit, limitReachedCount)) {
                        for (int i = 0; i < TEXT.length(); i += 10) {
                            writer.append(TEXT.substring(i, Math.min(i + 10, TEXT.length())));
                        }
                        writer.flush();
                        assertEquals(TEXT, sw.toString());

                        assertCaptureDefaults(writer, limit, limitReachedCount);
                    }
                }

                @Test
                @DisplayName("null")
                @SuppressWarnings("resource")
                void testAppendNullCharSequence() throws IOException {
                    int limit = 5;
                    AtomicInteger limitReachedCount = new AtomicInteger(0);

                    StringWriter sw = new StringWriter(TEXT.length());
                    try (BodyCapturingWriter writer = createWriter(sw, limit, limitReachedCount)) {
                        writer.append(null);
                        writer.append(null);
                        writer.append(null);

                        writer.flush();
                        assertEquals("nullnullnull", sw.toString());
                        assertEquals("nulln", writer.captured());
                        assertEquals(12, writer.totalChars());
                    }
                }
            }

            @Nested
            @DisplayName("append(CharSequence, int, int)")
            class AppendCharSequencePortion {

                @Test
                @DisplayName("non-null")
                @SuppressWarnings("resource")
                void testAppendNonNullCharSequencePortion() throws IOException {
                    int limit = 13;
                    AtomicInteger limitReachedCount = new AtomicInteger(0);

                    StringWriter sw = new StringWriter(TEXT.length());
                    try (BodyCapturingWriter writer = createWriter(sw, limit, limitReachedCount)) {
                        for (int i = 0; i < TEXT.length(); i += 10) {
                            int len = Math.min(10, TEXT.length() - i);
                            writer.append(TEXT, i, i + len);
                        }
                        writer.flush();
                        assertEquals(TEXT, sw.toString());

                        assertCaptureDefaults(writer, limit, limitReachedCount);
                    }
                }

                @Test
                @DisplayName("null")
                @SuppressWarnings("resource")
                void testAppendNullCharSequence() throws IOException {
                    int limit = 5;
                    AtomicInteger limitReachedCount = new AtomicInteger(0);

                    StringWriter sw = new StringWriter(TEXT.length());
                    try (BodyCapturingWriter writer = createWriter(sw, limit, limitReachedCount)) {
                        writer.append(null, 1, 3);
                        writer.append(null, 1, 3);
                        writer.append(null, 1, 3);

                        writer.flush();
                        assertEquals("ululul", sw.toString());

                        assertEquals("ululu", writer.captured());
                        assertEquals(6, writer.totalChars());
                    }
                }
            }

            @Test
            @DisplayName("append(char)")
            @SuppressWarnings("resource")
            void testAppendChar() throws IOException {
                int limit = 13;
                AtomicInteger limitReachedCount = new AtomicInteger(0);

                StringWriter sw = new StringWriter(TEXT.length());
                try (BodyCapturingWriter writer = createWriter(sw, limit, limitReachedCount)) {
                    for (char c : TEXT.toCharArray()) {
                        writer.append(c);
                    }
                    writer.flush();
                    assertEquals(TEXT, sw.toString());

                    assertCaptureDefaults(writer, limit, limitReachedCount);
                }
            }

            private BodyCapturingWriter createWriter(Writer delegate, int limit, AtomicInteger limitReachedCount) {
                Consumer<BodyCapturingWriter> limitReachedCallback = writer -> assertEquals(0, limitReachedCount.getAndIncrement());
                return createWriter(delegate, limit, limitReachedCallback);
            }

            private BodyCapturingWriter createWriter(Writer delegate, int limit, Consumer<BodyCapturingWriter> limitReachedCallback) {
                return new BodyCapturingWriter(delegate, DEFAULT_INITIAL_CAPACITY, limit, limitReachedCallback);
            }

            private void assertCaptureDefaults(BodyCapturingWriter writer, int limit, AtomicInteger limitReachedCount) {
                assertEquals(1, limitReachedCount.get());
                assertEquals(TEXT.substring(0, limit), writer.captured());
                assertEquals(TEXT.length(), writer.totalChars());
            }
        }
    }

    private static final class TestFilter extends BodyCapturingFilter {

        private final AtomicReference<CapturedData> capturedDataForRequestBodyRead = new AtomicReference<>();
        private final AtomicReference<CapturedData> capturedDataForRequestLimitReached = new AtomicReference<>();
        private final AtomicReference<CapturedData> capturedDataForResponseBodyProduced = new AtomicReference<>();
        private final AtomicReference<CapturedData> capturedDataForResponseLimitReached = new AtomicReference<>();

        @Override
        protected void bodyRead(BodyCapturingRequest request) {
            if (!capturedDataForRequestBodyRead.compareAndSet(null, new CapturedData(request))) {
                throw new IllegalStateException("bodyRead should only be called once per request");
            }
        }

        @Override
        protected void limitReached(BodyCapturingRequest request) {
            if (!capturedDataForRequestLimitReached.compareAndSet(null, new CapturedData(request))) {
                throw new IllegalStateException("limitReached should only be called once per request");
            }
        }

        @Override
        protected void bodyProduced(BodyCapturingResponse response, HttpServletRequest request) {
            if (!capturedDataForResponseBodyProduced.compareAndSet(null, new CapturedData(response))) {
                throw new IllegalStateException("bodyProduced should only be called once per response");
            }
        }

        @Override
        protected void limitReached(BodyCapturingResponse response, HttpServletRequest request) {
            if (!capturedDataForResponseLimitReached.compareAndSet(null, new CapturedData(response))) {
                throw new IllegalStateException("limitReached should only be called once per response");
            }
        }
    }

    private static final class IllegalStateTestingFilter extends BodyCapturingFilter {

        private final Consumer<BodyCapturingRequest> requestTester;
        private final Consumer<BodyCapturingResponse> responseTester;

        private IllegalStateTestingFilter(Consumer<BodyCapturingRequest> requestTester, Consumer<BodyCapturingResponse> responseTester) {
            this.requestTester = requestTester;
            this.responseTester = responseTester;
        }

        @Override
        protected void bodyRead(BodyCapturingRequest request) {
            requestTester.accept(request);
        }

        @Override
        protected void bodyProduced(BodyCapturingResponse response, HttpServletRequest request) {
            responseTester.accept(response);
        }
    }

    @SuppressWarnings("serial")
    private static final class BinaryEchoServlet extends HttpServlet {

        private final boolean useContentLength;

        private BinaryEchoServlet(boolean useContentLength) {
            this.useContentLength = useContentLength;
        }

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
            doPost(request, response);
        }

        @Override
        @SuppressWarnings("resource")
        protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
            String contentType = request.getContentType();
            if (contentType != null) {
                response.setContentType(contentType);
            }

            InputStream input = request.getInputStream();
            OutputStream output = response.getOutputStream();

            byte[] buffer = new byte[1024];
            int len;

            if (useContentLength) {
                long remaining = request.getContentLengthLong();
                while (remaining > 0) {
                    int chunkSize = (int) Math.min(remaining, buffer.length);
                    len = input.read(buffer, 0, chunkSize);
                    output.write(buffer, 0, len);
                    remaining -= len;
                }

            } else {
                while ((len = input.read(buffer)) != -1) {
                    output.write(buffer, 0, len);
                }
            }
        }
    }

    @SuppressWarnings("serial")
    private static final class TextEchoServlet extends HttpServlet {

        private final boolean useContentLength;

        private TextEchoServlet(boolean useContentLength) {
            this.useContentLength = useContentLength;
        }

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
            doPost(request, response);
        }

        @Override
        @SuppressWarnings("resource")
        protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
            String contentType = request.getContentType();
            if (contentType != null) {
                response.setContentType(contentType);
            }

            Reader input = request.getReader();
            Writer output = response.getWriter();

            char[] buffer = new char[1024];
            int len;

            if (useContentLength) {
                long remaining = request.getContentLengthLong();
                while (remaining > 0) {
                    int chunkSize = (int) Math.min(remaining, buffer.length);
                    len = input.read(buffer, 0, chunkSize);
                    output.write(buffer, 0, len);
                    remaining -= len;
                }

            } else {
                while ((len = input.read(buffer)) != -1) {
                    output.write(buffer, 0, len);
                }
            }
        }
    }

    @SuppressWarnings("serial")
    private static final class ConstantServlet extends HttpServlet {

        private static final String TEXT = "Hello World";

        private final boolean ensureBodyConsumed;
        private final boolean preferReader;

        private ConstantServlet(boolean ensureBodyConsumed, boolean preferReader) {
            this.ensureBodyConsumed = ensureBodyConsumed;
            this.preferReader = preferReader;
        }

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
            doPost(request, response);
        }

        @Override
        @SuppressWarnings("resource")
        protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
            if (ensureBodyConsumed) {
                BodyCapturingFilter.ensureBodyConsumed(request, preferReader);
            }
            response.setContentType("text/plain");
            response.getWriter().write(TEXT);
        }
    }

    @SuppressWarnings("serial")
    private static final class OnlyConsumingServlet extends HttpServlet {

        private final boolean reset;
        private final boolean resetBuffer;

        private OnlyConsumingServlet(boolean reset, boolean resetBuffer) {
            this.reset = reset;
            this.resetBuffer = resetBuffer;
        }

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
            doPost(request, response);
        }

        @Override
        @SuppressWarnings("resource")
        protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
            BodyCapturingFilter.ensureBodyConsumed(request);
            if (resetBuffer) {
                response.getWriter().write("Hello world");
                response.resetBuffer();
            }
            if (reset) {
                response.getWriter().write("Hello world");
                response.reset();
            }
        }
    }

    private static final class CapturedData {

        private final CaptureMode captureMode;
        private final byte[] bytes;
        private final String text;
        private final long totalSize;
        private final boolean isConsumed;

        private CapturedData(BodyCapturingRequest request) {
            captureMode = request.captureMode();
            bytes = captureMode == CaptureMode.BYTES ? request.capturedBinaryBody() : null;
            text = text(captureMode, request::capturedTextBody, request::capturedBinaryBodyAsString);
            totalSize = request.totalBodySize();
            isConsumed = request.bodyIsConsumed();
        }

        private CapturedData(BodyCapturingResponse response) {
            captureMode = response.captureMode();
            bytes = captureMode == CaptureMode.BYTES ? response.capturedBinaryBody() : null;
            text = text(captureMode, response::capturedTextBody, response::capturedBinaryBodyAsString);
            totalSize = response.totalBodySize();
            isConsumed = false;
        }

        private String text(CaptureMode captureMode, Supplier<String> ifText, Supplier<String> ifBytes) {
            switch (captureMode) {
            case TEXT:
                return ifText.get();
            case BYTES:
                return ifBytes.get();
            default:
                return null;
            }
        }

        private CapturedData(byte[] bytes, boolean isConsumed) {
            this.captureMode = CaptureMode.BYTES;
            this.bytes = bytes;
            this.text = new String(this.bytes, StandardCharsets.UTF_8);
            this.totalSize = bytes.length;
            this.isConsumed = isConsumed;
        }

        private CapturedData(byte[] bytes, int limit, boolean isConsumed) {
            this.captureMode = CaptureMode.BYTES;
            this.bytes = Arrays.copyOfRange(bytes, 0, limit);
            this.text = new String(this.bytes, StandardCharsets.UTF_8);
            this.totalSize = bytes.length;
            this.isConsumed = isConsumed;
        }

        private CapturedData(String text, boolean isConsumed) {
            this.captureMode = CaptureMode.TEXT;
            this.bytes = null;
            this.text = text;
            this.totalSize = text.length();
            this.isConsumed = isConsumed;
        }

        private CapturedData(String text, int limit, boolean isConsumed) {
            this.captureMode = CaptureMode.TEXT;
            this.bytes = null;
            this.text = text.substring(0, limit);
            this.totalSize = text.length();
            this.isConsumed = isConsumed;
        }

        private CapturedData(CaptureMode captureMode, byte[] bytes, String text, long totalSize, boolean isConsumed) {
            this.captureMode = captureMode;
            this.bytes = bytes;
            this.text = text;
            this.totalSize = totalSize;
            this.isConsumed = isConsumed;
        }

        private static CapturedData none() {
            return new CapturedData(CaptureMode.NONE, null, null, 0, false);
        }

        private static CapturedData emptyBytes(boolean isConsumed) {
            return new CapturedData(new byte[0], isConsumed);
        }

        private static CapturedData emptyText(boolean isConsumed) {
            return new CapturedData("", isConsumed);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || o.getClass() != getClass()) {
                return false;
            }
            BodyCapturingFilterTest.CapturedData other = (BodyCapturingFilterTest.CapturedData) o;
            return Objects.equals(captureMode, other.captureMode)
                    && Arrays.equals(bytes, other.bytes)
                    && Objects.equals(text, other.text)
                    && totalSize == other.totalSize
                    && isConsumed == other.isConsumed;
        }

        @Override
        public int hashCode() {
            return Objects.hash(captureMode, text, totalSize, isConsumed) ^ Arrays.hashCode(bytes);
        }

        @Override
        public String toString() {
            return "[captureMode=" + captureMode
                    + ",bytes=" + Arrays.toString(bytes)
                    + ",text=" + text
                    + ",totalSize=" + totalSize
                    + ",isConsumed=" + isConsumed
                    + "]";
        }
    }

    void dummy() {
        // dummy method to stop CheckStyle from thinking this is a utility class, just because it only has nested test classes
    }
}
