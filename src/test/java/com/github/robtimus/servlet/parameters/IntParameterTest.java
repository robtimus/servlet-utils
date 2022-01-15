/*
 * IntParameterTest.java
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
class IntParameterTest {

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
            when(config.getInitParameter(PARAM_NAME)).thenReturn("1");
            IntParameter parameter = IntParameter.of(config, PARAM_NAME);
            assertTrue(parameter.isSet());
        }

        @Test
        @DisplayName("requiredValue()")
        void testRequiredValue() {
            when(config.getInitParameter(PARAM_NAME)).thenReturn("1");
            IntParameter parameter = IntParameter.of(config, PARAM_NAME);
            assertEquals(1, parameter.requiredValue());
        }

        @Test
        @DisplayName("valueWithDefault(int)")
        void testValueWithDefault() {
            when(config.getInitParameter(PARAM_NAME)).thenReturn("1");
            IntParameter parameter = IntParameter.of(config, PARAM_NAME);
            assertEquals(1, parameter.valueWithDefault(0));
            assertEquals(1, parameter.valueWithDefault(Integer.MAX_VALUE));
        }

        @Nested
        @DisplayName("atLeast(int)")
        class AtLeast {

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "1", "2", "1000" })
            @DisplayName("matching value")
            void testMatchingValue(String value) {
                when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
                IntParameter parameter = IntParameter.of(config, PARAM_NAME);
                assertDoesNotThrow(() -> parameter.atLeast(1));
            }

            @Test
            @DisplayName("non-matching value")
            void testNonMatchingValue() {
                when(config.getInitParameter(PARAM_NAME)).thenReturn("0");
                IntParameter parameter = IntParameter.of(config, PARAM_NAME);
                assertThrows(IllegalStateException.class, () -> parameter.atLeast(1));
            }
        }

        @Nested
        @DisplayName("atMost(int)")
        class AtMost {

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "1", "0", "-1000" })
            @DisplayName("matching value")
            void testMatchingValue(String value) {
                when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
                IntParameter parameter = IntParameter.of(config, PARAM_NAME);
                assertDoesNotThrow(() -> parameter.atMost(1));
            }

            @Test
            @DisplayName("non-matching value")
            void testNonMatchingValue() {
                when(config.getInitParameter(PARAM_NAME)).thenReturn("2");
                IntParameter parameter = IntParameter.of(config, PARAM_NAME);
                assertThrows(IllegalStateException.class, () -> parameter.atMost(1));
            }
        }

        @Nested
        @DisplayName("greaterThan(int)")
        class GreaterThan {

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "2", "1000" })
            @DisplayName("matching value")
            void testMatchingValue(String value) {
                when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
                IntParameter parameter = IntParameter.of(config, PARAM_NAME);
                assertDoesNotThrow(() -> parameter.greaterThan(1));
            }

            @Test
            @DisplayName("non-matching value")
            void testNonMatchingValue() {
                when(config.getInitParameter(PARAM_NAME)).thenReturn("1");
                IntParameter parameter = IntParameter.of(config, PARAM_NAME);
                assertThrows(IllegalStateException.class, () -> parameter.greaterThan(1));
            }
        }

        @Nested
        @DisplayName("smallerThan(int)")
        class SmallerThan {

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "0", "-1000" })
            @DisplayName("matching value")
            void testMatchingValue(String value) {
                when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
                IntParameter parameter = IntParameter.of(config, PARAM_NAME);
                assertDoesNotThrow(() -> parameter.smallerThan(1));
            }

            @Test
            @DisplayName("non-matching value")
            void testNonMatchingValue() {
                when(config.getInitParameter(PARAM_NAME)).thenReturn("1");
                IntParameter parameter = IntParameter.of(config, PARAM_NAME);
                assertThrows(IllegalStateException.class, () -> parameter.smallerThan(1));
            }
        }

        @Nested
        @DisplayName("between(int, int)")
        class Between {

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "1", "2", "9" })
            @DisplayName("matching value")
            void testMatchingValue(String value) {
                when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
                IntParameter parameter = IntParameter.of(config, PARAM_NAME);
                assertDoesNotThrow(() -> parameter.between(1, 10));
            }

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "0", "10" })
            @DisplayName("non-matching value")
            void testNonMatchingValue(String value) {
                when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
                IntParameter parameter = IntParameter.of(config, PARAM_NAME);
                assertThrows(IllegalStateException.class, () -> parameter.between(1, 10));
            }
        }

        @Test
        @DisplayName("toString()")
        void testToString() {
            when(config.getInitParameter(PARAM_NAME)).thenReturn("1");
            IntParameter parameter = IntParameter.of(config, PARAM_NAME);
            assertEquals(String.format("%s=%s", PARAM_NAME, 1), parameter.toString());
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
            IntParameter parameter = IntParameter.of(config, PARAM_NAME);
            assertFalse(parameter.isSet());
        }

        @Test
        @DisplayName("requiredValue()")
        void testRequiredValue() {
            IntParameter parameter = IntParameter.of(config, PARAM_NAME);
            assertThrows(IllegalStateException.class, parameter::requiredValue);
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(ints = { 1, Integer.MAX_VALUE })
        @DisplayName("valueWithDefault(int)")
        void testValueWithDefault(int defaultValue) {
            IntParameter parameter = IntParameter.of(config, PARAM_NAME);
            assertEquals(defaultValue, parameter.valueWithDefault(defaultValue));
        }

        @Test
        @DisplayName("atLeast(int)")
        void testAtLeast() {
            IntParameter parameter = IntParameter.of(config, PARAM_NAME);
            assertDoesNotThrow(() -> parameter.atLeast(1));
        }

        @Test
        @DisplayName("atMost(int)")
        void testAtMost() {
            IntParameter parameter = IntParameter.of(config, PARAM_NAME);
            assertDoesNotThrow(() -> parameter.atMost(1));
        }

        @Test
        @DisplayName("greaterThan(int)")
        void testGreaterThan() {
            IntParameter parameter = IntParameter.of(config, PARAM_NAME);
            assertDoesNotThrow(() -> parameter.greaterThan(1));
        }

        @Test
        @DisplayName("smallerThan(int)")
        void testSmallerThan() {
            IntParameter parameter = IntParameter.of(config, PARAM_NAME);
            assertDoesNotThrow(() -> parameter.smallerThan(1));
        }

        @Test
        @DisplayName("between(int, int)")
        void testBetween() {
            IntParameter parameter = IntParameter.of(config, PARAM_NAME);
            assertDoesNotThrow(() -> parameter.between(1, 0));
        }

        @Test
        @DisplayName("toString()")
        void testToString() {
            IntParameter parameter = IntParameter.of(config, PARAM_NAME);
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
            IntParameter parameter = IntParameter.of(config, PARAM_NAME);
            assertFalse(parameter.isSet());
        }

        @Test
        @DisplayName("valid value")
        void testValidValue() {
            when(config.getInitParameter(PARAM_NAME)).thenReturn("1");
            IntParameter parameter = IntParameter.of(config, PARAM_NAME);
            assertTrue(parameter.isSet());
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = { "1s", "1E", "x" })
        @DisplayName("invalid value")
        void testInvalidValue(String value) {
            when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
            assertThrows(IllegalStateException.class, () -> IntParameter.of(config, PARAM_NAME));
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
            IntParameter parameter = IntParameter.of(config, PARAM_NAME);
            assertFalse(parameter.isSet());
        }

        @Test
        @DisplayName("valid value")
        void testValidValue() {
            when(config.getInitParameter(PARAM_NAME)).thenReturn("1");
            IntParameter parameter = IntParameter.of(config, PARAM_NAME);
            assertTrue(parameter.isSet());
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = { "1s", "1E", "x" })
        @DisplayName("invalid value")
        void testInvalidValue(String value) {
            when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
            assertThrows(IllegalStateException.class, () -> IntParameter.of(config, PARAM_NAME));
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
            IntParameter parameter = IntParameter.of(context, PARAM_NAME);
            assertFalse(parameter.isSet());
        }

        @Test
        @DisplayName("valid value")
        void testValidValue() {
            when(context.getInitParameter(PARAM_NAME)).thenReturn("1");
            IntParameter parameter = IntParameter.of(context, PARAM_NAME);
            assertTrue(parameter.isSet());
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = { "1s", "1E", "x" })
        @DisplayName("invalid value")
        void testInvalidValue(String value) {
            when(context.getInitParameter(PARAM_NAME)).thenReturn(value);
            assertThrows(IllegalStateException.class, () -> IntParameter.of(context, PARAM_NAME));
        }
    }
}
