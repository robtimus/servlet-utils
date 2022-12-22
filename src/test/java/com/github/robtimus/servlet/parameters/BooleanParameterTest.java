/*
 * BooleanParameterTest.java
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
class BooleanParameterTest {

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
            when(config.getInitParameter(PARAM_NAME)).thenReturn("true");
            BooleanParameter parameter = BooleanParameter.of(config, PARAM_NAME);
            assertTrue(parameter.isSet());
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = { "true", "false" })
        @DisplayName("requiredValue()")
        void testRequiredValue(String value) {
            when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
            BooleanParameter parameter = BooleanParameter.of(config, PARAM_NAME);
            assertEquals(Boolean.parseBoolean(value), parameter.requiredValue());
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = { "true", "false" })
        @DisplayName("valueWithDefault(boolean)")
        void testValueWithDefault(String value) {
            when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
            BooleanParameter parameter = BooleanParameter.of(config, PARAM_NAME);
            assertEquals(Boolean.parseBoolean(value), parameter.valueWithDefault(true));
            assertEquals(Boolean.parseBoolean(value), parameter.valueWithDefault(false));
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = { "true", "false" })
        @DisplayName("toString()")
        void testToString(String value) {
            when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
            BooleanParameter parameter = BooleanParameter.of(config, PARAM_NAME);
            assertEquals(String.format("%s=%s", PARAM_NAME, value), parameter.toString());
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
            BooleanParameter parameter = BooleanParameter.of(config, PARAM_NAME);
            assertFalse(parameter.isSet());
        }

        @Test
        @DisplayName("requiredValue()")
        void testRequiredValue() {
            BooleanParameter parameter = BooleanParameter.of(config, PARAM_NAME);
            assertThrows(IllegalStateException.class, parameter::requiredValue);
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(booleans = { true, false })
        @DisplayName("valueWithDefault(boolean)")
        void testValueWithDefault(boolean defaultValue) {
            BooleanParameter parameter = BooleanParameter.of(config, PARAM_NAME);
            assertEquals(defaultValue, parameter.valueWithDefault(defaultValue));
        }

        @Test
        @DisplayName("toString()")
        void testToString() {
            BooleanParameter parameter = BooleanParameter.of(config, PARAM_NAME);
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
            BooleanParameter parameter = BooleanParameter.of(config, PARAM_NAME);
            assertFalse(parameter.isSet());
        }

        @Test
        @DisplayName("valid value")
        void testValidValue() {
            when(config.getInitParameter(PARAM_NAME)).thenReturn("true");
            BooleanParameter parameter = BooleanParameter.of(config, PARAM_NAME);
            assertTrue(parameter.isSet());
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = { "TRUE", "FALSE", "x" })
        @DisplayName("invalid value")
        void testInvalidValue(String value) {
            when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
            assertThrows(IllegalStateException.class, () -> BooleanParameter.of(config, PARAM_NAME));
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
            BooleanParameter parameter = BooleanParameter.of(config, PARAM_NAME);
            assertFalse(parameter.isSet());
        }

        @Test
        @DisplayName("valid value")
        void testValidValue() {
            when(config.getInitParameter(PARAM_NAME)).thenReturn("true");
            BooleanParameter parameter = BooleanParameter.of(config, PARAM_NAME);
            assertTrue(parameter.isSet());
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = { "TRUE", "FALSE", "x" })
        @DisplayName("invalid value")
        void testInvalidValue(String value) {
            when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
            assertThrows(IllegalStateException.class, () -> BooleanParameter.of(config, PARAM_NAME));
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
            BooleanParameter parameter = BooleanParameter.of(context, PARAM_NAME);
            assertFalse(parameter.isSet());
        }

        @Test
        @DisplayName("valid value")
        void testValidValue() {
            when(context.getInitParameter(PARAM_NAME)).thenReturn("true");
            BooleanParameter parameter = BooleanParameter.of(context, PARAM_NAME);
            assertTrue(parameter.isSet());
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = { "TRUE", "FALSE", "x" })
        @DisplayName("invalid value")
        void testInvalidValue(String value) {
            when(context.getInitParameter(PARAM_NAME)).thenReturn(value);
            assertThrows(IllegalStateException.class, () -> BooleanParameter.of(context, PARAM_NAME));
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
            BooleanParameter parameter = BooleanParameter.of(request, PARAM_NAME);
            assertFalse(parameter.isSet());
        }

        @Test
        @DisplayName("valid value")
        void testValidValue() {
            when(request.getParameter(PARAM_NAME)).thenReturn("true");
            BooleanParameter parameter = BooleanParameter.of(request, PARAM_NAME);
            assertTrue(parameter.isSet());
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = { "TRUE", "FALSE", "x" })
        @DisplayName("invalid value")
        void testInvalidValue(String value) {
            when(request.getParameter(PARAM_NAME)).thenReturn(value);
            assertThrows(IllegalStateException.class, () -> BooleanParameter.of(request, PARAM_NAME));
        }
    }
}
