/*
 * URLParameterTest.java
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import javax.servlet.FilterConfig;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@SuppressWarnings("nls")
class URLParameterTest {

    private static final String PARAM_NAME = "param";

    @Nested
    @DisplayName("parameter set")
    class ParameterSet {

        private FilterConfig config;

        @BeforeEach
        void initConfig() {
            config = mock(FilterConfig.class);
        }

        @Test
        @DisplayName("isSet()")
        void testIsSet() {
            when(config.getInitParameter(PARAM_NAME)).thenReturn("https://example.org");
            URLParameter parameter = URLParameter.of(config, PARAM_NAME);
            assertTrue(parameter.isSet());
        }

        @Test
        @DisplayName("requiredValue()")
        void testRequiredValue() {
            when(config.getInitParameter(PARAM_NAME)).thenReturn("https://example.org");
            URLParameter parameter = URLParameter.of(config, PARAM_NAME);
            assertEquals(create("https://example.org"), parameter.requiredValue());
        }

        @Test
        @DisplayName("valueWithDefault(URL)")
        void testValueWithDefault() {
            when(config.getInitParameter(PARAM_NAME)).thenReturn("https://example.org");
            URLParameter parameter = URLParameter.of(config, PARAM_NAME);
            assertEquals(create("https://example.org"), parameter.valueWithDefault(create("https://example.org/default")));
        }

        @Nested
        @DisplayName("protocolIs(String)")
        class ProtocolIs {

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "https://example.org", "HTTPS://example.org" })
            @DisplayName("valid protocol")
            void testValidProtocol(String value) {
                when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
                URLParameter parameter = URLParameter.of(config, PARAM_NAME);
                assertDoesNotThrow(() -> parameter.protocolIs("https"));
            }

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "http://example.org", "mailto:test@example.org" })
            @DisplayName("invalid protocol")
            void testInvalidProtocol(String value) {
                when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
                URLParameter parameter = URLParameter.of(config, PARAM_NAME);
                assertThrows(IllegalStateException.class, () -> parameter.protocolIs("https"));
            }

            @Test
            @DisplayName("null protocol")
            void testNullProtocol() {
                when(config.getInitParameter(PARAM_NAME)).thenReturn("http://example.org");
                URLParameter parameter = URLParameter.of(config, PARAM_NAME);
                assertThrows(IllegalStateException.class, () -> parameter.protocolIs(null));
            }
        }

        @Nested
        @DisplayName("protocolIn(String...)")
        class ProtocolInArray {

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "http://example.org", "https://example.org", "HTTP://example.org", "HTTPS://example.org" })
            @DisplayName("valid protocol")
            void testValidProtocol(String value) {
                when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
                URLParameter parameter = URLParameter.of(config, PARAM_NAME);
                assertDoesNotThrow(() -> parameter.protocolIn("http", "https", null));
            }

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = "mailto:test@example.org")
            @DisplayName("invalid protocol")
            void testInvalidProtocol(String value) {
                when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
                URLParameter parameter = URLParameter.of(config, PARAM_NAME);
                assertThrows(IllegalStateException.class, () -> parameter.protocolIn("http", "https", null));
            }

            @Test
            @DisplayName("null protocols")
            void testNullProtocol() {
                when(config.getInitParameter(PARAM_NAME)).thenReturn("http://example.org");
                URLParameter parameter = URLParameter.of(config, PARAM_NAME);
                assertThrows(NullPointerException.class, () -> parameter.protocolIn((String[]) null));
            }
        }

        @Nested
        @DisplayName("protocolIn(Collection)")
        class ProtocolInCollection {

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "http://example.org", "https://example.org", "HTTP://example.org", "HTTPS://example.org" })
            @DisplayName("valid protocol")
            void testValidProtocol(String value) {
                when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
                URLParameter parameter = URLParameter.of(config, PARAM_NAME);
                Collection<String> protocols = Arrays.asList("http", "https", null);
                assertDoesNotThrow(() -> parameter.protocolIn(protocols));
            }

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = "mailto:test@example.org")
            @DisplayName("invalid protocol")
            void testInvalidProtocol(String value) {
                when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
                URLParameter parameter = URLParameter.of(config, PARAM_NAME);
                Collection<String> protocols = Arrays.asList("http", "https", null);
                assertThrows(IllegalStateException.class, () -> parameter.protocolIn(protocols));
            }

            @Test
            @DisplayName("null protocols")
            void testNullProtocol() {
                when(config.getInitParameter(PARAM_NAME)).thenReturn("http://example.org");
                URLParameter parameter = URLParameter.of(config, PARAM_NAME);
                assertThrows(NullPointerException.class, () -> parameter.protocolIn((Collection<String>) null));
            }
        }

        @Test
        @DisplayName("toString()")
        void testToString() {
            when(config.getInitParameter(PARAM_NAME)).thenReturn("https://example.org");
            URLParameter parameter = URLParameter.of(config, PARAM_NAME);
            assertEquals(String.format("%s=%s", PARAM_NAME, "https://example.org"), parameter.toString());
        }
    }

    @Nested
    @DisplayName("parameter not set")
    class ParameterNotSet {

        private FilterConfig config;

        @BeforeEach
        void initConfig() {
            config = mock(FilterConfig.class);
        }

        @Test
        @DisplayName("isSet()")
        void testIsSet() {
            URLParameter parameter = URLParameter.of(config, PARAM_NAME);
            assertFalse(parameter.isSet());
        }

        @Test
        @DisplayName("requiredValue()")
        void testRequiredValue() {
            URLParameter parameter = URLParameter.of(config, PARAM_NAME);
            assertThrows(IllegalStateException.class, parameter::requiredValue);
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = { "https://example.org/default1", "https://example.org/default2" })
        @DisplayName("valueWithDefault(URL)")
        void testValueWithDefault(URL defaultValue) {
            URLParameter parameter = URLParameter.of(config, PARAM_NAME);
            assertEquals(defaultValue, parameter.valueWithDefault(defaultValue));
        }

        @Nested
        @DisplayName("protocolIs(String)")
        class ProtocolIs {

            @Test
            @DisplayName("non-null protocol")
            void testNonNullProtocol() {
                URLParameter parameter = URLParameter.of(config, PARAM_NAME);
                assertDoesNotThrow(() -> parameter.protocolIs("https"));
            }

            @Test
            @DisplayName("null protocol")
            void testNullProtocol() {
                URLParameter parameter = URLParameter.of(config, PARAM_NAME);
                assertDoesNotThrow(() -> parameter.protocolIs(null));
            }
        }

        @Nested
        @DisplayName("protocolIn(String...)")
        class ProtocolInArray {

            @Test
            @DisplayName("non-null protocols")
            void testNonNullProtocols() {
                URLParameter parameter = URLParameter.of(config, PARAM_NAME);
                assertDoesNotThrow(() -> parameter.protocolIn("http", "https", null));
            }

            @Test
            @DisplayName("null protocols")
            void testNullProtocols() {
                URLParameter parameter = URLParameter.of(config, PARAM_NAME);
                assertThrows(NullPointerException.class, () -> parameter.protocolIn((String[]) null));
            }
        }

        @Nested
        @DisplayName("protocolIn(Collection)")
        class ProtocolInCollection {

            @Test
            @DisplayName("non-null protocols")
            void testValidProtocol() {
                URLParameter parameter = URLParameter.of(config, PARAM_NAME);
                Collection<String> protocols = Arrays.asList("http", "https", null);
                assertDoesNotThrow(() -> parameter.protocolIn(protocols));
            }

            @Test
            @DisplayName("null protocols")
            void testNullProtocol() {
                URLParameter parameter = URLParameter.of(config, PARAM_NAME);
                assertThrows(NullPointerException.class, () -> parameter.protocolIn((Collection<String>) null));
            }
        }

        @Test
        @DisplayName("toString()")
        void testToString() {
            URLParameter parameter = URLParameter.of(config, PARAM_NAME);
            assertEquals(String.format("%s (not set)", PARAM_NAME), parameter.toString());
        }
    }

    @Nested
    @DisplayName("of(FilterConfig, String)")
    class OfFilterConfig {

        private FilterConfig config;

        @BeforeEach
        void initConfig() {
            config = mock(FilterConfig.class);
        }

        @Test
        @DisplayName("parameter not set")
        void testNotSet() {
            URLParameter parameter = URLParameter.of(config, PARAM_NAME);
            assertFalse(parameter.isSet());
        }

        @Test
        @DisplayName("valid value")
        void testValidValue() {
            when(config.getInitParameter(PARAM_NAME)).thenReturn("https://example.org");
            URLParameter parameter = URLParameter.of(config, PARAM_NAME);
            assertTrue(parameter.isSet());
        }

        @Test
        @DisplayName("invalid value")
        void testInvalidValue() {
            when(config.getInitParameter(PARAM_NAME)).thenReturn("https://[1::/path");
            assertThrows(IllegalStateException.class, () -> URLParameter.of(config, PARAM_NAME));
        }
    }

    @Nested
    @DisplayName("of(ServletConfig, String)")
    class OfServletConfig {

        private ServletConfig config;

        @BeforeEach
        void initConfig() {
            config = mock(ServletConfig.class);
        }

        @Test
        @DisplayName("parameter not set")
        void testNotSet() {
            URLParameter parameter = URLParameter.of(config, PARAM_NAME);
            assertFalse(parameter.isSet());
        }

        @Test
        @DisplayName("valid value")
        void testValidValue() {
            when(config.getInitParameter(PARAM_NAME)).thenReturn("https://example.org");
            URLParameter parameter = URLParameter.of(config, PARAM_NAME);
            assertTrue(parameter.isSet());
        }

        @Test
        @DisplayName("invalid value")
        void testInvalidValue() {
            when(config.getInitParameter(PARAM_NAME)).thenReturn("https://[1::/path");
            assertThrows(IllegalStateException.class, () -> URLParameter.of(config, PARAM_NAME));
        }
    }

    @Nested
    @DisplayName("of(ServletContext, String)")
    class OfServletContext {

        private ServletContext context;

        @BeforeEach
        void initContext() {
            context = mock(ServletContext.class);
        }

        @Test
        @DisplayName("parameter not set")
        void testNotSet() {
            URLParameter parameter = URLParameter.of(context, PARAM_NAME);
            assertFalse(parameter.isSet());
        }

        @Test
        @DisplayName("valid value")
        void testValidValue() {
            when(context.getInitParameter(PARAM_NAME)).thenReturn("https://example.org");
            URLParameter parameter = URLParameter.of(context, PARAM_NAME);
            assertTrue(parameter.isSet());
        }

        @Test
        @DisplayName("invalid value")
        void testInvalidValue() {
            when(context.getInitParameter(PARAM_NAME)).thenReturn("https://[1::/path");
            assertThrows(IllegalStateException.class, () -> URLParameter.of(context, PARAM_NAME));
        }
    }

    @Nested
    @DisplayName("of(ServletRequest, String)")
    class OfServletRequest {

        private ServletRequest request;

        @BeforeEach
        void initRequest() {
            request = mock(ServletRequest.class);
        }

        @Test
        @DisplayName("parameter not set")
        void testNotSet() {
            URLParameter parameter = URLParameter.of(request, PARAM_NAME);
            assertFalse(parameter.isSet());
        }

        @Test
        @DisplayName("valid value")
        void testValidValue() {
            when(request.getParameter(PARAM_NAME)).thenReturn("https://example.org");
            URLParameter parameter = URLParameter.of(request, PARAM_NAME);
            assertTrue(parameter.isSet());
        }

        @Test
        @DisplayName("invalid value")
        void testInvalidValue() {
            when(request.getParameter(PARAM_NAME)).thenReturn("https://[1::/path");
            assertThrows(IllegalStateException.class, () -> URLParameter.of(request, PARAM_NAME));
        }
    }

    private static URL create(String spec) {
        return assertDoesNotThrow(() -> new URL(spec));
    }
}
