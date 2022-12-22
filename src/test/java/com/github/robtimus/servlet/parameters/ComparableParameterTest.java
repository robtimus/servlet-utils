/*
 * ComparableParameterTest.java
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
import java.math.BigDecimal;
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
class ComparableParameterTest {

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
            ComparableParameter<BigDecimal> parameter = ComparableParameter.of(config, PARAM_NAME, BigDecimal::new);
            assertTrue(parameter.isSet());
        }

        @Test
        @DisplayName("requiredValue()")
        void testRequiredValue() {
            when(config.getInitParameter(PARAM_NAME)).thenReturn("1.0");
            ComparableParameter<BigDecimal> parameter = ComparableParameter.of(config, PARAM_NAME, BigDecimal::new);
            assertEquals(new BigDecimal("1.0"), parameter.requiredValue());
        }

        @Test
        @DisplayName("valueWithDefault(BigDecimal)")
        void testValueWithDefault() {
            when(config.getInitParameter(PARAM_NAME)).thenReturn("1.0");
            ComparableParameter<BigDecimal> parameter = ComparableParameter.of(config, PARAM_NAME, BigDecimal::new);
            assertEquals(new BigDecimal("1.0"), parameter.valueWithDefault(BigDecimal.ZERO));
        }

        @Nested
        @DisplayName("atLeast(BigDecimal)")
        class AtLeast {

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "1.0", "1.0001", "1000" })
            @DisplayName("matching value")
            void testMatchingValue(String value) {
                when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
                ComparableParameter<BigDecimal> parameter = ComparableParameter.of(config, PARAM_NAME, BigDecimal::new);
                assertDoesNotThrow(() -> parameter.atLeast(BigDecimal.ONE));
            }

            @Test
            @DisplayName("non-matching value")
            void testNonMatchingValue() {
                when(config.getInitParameter(PARAM_NAME)).thenReturn("0.9999");
                ComparableParameter<BigDecimal> parameter = ComparableParameter.of(config, PARAM_NAME, BigDecimal::new);
                assertThrows(IllegalStateException.class, () -> parameter.atLeast(BigDecimal.ONE));
            }

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "0.9999", "1.0", "1.0001", "1000" })
            @DisplayName("null minimum")
            void testNullMinimum(String value) {
                when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
                ComparableParameter<BigDecimal> parameter = ComparableParameter.of(config, PARAM_NAME, BigDecimal::new);
                assertThrows(NullPointerException.class, () -> parameter.atLeast(null));
            }
        }

        @Nested
        @DisplayName("atMost(BigDecimal)")
        class AtMost {

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "1.0", "0.9999", "-1000" })
            @DisplayName("matching value")
            void testMatchingValue(String value) {
                when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
                ComparableParameter<BigDecimal> parameter = ComparableParameter.of(config, PARAM_NAME, BigDecimal::new);
                assertDoesNotThrow(() -> parameter.atMost(BigDecimal.ONE));
            }

            @Test
            @DisplayName("non-matching value")
            void testNonMatchingValue() {
                when(config.getInitParameter(PARAM_NAME)).thenReturn("1.0001");
                ComparableParameter<BigDecimal> parameter = ComparableParameter.of(config, PARAM_NAME, BigDecimal::new);
                assertThrows(IllegalStateException.class, () -> parameter.atMost(BigDecimal.ONE));
            }

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "0.9999", "1.0", "1.0001", "-1000" })
            @DisplayName("null maximum")
            void testNullMaximum(String value) {
                when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
                ComparableParameter<BigDecimal> parameter = ComparableParameter.of(config, PARAM_NAME, BigDecimal::new);
                assertThrows(NullPointerException.class, () -> parameter.atMost(null));
            }
        }

        @Nested
        @DisplayName("greaterThan(BigDecimal)")
        class GreaterThan {

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "1.0001", "1000" })
            @DisplayName("matching value")
            void testMatchingValue(String value) {
                when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
                ComparableParameter<BigDecimal> parameter = ComparableParameter.of(config, PARAM_NAME, BigDecimal::new);
                assertDoesNotThrow(() -> parameter.greaterThan(BigDecimal.ONE));
            }

            @Test
            @DisplayName("non-matching value")
            void testNonMatchingValue() {
                when(config.getInitParameter(PARAM_NAME)).thenReturn("1.0");
                ComparableParameter<BigDecimal> parameter = ComparableParameter.of(config, PARAM_NAME, BigDecimal::new);
                assertThrows(IllegalStateException.class, () -> parameter.greaterThan(BigDecimal.ONE));
            }

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "0.9999", "1.0", "1.0001", "1000" })
            @DisplayName("null minimum")
            void testNullMinimum(String value) {
                when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
                ComparableParameter<BigDecimal> parameter = ComparableParameter.of(config, PARAM_NAME, BigDecimal::new);
                assertThrows(NullPointerException.class, () -> parameter.greaterThan(null));
            }
        }

        @Nested
        @DisplayName("smallerThan(BigDecimal)")
        class SmallerThan {

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "0.9999", "-1000" })
            @DisplayName("matching value")
            void testMatchingValue(String value) {
                when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
                ComparableParameter<BigDecimal> parameter = ComparableParameter.of(config, PARAM_NAME, BigDecimal::new);
                assertDoesNotThrow(() -> parameter.smallerThan(BigDecimal.ONE));
            }

            @Test
            @DisplayName("non-matching value")
            void testNonMatchingValue() {
                when(config.getInitParameter(PARAM_NAME)).thenReturn("1.0");
                ComparableParameter<BigDecimal> parameter = ComparableParameter.of(config, PARAM_NAME, BigDecimal::new);
                assertThrows(IllegalStateException.class, () -> parameter.smallerThan(BigDecimal.ONE));
            }

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "0.9999", "1.0", "1.0001", "-1000" })
            @DisplayName("null maximum")
            void testNullMaximum(String value) {
                when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
                ComparableParameter<BigDecimal> parameter = ComparableParameter.of(config, PARAM_NAME, BigDecimal::new);
                assertThrows(NullPointerException.class, () -> parameter.smallerThan(null));
            }
        }

        @Nested
        @DisplayName("between(BigDecimal, BigDecimal)")
        class Between {

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "1.0", "1.0001", "9.9999" })
            @DisplayName("matching value")
            void testMatchingValue(String value) {
                when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
                ComparableParameter<BigDecimal> parameter = ComparableParameter.of(config, PARAM_NAME, BigDecimal::new);
                assertDoesNotThrow(() -> parameter.between(BigDecimal.ONE, BigDecimal.TEN));
            }

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "0.9999", "10.0" })
            @DisplayName("non-matching value")
            void testNonMatchingValue(String value) {
                when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
                ComparableParameter<BigDecimal> parameter = ComparableParameter.of(config, PARAM_NAME, BigDecimal::new);
                assertThrows(IllegalStateException.class, () -> parameter.between(BigDecimal.ONE, BigDecimal.TEN));
            }

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "0.9999", "1.0", "1.0001", "9.9999", "10.0" })
            @DisplayName("null minimum")
            void testNullMinimum(String value) {
                when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
                ComparableParameter<BigDecimal> parameter = ComparableParameter.of(config, PARAM_NAME, BigDecimal::new);
                assertThrows(NullPointerException.class, () -> parameter.between(null, BigDecimal.TEN));
            }

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "0.9999", "1.0", "1.0001", "9.9999", "10.0" })
            @DisplayName("null maximum")
            void testNullMaximum(String value) {
                when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
                ComparableParameter<BigDecimal> parameter = ComparableParameter.of(config, PARAM_NAME, BigDecimal::new);
                assertThrows(NullPointerException.class, () -> parameter.between(BigDecimal.ONE, null));
            }
        }

        @Test
        @DisplayName("toString()")
        void testToString() {
            when(config.getInitParameter(PARAM_NAME)).thenReturn("1.0");
            ComparableParameter<BigDecimal> parameter = ComparableParameter.of(config, PARAM_NAME, BigDecimal::new);
            assertEquals(String.format("%s=%s", PARAM_NAME, new BigDecimal("1.0")), parameter.toString());
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
            ComparableParameter<BigDecimal> parameter = ComparableParameter.of(config, PARAM_NAME, BigDecimal::new);
            assertFalse(parameter.isSet());
        }

        @Test
        @DisplayName("requiredValue()")
        void testRequiredValue() {
            ComparableParameter<BigDecimal> parameter = ComparableParameter.of(config, PARAM_NAME, BigDecimal::new);
            assertThrows(IllegalStateException.class, parameter::requiredValue);
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = { "1.0", "100" })
        @DisplayName("valueWithDefault(BigDecimal)")
        void testValueWithDefault(BigDecimal defaultValue) {
            ComparableParameter<BigDecimal> parameter = ComparableParameter.of(config, PARAM_NAME, BigDecimal::new);
            assertEquals(defaultValue, parameter.valueWithDefault(defaultValue));
        }

        @Nested
        @DisplayName("atLeast(BigDecimal)")
        class AtLeast {

            @Test
            @DisplayName("non-null minimum")
            void testMatchingValue() {
                ComparableParameter<BigDecimal> parameter = ComparableParameter.of(config, PARAM_NAME, BigDecimal::new);
                assertDoesNotThrow(() -> parameter.atLeast(BigDecimal.ONE));
            }

            @Test
            @DisplayName("null minimum")
            void testNullMinimum() {
                ComparableParameter<BigDecimal> parameter = ComparableParameter.of(config, PARAM_NAME, BigDecimal::new);
                assertThrows(NullPointerException.class, () -> parameter.atLeast(null));
            }
        }

        @Nested
        @DisplayName("atMost(BigDecimal)")
        class AtMost {

            @Test
            @DisplayName("non-null maximum")
            void testMatchingValue() {
                ComparableParameter<BigDecimal> parameter = ComparableParameter.of(config, PARAM_NAME, BigDecimal::new);
                assertDoesNotThrow(() -> parameter.atMost(BigDecimal.ONE));
            }

            @Test
            @DisplayName("null maximum")
            void testNullMaximum() {
                ComparableParameter<BigDecimal> parameter = ComparableParameter.of(config, PARAM_NAME, BigDecimal::new);
                assertThrows(NullPointerException.class, () -> parameter.atMost(null));
            }
        }

        @Nested
        @DisplayName("greaterThan(BigDecimal)")
        class GreaterThan {

            @Test
            @DisplayName("non-null minimum")
            void testMatchingValue() {
                ComparableParameter<BigDecimal> parameter = ComparableParameter.of(config, PARAM_NAME, BigDecimal::new);
                assertDoesNotThrow(() -> parameter.greaterThan(BigDecimal.ONE));
            }

            @Test
            @DisplayName("null minimum")
            void testNullMinimum() {
                ComparableParameter<BigDecimal> parameter = ComparableParameter.of(config, PARAM_NAME, BigDecimal::new);
                assertThrows(NullPointerException.class, () -> parameter.greaterThan(null));
            }
        }

        @Nested
        @DisplayName("smallerThan(BigDecimal)")
        class SmallerThan {

            @Test
            @DisplayName("non-null maximum")
            void testMatchingValue() {
                ComparableParameter<BigDecimal> parameter = ComparableParameter.of(config, PARAM_NAME, BigDecimal::new);
                assertDoesNotThrow(() -> parameter.smallerThan(BigDecimal.ONE));
            }

            @Test
            @DisplayName("null maximum")
            void testNullMaximum() {
                ComparableParameter<BigDecimal> parameter = ComparableParameter.of(config, PARAM_NAME, BigDecimal::new);
                assertThrows(NullPointerException.class, () -> parameter.smallerThan(null));
            }
        }

        @Nested
        @DisplayName("between(BigDecimal, BigDecimal)")
        class Between {

            @Test
            @DisplayName("non-null minimum and maximum")
            void testMatchingValue() {
                ComparableParameter<BigDecimal> parameter = ComparableParameter.of(config, PARAM_NAME, BigDecimal::new);
                assertDoesNotThrow(() -> parameter.between(BigDecimal.ONE, BigDecimal.TEN));
            }

            @Test
            @DisplayName("null minimum")
            void testNullMinimum() {
                ComparableParameter<BigDecimal> parameter = ComparableParameter.of(config, PARAM_NAME, BigDecimal::new);
                assertThrows(NullPointerException.class, () -> parameter.between(null, BigDecimal.TEN));
            }

            @Test
            @DisplayName("null maximum")
            void testNullMaximum() {
                ComparableParameter<BigDecimal> parameter = ComparableParameter.of(config, PARAM_NAME, BigDecimal::new);
                assertThrows(NullPointerException.class, () -> parameter.between(BigDecimal.ONE, null));
            }
        }

        @Test
        @DisplayName("toString()")
        void testToString() {
            ComparableParameter<BigDecimal> parameter = ComparableParameter.of(config, PARAM_NAME, BigDecimal::new);
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
            ComparableParameter<BigDecimal> parameter = ComparableParameter.of(config, PARAM_NAME, BigDecimal::new);
            assertFalse(parameter.isSet());
        }

        @Test
        @DisplayName("valid value")
        void testValidValue() {
            when(config.getInitParameter(PARAM_NAME)).thenReturn("1.0");
            ComparableParameter<BigDecimal> parameter = ComparableParameter.of(config, PARAM_NAME, BigDecimal::new);
            assertTrue(parameter.isSet());
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = { "1s", "1E", "x" })
        @DisplayName("invalid value")
        void testInvalidValue(String value) {
            when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
            assertThrows(IllegalStateException.class, () -> ComparableParameter.of(config, PARAM_NAME, BigDecimal::new));
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
            ComparableParameter<BigDecimal> parameter = ComparableParameter.of(config, PARAM_NAME, BigDecimal::new);
            assertFalse(parameter.isSet());
        }

        @Test
        @DisplayName("valid value")
        void testValidValue() {
            when(config.getInitParameter(PARAM_NAME)).thenReturn("1.0");
            ComparableParameter<BigDecimal> parameter = ComparableParameter.of(config, PARAM_NAME, BigDecimal::new);
            assertTrue(parameter.isSet());
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = { "1s", "1E", "x" })
        @DisplayName("invalid value")
        void testInvalidValue(String value) {
            when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
            assertThrows(IllegalStateException.class, () -> ComparableParameter.of(config, PARAM_NAME, BigDecimal::new));
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
            ComparableParameter<BigDecimal> parameter = ComparableParameter.of(context, PARAM_NAME, BigDecimal::new);
            assertFalse(parameter.isSet());
        }

        @Test
        @DisplayName("valid value")
        void testValidValue() {
            when(context.getInitParameter(PARAM_NAME)).thenReturn("1.0");
            ComparableParameter<BigDecimal> parameter = ComparableParameter.of(context, PARAM_NAME, BigDecimal::new);
            assertTrue(parameter.isSet());
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = { "1s", "1E", "x" })
        @DisplayName("invalid value")
        void testInvalidValue(String value) {
            when(context.getInitParameter(PARAM_NAME)).thenReturn(value);
            assertThrows(IllegalStateException.class, () -> ComparableParameter.of(context, PARAM_NAME, BigDecimal::new));
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
            ComparableParameter<BigDecimal> parameter = ComparableParameter.of(request, PARAM_NAME, BigDecimal::new);
            assertFalse(parameter.isSet());
        }

        @Test
        @DisplayName("valid value")
        void testValidValue() {
            when(request.getParameter(PARAM_NAME)).thenReturn("1.0");
            ComparableParameter<BigDecimal> parameter = ComparableParameter.of(request, PARAM_NAME, BigDecimal::new);
            assertTrue(parameter.isSet());
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = { "1s", "1E", "x" })
        @DisplayName("invalid value")
        void testInvalidValue(String value) {
            when(request.getParameter(PARAM_NAME)).thenReturn(value);
            assertThrows(IllegalStateException.class, () -> ComparableParameter.of(request, PARAM_NAME, BigDecimal::new));
        }
    }
}
