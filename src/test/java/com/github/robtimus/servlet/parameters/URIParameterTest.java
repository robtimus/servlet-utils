/*
 * URIParameterTest.java
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
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import javax.servlet.FilterConfig;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@SuppressWarnings("nls")
class URIParameterTest {

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
            URIParameter parameter = URIParameter.of(config, PARAM_NAME);
            assertTrue(parameter.isSet());
        }

        @Test
        @DisplayName("requiredValue()")
        void testRequiredValue() {
            when(config.getInitParameter(PARAM_NAME)).thenReturn("https://example.org");
            URIParameter parameter = URIParameter.of(config, PARAM_NAME);
            assertEquals(URI.create("https://example.org"), parameter.requiredValue());
        }

        @Test
        @DisplayName("valueWithDefault(URI)")
        void testValueWithDefault() {
            when(config.getInitParameter(PARAM_NAME)).thenReturn("https://example.org");
            URIParameter parameter = URIParameter.of(config, PARAM_NAME);
            assertEquals(URI.create("https://example.org"), parameter.valueWithDefault(URI.create("https://example.org/default")));
        }

        @Nested
        @DisplayName("schemeIs(String)")
        class SchemeIs {

            @Test
            @DisplayName("valid scheme")
            void testValidScheme() {
                when(config.getInitParameter(PARAM_NAME)).thenReturn("https://example.org");
                URIParameter parameter = URIParameter.of(config, PARAM_NAME);
                assertDoesNotThrow(() -> parameter.schemeIs("https"));
            }

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "http://example.org", "HTTPS://example.org", "mailto:test@example.org" })
            @DisplayName("invalid scheme")
            void testInvalidScheme(String value) {
                when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
                URIParameter parameter = URIParameter.of(config, PARAM_NAME);
                assertThrows(IllegalStateException.class, () -> parameter.schemeIs("https"));
            }

            @Test
            @DisplayName("null scheme")
            void testNullScheme() {
                when(config.getInitParameter(PARAM_NAME)).thenReturn("http://example.org");
                URIParameter parameter = URIParameter.of(config, PARAM_NAME);
                assertThrows(IllegalStateException.class, () -> parameter.schemeIs(null));
            }
        }

        @Nested
        @DisplayName("schemeIn(String...)")
        class SchemeInArray {

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "http://example.org", "https://example.org" })
            @DisplayName("valid scheme")
            void testValidScheme(String value) {
                when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
                URIParameter parameter = URIParameter.of(config, PARAM_NAME);
                assertDoesNotThrow(() -> parameter.schemeIn("http", "https", null));
            }

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "HTTP://example.org", "HTTPS://example.org", "mailto:test@example.org" })
            @DisplayName("invalid scheme")
            void testInvalidScheme(String value) {
                when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
                URIParameter parameter = URIParameter.of(config, PARAM_NAME);
                assertThrows(IllegalStateException.class, () -> parameter.schemeIn("http", "https", null));
            }

            @Test
            @DisplayName("null schemes")
            void testNullScheme() {
                when(config.getInitParameter(PARAM_NAME)).thenReturn("http://example.org");
                URIParameter parameter = URIParameter.of(config, PARAM_NAME);
                assertThrows(NullPointerException.class, () -> parameter.schemeIn((String[]) null));
            }
        }

        @Nested
        @DisplayName("schemeIn(Collection)")
        class SchemeInCollection {

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "http://example.org", "https://example.org" })
            @DisplayName("valid scheme")
            void testValidScheme(String value) {
                when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
                URIParameter parameter = URIParameter.of(config, PARAM_NAME);
                Collection<String> schemes = Arrays.asList("http", "https", null);
                assertDoesNotThrow(() -> parameter.schemeIn(schemes));
            }

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "HTTP://example.org", "HTTPS://example.org", "mailto:test@example.org" })
            @DisplayName("invalid scheme")
            void testInvalidScheme(String value) {
                when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
                URIParameter parameter = URIParameter.of(config, PARAM_NAME);
                Collection<String> schemes = Arrays.asList("http", "https", null);
                assertThrows(IllegalStateException.class, () -> parameter.schemeIn(schemes));
            }

            @Test
            @DisplayName("null schemes")
            void testNullScheme() {
                when(config.getInitParameter(PARAM_NAME)).thenReturn("http://example.org");
                URIParameter parameter = URIParameter.of(config, PARAM_NAME);
                assertThrows(NullPointerException.class, () -> parameter.schemeIn((Collection<String>) null));
            }
        }

        @Test
        @DisplayName("toString()")
        void testToString() {
            when(config.getInitParameter(PARAM_NAME)).thenReturn("https://example.org");
            URIParameter parameter = URIParameter.of(config, PARAM_NAME);
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
            URIParameter parameter = URIParameter.of(config, PARAM_NAME);
            assertFalse(parameter.isSet());
        }

        @Test
        @DisplayName("requiredValue()")
        void testRequiredValue() {
            URIParameter parameter = URIParameter.of(config, PARAM_NAME);
            assertThrows(IllegalStateException.class, parameter::requiredValue);
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = { "https://example.org/default1", "https://example.org/default2" })
        @DisplayName("valueWithDefault(URI)")
        void testValueWithDefault(URI defaultValue) {
            URIParameter parameter = URIParameter.of(config, PARAM_NAME);
            assertEquals(defaultValue, parameter.valueWithDefault(defaultValue));
        }

        @Nested
        @DisplayName("schemeIs(String)")
        class SchemeIs {

            @Test
            @DisplayName("non-null scheme")
            void testNonNullScheme() {
                URIParameter parameter = URIParameter.of(config, PARAM_NAME);
                assertDoesNotThrow(() -> parameter.schemeIs("https"));
            }

            @Test
            @DisplayName("null scheme")
            void testNullScheme() {
                URIParameter parameter = URIParameter.of(config, PARAM_NAME);
                assertDoesNotThrow(() -> parameter.schemeIs(null));
            }
        }

        @Nested
        @DisplayName("schemeIn(String...)")
        class SchemeInArray {

            @Test
            @DisplayName("non-null schemes")
            void testNonNullSchemes() {
                URIParameter parameter = URIParameter.of(config, PARAM_NAME);
                assertDoesNotThrow(() -> parameter.schemeIn("http", "https", null));
            }

            @Test
            @DisplayName("null schemes")
            void testNullSchemes() {
                URIParameter parameter = URIParameter.of(config, PARAM_NAME);
                assertThrows(NullPointerException.class, () -> parameter.schemeIn((String[]) null));
            }
        }

        @Nested
        @DisplayName("schemeIn(Collection)")
        class SchemeInCollection {

            @Test
            @DisplayName("non-null schemes")
            void testValidScheme() {
                URIParameter parameter = URIParameter.of(config, PARAM_NAME);
                Collection<String> schemes = Arrays.asList("http", "https", null);
                assertDoesNotThrow(() -> parameter.schemeIn(schemes));
            }

            @Test
            @DisplayName("null schemes")
            void testNullScheme() {
                URIParameter parameter = URIParameter.of(config, PARAM_NAME);
                assertThrows(NullPointerException.class, () -> parameter.schemeIn((Collection<String>) null));
            }
        }

        @Test
        @DisplayName("toString()")
        void testToString() {
            URIParameter parameter = URIParameter.of(config, PARAM_NAME);
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
            URIParameter parameter = URIParameter.of(config, PARAM_NAME);
            assertFalse(parameter.isSet());
        }

        @Test
        @DisplayName("valid value")
        void testValidValue() {
            when(config.getInitParameter(PARAM_NAME)).thenReturn("https://example.org");
            URIParameter parameter = URIParameter.of(config, PARAM_NAME);
            assertTrue(parameter.isSet());
        }

        @Test
        @DisplayName("invalid value")
        void testInvalidValue() {
            when(config.getInitParameter(PARAM_NAME)).thenReturn("https://[1::/path");
            assertThrows(IllegalStateException.class, () -> URIParameter.of(config, PARAM_NAME));
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
            URIParameter parameter = URIParameter.of(config, PARAM_NAME);
            assertFalse(parameter.isSet());
        }

        @Test
        @DisplayName("valid value")
        void testValidValue() {
            when(config.getInitParameter(PARAM_NAME)).thenReturn("https://example.org");
            URIParameter parameter = URIParameter.of(config, PARAM_NAME);
            assertTrue(parameter.isSet());
        }

        @Test
        @DisplayName("invalid value")
        void testInvalidValue() {
            when(config.getInitParameter(PARAM_NAME)).thenReturn("https://[1::/path");
            assertThrows(IllegalStateException.class, () -> URIParameter.of(config, PARAM_NAME));
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
            URIParameter parameter = URIParameter.of(context, PARAM_NAME);
            assertFalse(parameter.isSet());
        }

        @Test
        @DisplayName("valid value")
        void testValidValue() {
            when(context.getInitParameter(PARAM_NAME)).thenReturn("https://example.org");
            URIParameter parameter = URIParameter.of(context, PARAM_NAME);
            assertTrue(parameter.isSet());
        }

        @Test
        @DisplayName("invalid value")
        void testInvalidValue() {
            when(context.getInitParameter(PARAM_NAME)).thenReturn("https://[1::/path");
            assertThrows(IllegalStateException.class, () -> URIParameter.of(context, PARAM_NAME));
        }
    }
}
