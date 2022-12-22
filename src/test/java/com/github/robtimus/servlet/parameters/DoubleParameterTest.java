/*
 * DoubleParameterTest.java
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
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@SuppressWarnings("nls")
class DoubleParameterTest {

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
            when(config.getInitParameter(PARAM_NAME)).thenReturn("1.0");
            DoubleParameter parameter = DoubleParameter.of(config, PARAM_NAME);
            assertTrue(parameter.isSet());
        }

        @Test
        @DisplayName("requiredValue()")
        void testRequiredValue() {
            when(config.getInitParameter(PARAM_NAME)).thenReturn("1.0");
            DoubleParameter parameter = DoubleParameter.of(config, PARAM_NAME);
            assertEquals(1D, parameter.requiredValue());
        }

        @Test
        @DisplayName("valueWithDefault(double)")
        void testValueWithDefault() {
            when(config.getInitParameter(PARAM_NAME)).thenReturn("1.0");
            DoubleParameter parameter = DoubleParameter.of(config, PARAM_NAME);
            assertEquals(1D, parameter.valueWithDefault(0D));
            assertEquals(1D, parameter.valueWithDefault(Double.POSITIVE_INFINITY));
        }

        @Nested
        @DisplayName("atLeast(double)")
        class AtLeast {

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "1.0", "1.0001", "1000" })
            @DisplayName("matching value")
            void testMatchingValue(String value) {
                when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
                DoubleParameter parameter = DoubleParameter.of(config, PARAM_NAME);
                assertDoesNotThrow(() -> parameter.atLeast(1D));
            }

            @Test
            @DisplayName("non-matching value")
            void testNonMatchingValue() {
                when(config.getInitParameter(PARAM_NAME)).thenReturn("0.9999");
                DoubleParameter parameter = DoubleParameter.of(config, PARAM_NAME);
                assertThrows(IllegalStateException.class, () -> parameter.atLeast(1D));
            }
        }

        @Nested
        @DisplayName("atMost(double)")
        class AtMost {

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "1.0", "0.9999", "-1000" })
            @DisplayName("matching value")
            void testMatchingValue(String value) {
                when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
                DoubleParameter parameter = DoubleParameter.of(config, PARAM_NAME);
                assertDoesNotThrow(() -> parameter.atMost(1D));
            }

            @Test
            @DisplayName("non-matching value")
            void testNonMatchingValue() {
                when(config.getInitParameter(PARAM_NAME)).thenReturn("1.0001");
                DoubleParameter parameter = DoubleParameter.of(config, PARAM_NAME);
                assertThrows(IllegalStateException.class, () -> parameter.atMost(1D));
            }
        }

        @Nested
        @DisplayName("greaterThan(double)")
        class GreaterThan {

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "1.0001", "1000" })
            @DisplayName("matching value")
            void testMatchingValue(String value) {
                when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
                DoubleParameter parameter = DoubleParameter.of(config, PARAM_NAME);
                assertDoesNotThrow(() -> parameter.greaterThan(1D));
            }

            @Test
            @DisplayName("non-matching value")
            void testNonMatchingValue() {
                when(config.getInitParameter(PARAM_NAME)).thenReturn("1.0");
                DoubleParameter parameter = DoubleParameter.of(config, PARAM_NAME);
                assertThrows(IllegalStateException.class, () -> parameter.greaterThan(1D));
            }
        }

        @Nested
        @DisplayName("smallerThan(double)")
        class SmallerThan {

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "0.9999", "-1000" })
            @DisplayName("matching value")
            void testMatchingValue(String value) {
                when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
                DoubleParameter parameter = DoubleParameter.of(config, PARAM_NAME);
                assertDoesNotThrow(() -> parameter.smallerThan(1D));
            }

            @Test
            @DisplayName("non-matching value")
            void testNonMatchingValue() {
                when(config.getInitParameter(PARAM_NAME)).thenReturn("1.0");
                DoubleParameter parameter = DoubleParameter.of(config, PARAM_NAME);
                assertThrows(IllegalStateException.class, () -> parameter.smallerThan(1D));
            }
        }

        @Nested
        @DisplayName("between(double, double)")
        class Between {

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "1.0", "1.0001", "9.9999" })
            @DisplayName("matching value")
            void testMatchingValue(String value) {
                when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
                DoubleParameter parameter = DoubleParameter.of(config, PARAM_NAME);
                assertDoesNotThrow(() -> parameter.between(1D, 10D));
            }

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "0.9999", "10.0" })
            @DisplayName("non-matching value")
            void testNonMatchingValue(String value) {
                when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
                DoubleParameter parameter = DoubleParameter.of(config, PARAM_NAME);
                assertThrows(IllegalStateException.class, () -> parameter.between(1D, 10D));
            }
        }

        @Test
        @DisplayName("toString()")
        void testToString() {
            when(config.getInitParameter(PARAM_NAME)).thenReturn("1.0");
            DoubleParameter parameter = DoubleParameter.of(config, PARAM_NAME);
            assertEquals(String.format("%s=%s", PARAM_NAME, 1D), parameter.toString());
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
            DoubleParameter parameter = DoubleParameter.of(config, PARAM_NAME);
            assertFalse(parameter.isSet());
        }

        @Test
        @DisplayName("requiredValue()")
        void testRequiredValue() {
            DoubleParameter parameter = DoubleParameter.of(config, PARAM_NAME);
            assertThrows(IllegalStateException.class, parameter::requiredValue);
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(doubles = { 1D, Double.POSITIVE_INFINITY })
        @DisplayName("valueWithDefault(double)")
        void testValueWithDefault(double defaultValue) {
            DoubleParameter parameter = DoubleParameter.of(config, PARAM_NAME);
            assertEquals(defaultValue, parameter.valueWithDefault(defaultValue));
        }

        @Test
        @DisplayName("atLeast(double)")
        void testAtLeast() {
            DoubleParameter parameter = DoubleParameter.of(config, PARAM_NAME);
            assertDoesNotThrow(() -> parameter.atLeast(1D));
        }

        @Test
        @DisplayName("atMost(double)")
        void testAtMost() {
            DoubleParameter parameter = DoubleParameter.of(config, PARAM_NAME);
            assertDoesNotThrow(() -> parameter.atMost(1D));
        }

        @Test
        @DisplayName("greaterThan(double)")
        void testGreaterThan() {
            DoubleParameter parameter = DoubleParameter.of(config, PARAM_NAME);
            assertDoesNotThrow(() -> parameter.greaterThan(1D));
        }

        @Test
        @DisplayName("smallerThan(double)")
        void testSmallerThan() {
            DoubleParameter parameter = DoubleParameter.of(config, PARAM_NAME);
            assertDoesNotThrow(() -> parameter.smallerThan(1D));
        }

        @Test
        @DisplayName("between(double, double)")
        void testBetween() {
            DoubleParameter parameter = DoubleParameter.of(config, PARAM_NAME);
            assertDoesNotThrow(() -> parameter.between(1D, 0D));
        }

        @Test
        @DisplayName("toString()")
        void testToString() {
            DoubleParameter parameter = DoubleParameter.of(config, PARAM_NAME);
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
            DoubleParameter parameter = DoubleParameter.of(config, PARAM_NAME);
            assertFalse(parameter.isSet());
        }

        @Test
        @DisplayName("valid value")
        void testValidValue() {
            when(config.getInitParameter(PARAM_NAME)).thenReturn("1.0");
            DoubleParameter parameter = DoubleParameter.of(config, PARAM_NAME);
            assertTrue(parameter.isSet());
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = { "1s", "1E", "x" })
        @DisplayName("invalid value")
        void testInvalidValue(String value) {
            when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
            assertThrows(IllegalStateException.class, () -> DoubleParameter.of(config, PARAM_NAME));
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
            DoubleParameter parameter = DoubleParameter.of(config, PARAM_NAME);
            assertFalse(parameter.isSet());
        }

        @Test
        @DisplayName("valid value")
        void testValidValue() {
            when(config.getInitParameter(PARAM_NAME)).thenReturn("1.0");
            DoubleParameter parameter = DoubleParameter.of(config, PARAM_NAME);
            assertTrue(parameter.isSet());
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = { "1s", "1E", "x" })
        @DisplayName("invalid value")
        void testInvalidValue(String value) {
            when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
            assertThrows(IllegalStateException.class, () -> DoubleParameter.of(config, PARAM_NAME));
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
            DoubleParameter parameter = DoubleParameter.of(context, PARAM_NAME);
            assertFalse(parameter.isSet());
        }

        @Test
        @DisplayName("valid value")
        void testValidValue() {
            when(context.getInitParameter(PARAM_NAME)).thenReturn("1.0");
            DoubleParameter parameter = DoubleParameter.of(context, PARAM_NAME);
            assertTrue(parameter.isSet());
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = { "1s", "1E", "x" })
        @DisplayName("invalid value")
        void testInvalidValue(String value) {
            when(context.getInitParameter(PARAM_NAME)).thenReturn(value);
            assertThrows(IllegalStateException.class, () -> DoubleParameter.of(context, PARAM_NAME));
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
            DoubleParameter parameter = DoubleParameter.of(request, PARAM_NAME);
            assertFalse(parameter.isSet());
        }

        @Test
        @DisplayName("valid value")
        void testValidValue() {
            when(request.getParameter(PARAM_NAME)).thenReturn("1.0");
            DoubleParameter parameter = DoubleParameter.of(request, PARAM_NAME);
            assertTrue(parameter.isSet());
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = { "1s", "1E", "x" })
        @DisplayName("invalid value")
        void testInvalidValue(String value) {
            when(request.getParameter(PARAM_NAME)).thenReturn(value);
            assertThrows(IllegalStateException.class, () -> DoubleParameter.of(request, PARAM_NAME));
        }
    }
}
