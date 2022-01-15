/*
 * StringParameterTest.java
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import java.util.function.Function;
import java.util.regex.Pattern;
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
class StringParameterTest {

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
            when(config.getInitParameter(PARAM_NAME)).thenReturn("foo");
            StringParameter parameter = StringParameter.of(config, PARAM_NAME);
            assertTrue(parameter.isSet());
        }

        @Test
        @DisplayName("requiredValue()")
        void testRequiredValue() {
            when(config.getInitParameter(PARAM_NAME)).thenReturn("foo");
            StringParameter parameter = StringParameter.of(config, PARAM_NAME);
            assertEquals("foo", parameter.requiredValue());
        }

        @Test
        @DisplayName("valueWithDefault(String)")
        void testValueWithDefault() {
            when(config.getInitParameter(PARAM_NAME)).thenReturn("foo");
            StringParameter parameter = StringParameter.of(config, PARAM_NAME);
            assertEquals("foo", parameter.valueWithDefault("bar"));
        }

        @Nested
        @DisplayName("atLeast(String)")
        class AtLeast {

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "foo", "hello", "world" })
            @DisplayName("matching value")
            void testMatchingValue(String value) {
                when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
                StringParameter parameter = StringParameter.of(config, PARAM_NAME);
                assertDoesNotThrow(() -> parameter.atLeast("foo"));
            }

            @Test
            @DisplayName("non-matching value")
            void testNonMatchingValue() {
                when(config.getInitParameter(PARAM_NAME)).thenReturn("bar");
                StringParameter parameter = StringParameter.of(config, PARAM_NAME);
                assertThrows(IllegalStateException.class, () -> parameter.atLeast("foo"));
            }

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "foo", "bar", "hello", "world" })
            @DisplayName("null minimum")
            void testNullMinimum(String value) {
                when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
                StringParameter parameter = StringParameter.of(config, PARAM_NAME);
                assertThrows(NullPointerException.class, () -> parameter.atLeast(null));
            }
        }

        @Nested
        @DisplayName("atMost(String)")
        class AtMost {

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "foo", "bar", "hello" })
            @DisplayName("matching value")
            void testMatchingValue(String value) {
                when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
                StringParameter parameter = StringParameter.of(config, PARAM_NAME);
                assertDoesNotThrow(() -> parameter.atMost("hello"));
            }

            @Test
            @DisplayName("non-matching value")
            void testNonMatchingValue() {
                when(config.getInitParameter(PARAM_NAME)).thenReturn("world");
                StringParameter parameter = StringParameter.of(config, PARAM_NAME);
                assertThrows(IllegalStateException.class, () -> parameter.atMost("hello"));
            }

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "foo", "bar", "hello", "world" })
            @DisplayName("null maximum")
            void testNullMaximum(String value) {
                when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
                StringParameter parameter = StringParameter.of(config, PARAM_NAME);
                assertThrows(NullPointerException.class, () -> parameter.atMost(null));
            }
        }

        @Nested
        @DisplayName("greaterThan(String)")
        class GreaterThan {

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "hello", "world" })
            @DisplayName("matching value")
            void testMatchingValue(String value) {
                when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
                StringParameter parameter = StringParameter.of(config, PARAM_NAME);
                assertDoesNotThrow(() -> parameter.greaterThan("foo"));
            }

            @Test
            @DisplayName("non-matching value")
            void testNonMatchingValue() {
                when(config.getInitParameter(PARAM_NAME)).thenReturn("foo");
                StringParameter parameter = StringParameter.of(config, PARAM_NAME);
                assertThrows(IllegalStateException.class, () -> parameter.greaterThan("foo"));
            }

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "foo", "bar", "hello", "world" })
            @DisplayName("null minimum")
            void testNullMinimum(String value) {
                when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
                StringParameter parameter = StringParameter.of(config, PARAM_NAME);
                assertThrows(NullPointerException.class, () -> parameter.greaterThan(null));
            }
        }

        @Nested
        @DisplayName("smallerThan(String)")
        class SmallerThan {

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "foo", "bar" })
            @DisplayName("matching value")
            void testMatchingValue(String value) {
                when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
                StringParameter parameter = StringParameter.of(config, PARAM_NAME);
                assertDoesNotThrow(() -> parameter.smallerThan("hello"));
            }

            @Test
            @DisplayName("non-matching value")
            void testNonMatchingValue() {
                when(config.getInitParameter(PARAM_NAME)).thenReturn("hello");
                StringParameter parameter = StringParameter.of(config, PARAM_NAME);
                assertThrows(IllegalStateException.class, () -> parameter.smallerThan("hello"));
            }

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "foo", "bar", "hello", "world" })
            @DisplayName("null maximum")
            void testNullMaximum(String value) {
                when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
                StringParameter parameter = StringParameter.of(config, PARAM_NAME);
                assertThrows(NullPointerException.class, () -> parameter.smallerThan(null));
            }
        }

        @Nested
        @DisplayName("between(String, String)")
        class Between {

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "foo", "hello" })
            @DisplayName("matching value")
            void testMatchingValue(String value) {
                when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
                StringParameter parameter = StringParameter.of(config, PARAM_NAME);
                assertDoesNotThrow(() -> parameter.between("foo", "world"));
            }

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "bar", "world" })
            @DisplayName("non-matching value")
            void testNonMatchingValue(String value) {
                when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
                StringParameter parameter = StringParameter.of(config, PARAM_NAME);
                assertThrows(IllegalStateException.class, () -> parameter.between("foo", "world"));
            }

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "foo", "bar", "hello", "world" })
            @DisplayName("null minimum")
            void testNullMinimum(String value) {
                when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
                StringParameter parameter = StringParameter.of(config, PARAM_NAME);
                assertThrows(NullPointerException.class, () -> parameter.between(null, "world"));
            }

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "foo", "bar", "hello", "world" })
            @DisplayName("null maximum")
            void testNullMaximum(String value) {
                when(config.getInitParameter(PARAM_NAME)).thenReturn(value);
                StringParameter parameter = StringParameter.of(config, PARAM_NAME);
                assertThrows(NullPointerException.class, () -> parameter.between("foo", null));
            }
        }

        @Nested
        @DisplayName("notEmpty")
        class NotEmpty {

            @Test
            @DisplayName("matching value")
            void testMatchingValue() {
                when(config.getInitParameter(PARAM_NAME)).thenReturn(" ");
                StringParameter parameter = StringParameter.of(config, PARAM_NAME);
                assertDoesNotThrow(parameter::notEmpty);
            }

            @Test
            @DisplayName("non-matching value")
            void testNonMatchingValue() {
                when(config.getInitParameter(PARAM_NAME)).thenReturn("");
                StringParameter parameter = StringParameter.of(config, PARAM_NAME);
                assertThrows(IllegalStateException.class, parameter::notEmpty);
            }
        }

        @Nested
        @DisplayName("notBlank")
        class NotBlank {

            @Test
            @DisplayName("matching value")
            void testMatchingValue() {
                when(config.getInitParameter(PARAM_NAME)).thenReturn("    .    ");
                StringParameter parameter = StringParameter.of(config, PARAM_NAME);
                assertDoesNotThrow(parameter::notBlank);
            }

            @Test
            @DisplayName("non-matching value")
            void testNonMatchingValue() {
                when(config.getInitParameter(PARAM_NAME)).thenReturn("   ");
                StringParameter parameter = StringParameter.of(config, PARAM_NAME);
                assertThrows(IllegalStateException.class, parameter::notBlank);
            }
        }

        @Nested
        @DisplayName("matching")
        class Matching {

            @Test
            @DisplayName("matching value")
            void testMatchingValue() {
                when(config.getInitParameter(PARAM_NAME)).thenReturn("123456");
                StringParameter parameter = StringParameter.of(config, PARAM_NAME);
                Pattern pattern = Pattern.compile("\\d+");
                assertDoesNotThrow(() -> parameter.matching(pattern));
            }

            @Test
            @DisplayName("non-matching value")
            void testNonMatchingValue() {
                when(config.getInitParameter(PARAM_NAME)).thenReturn("123456s");
                StringParameter parameter = StringParameter.of(config, PARAM_NAME);
                Pattern pattern = Pattern.compile("\\d+");
                assertThrows(IllegalStateException.class, () -> parameter.matching(pattern));
            }

            @Test
            @DisplayName("null pattern")
            void testNullPattern() {
                when(config.getInitParameter(PARAM_NAME)).thenReturn("123456s");
                StringParameter parameter = StringParameter.of(config, PARAM_NAME);
                assertThrows(NullPointerException.class, () -> parameter.matching(null));
            }
        }

        @Nested
        @DisplayName("transform(Function)")
        class Transform {

            @Test
            @DisplayName("non-null function")
            void testNonNullFunction() {
                when(config.getInitParameter(PARAM_NAME)).thenReturn("foo");
                StringParameter parameter = StringParameter.of(config, PARAM_NAME).transform(String::toUpperCase);
                assertTrue(parameter.isSet());
                assertEquals("FOO", parameter.requiredValue());
            }

            @Test
            @DisplayName("function returning null")
            void testFunctionReturningNull() {
                when(config.getInitParameter(PARAM_NAME)).thenReturn("foo");
                StringParameter parameter = StringParameter.of(config, PARAM_NAME).transform(s -> null);
                assertFalse(parameter.isSet());
            }

            @Test
            @DisplayName("null function")
            void testNullFunction() {
                when(config.getInitParameter(PARAM_NAME)).thenReturn("foo");
                StringParameter parameter = StringParameter.of(config, PARAM_NAME);
                assertThrows(NullPointerException.class, () -> parameter.transform(null));
            }
        }

        @Test
        @DisplayName("toString()")
        void testToString() {
            when(config.getInitParameter(PARAM_NAME)).thenReturn("foo");
            StringParameter parameter = StringParameter.of(config, PARAM_NAME);
            assertEquals(String.format("%s=%s", PARAM_NAME, "foo"), parameter.toString());
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
            StringParameter parameter = StringParameter.of(config, PARAM_NAME);
            assertFalse(parameter.isSet());
        }

        @Test
        @DisplayName("requiredValue()")
        void testRequiredValue() {
            StringParameter parameter = StringParameter.of(config, PARAM_NAME);
            assertThrows(IllegalStateException.class, parameter::requiredValue);
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = { "foo", "bar" })
        @DisplayName("valueWithDefault(String)")
        void testValueWithDefault(String defaultValue) {
            StringParameter parameter = StringParameter.of(config, PARAM_NAME);
            assertEquals(defaultValue, parameter.valueWithDefault(defaultValue));
        }

        @Nested
        @DisplayName("atLeast(String)")
        class AtLeast {

            @Test
            @DisplayName("non-null minimum")
            void testMatchingValue() {
                StringParameter parameter = StringParameter.of(config, PARAM_NAME);
                assertDoesNotThrow(() -> parameter.atLeast("foo"));
            }

            @Test
            @DisplayName("null minimum")
            void testNullMinimum() {
                StringParameter parameter = StringParameter.of(config, PARAM_NAME);
                assertThrows(NullPointerException.class, () -> parameter.atLeast(null));
            }
        }

        @Nested
        @DisplayName("atMost(String)")
        class AtMost {

            @Test
            @DisplayName("non-null maximum")
            void testMatchingValue() {
                StringParameter parameter = StringParameter.of(config, PARAM_NAME);
                assertDoesNotThrow(() -> parameter.atMost("foo"));
            }

            @Test
            @DisplayName("null maximum")
            void testNullMaximum() {
                StringParameter parameter = StringParameter.of(config, PARAM_NAME);
                assertThrows(NullPointerException.class, () -> parameter.atMost(null));
            }
        }

        @Nested
        @DisplayName("greaterThan(String)")
        class GreaterThan {

            @Test
            @DisplayName("non-null minimum")
            void testMatchingValue() {
                StringParameter parameter = StringParameter.of(config, PARAM_NAME);
                assertDoesNotThrow(() -> parameter.greaterThan("foo"));
            }

            @Test
            @DisplayName("null minimum")
            void testNullMinimum() {
                StringParameter parameter = StringParameter.of(config, PARAM_NAME);
                assertThrows(NullPointerException.class, () -> parameter.greaterThan(null));
            }
        }

        @Nested
        @DisplayName("smallerThan(String)")
        class SmallerThan {

            @Test
            @DisplayName("non-null maximum")
            void testMatchingValue() {
                StringParameter parameter = StringParameter.of(config, PARAM_NAME);
                assertDoesNotThrow(() -> parameter.smallerThan("foo"));
            }

            @Test
            @DisplayName("null maximum")
            void testNullMaximum() {
                StringParameter parameter = StringParameter.of(config, PARAM_NAME);
                assertThrows(NullPointerException.class, () -> parameter.smallerThan(null));
            }
        }

        @Nested
        @DisplayName("between(String, String)")
        class Between {

            @Test
            @DisplayName("non-null minimum and maximum")
            void testMatchingValue() {
                StringParameter parameter = StringParameter.of(config, PARAM_NAME);
                assertDoesNotThrow(() -> parameter.between("foo", "world"));
            }

            @Test
            @DisplayName("null minimum")
            void testNullMinimum() {
                StringParameter parameter = StringParameter.of(config, PARAM_NAME);
                assertThrows(NullPointerException.class, () -> parameter.between(null, "world"));
            }

            @Test
            @DisplayName("null maximum")
            void testNullMaximum() {
                StringParameter parameter = StringParameter.of(config, PARAM_NAME);
                assertThrows(NullPointerException.class, () -> parameter.between("foo", null));
            }
        }

        @Test
        @DisplayName("notEmpty")
        void testNotEmpty() {
            StringParameter parameter = StringParameter.of(config, PARAM_NAME);
            assertDoesNotThrow(parameter::notEmpty);
        }

        @Test
        @DisplayName("notBlank")
        void testNotBlank() {
            StringParameter parameter = StringParameter.of(config, PARAM_NAME);
            assertDoesNotThrow(parameter::notBlank);
        }

        @Nested
        @DisplayName("matching")
        class Matching {

            @Test
            @DisplayName("non-null pattern")
            void testNonNullPattern() {
                StringParameter parameter = StringParameter.of(config, PARAM_NAME);
                Pattern pattern = Pattern.compile("\\d+");
                assertDoesNotThrow(() -> parameter.matching(pattern));
            }

            @Test
            @DisplayName("null pattern")
            void testNullPattern() {
                StringParameter parameter = StringParameter.of(config, PARAM_NAME);
                assertThrows(NullPointerException.class, () -> parameter.matching(null));
            }
        }

        @Nested
        @DisplayName("transform(Function)")
        class Transform {

            @Test
            @DisplayName("non-null function")
            void testNonNullFunction() {
                @SuppressWarnings("unchecked")
                Function<String, String> function = mock(Function.class);

                StringParameter parameter = StringParameter.of(config, PARAM_NAME).transform(function);
                assertFalse(parameter.isSet());

                verifyNoInteractions(function);
            }

            @Test
            @DisplayName("null function")
            void testNullFunction() {
                when(config.getInitParameter(PARAM_NAME)).thenReturn("foo");
                StringParameter parameter = StringParameter.of(config, PARAM_NAME);
                assertThrows(NullPointerException.class, () -> parameter.transform(null));
            }
        }

        @Test
        @DisplayName("toString()")
        void testToString() {
            StringParameter parameter = StringParameter.of(config, PARAM_NAME);
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
            StringParameter parameter = StringParameter.of(config, PARAM_NAME);
            assertFalse(parameter.isSet());
        }

        @Test
        @DisplayName("parameter set")
        void testParameterSet() {
            when(config.getInitParameter(PARAM_NAME)).thenReturn("foo");
            StringParameter parameter = StringParameter.of(config, PARAM_NAME);
            assertTrue(parameter.isSet());
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
            StringParameter parameter = StringParameter.of(config, PARAM_NAME);
            assertFalse(parameter.isSet());
        }

        @Test
        @DisplayName("parameter set")
        void testParameterSet() {
            when(config.getInitParameter(PARAM_NAME)).thenReturn("1.0");
            StringParameter parameter = StringParameter.of(config, PARAM_NAME);
            assertTrue(parameter.isSet());
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
            StringParameter parameter = StringParameter.of(context, PARAM_NAME);
            assertFalse(parameter.isSet());
        }

        @Test
        @DisplayName("parameter set")
        void testParameterSet() {
            when(context.getInitParameter(PARAM_NAME)).thenReturn("1.0");
            StringParameter parameter = StringParameter.of(context, PARAM_NAME);
            assertTrue(parameter.isSet());
        }
    }
}
