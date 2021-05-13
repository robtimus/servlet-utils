/*
 * ServletConfigWrapperTest.java
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

package com.github.robtimus.javax.servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@SuppressWarnings("nls")
class ServletConfigWrapperTest {

    private ServletConfig servletConfig;

    @BeforeEach
    void initServletConfig() {
        servletConfig = mock(ServletConfig.class);
    }

    @Test
    @DisplayName("delegates getServletName()")
    void testGetServletName() {
        when(servletConfig.getServletName()).thenReturn("testServlet");

        ServletConfigWrapper wrapper = new ServletConfigWrapper(servletConfig);

        assertEquals("testServlet", wrapper.getServletName());

        verify(servletConfig).getServletName();
        verifyNoMoreInteractions(servletConfig);
    }

    @Test
    @DisplayName("delegates getServletContext()")
    void testGetServletContext() {
        ServletContext servletContext = mock(ServletContext.class);

        when(servletConfig.getServletContext()).thenReturn(servletContext);

        ServletConfigWrapper wrapper = new ServletConfigWrapper(servletConfig);

        assertSame(servletContext, wrapper.getServletContext());

        verify(servletConfig).getServletContext();
        verifyNoMoreInteractions(servletConfig);
    }

    @Nested
    @DisplayName("getInitParameter(String)")
    class GetInitParameter {

        @Nested
        @DisplayName("with no additional parameters")
        class WithNoAdditionalParameters {

            @Test
            @DisplayName("non-existing parameter")
            void testNonExistingParameter() {
                ServletConfigWrapper wrapper = new ServletConfigWrapper(servletConfig);

                assertNull(wrapper.getInitParameter("nonExisting"));

                verify(servletConfig).getInitParameter("nonExisting");
                verifyNoMoreInteractions(servletConfig);
            }

            @Test
            @DisplayName("existing parameter")
            void testExistingParameter() {
                when(servletConfig.getInitParameter("existing")).thenReturn("value");

                ServletConfigWrapper wrapper = new ServletConfigWrapper(servletConfig);

                assertEquals("value", wrapper.getInitParameter("existing"));

                verify(servletConfig).getInitParameter("existing");
                verifyNoMoreInteractions(servletConfig);
            }
        }

        @Nested
        @DisplayName("with extra parameters")
        class WithExtraParameters {

            @Test
            @DisplayName("non-existing parameter")
            void testNonExistingParameter() {
                ServletConfigWrapper wrapper = new ServletConfigWrapper(servletConfig)
                        .withInitParameter("boolean", true)
                        .withInitParameter("int", 13);

                assertNull(wrapper.getInitParameter("nonExisting"));

                verify(servletConfig).getInitParameter("nonExisting");
                verifyNoMoreInteractions(servletConfig);
            }

            @Test
            @DisplayName("existing original parameter")
            void testExistingOriginalParameter() {
                when(servletConfig.getInitParameter("existing")).thenReturn("value");

                ServletConfigWrapper wrapper = new ServletConfigWrapper(servletConfig)
                        .withInitParameter("boolean", true)
                        .withInitParameter("int", 13);

                assertEquals("value", wrapper.getInitParameter("existing"));

                verify(servletConfig).getInitParameter("existing");
                verifyNoMoreInteractions(servletConfig);
            }

            @Test
            @DisplayName("existing extra parameter")
            void testExistingExtraParameter() {
                ServletConfigWrapper wrapper = new ServletConfigWrapper(servletConfig)
                        .withInitParameter("boolean", true)
                        .withInitParameter("int", 13);

                assertEquals("true", wrapper.getInitParameter("boolean"));
                assertEquals("13", wrapper.getInitParameter("int"));

                verifyNoInteractions(servletConfig);
            }

            @Test
            @DisplayName("existing overwritten parameter")
            void testExistingOverwrittenParameter() {
                when(servletConfig.getInitParameter("existing")).thenReturn("value");

                ServletConfigWrapper wrapper = new ServletConfigWrapper(servletConfig)
                        .withInitParameter("boolean", true)
                        .withInitParameter("int", 13)
                        .withInitParameter("existing", "overwritten")
                        ;

                assertEquals("overwritten", wrapper.getInitParameter("existing"));

                verifyNoInteractions(servletConfig);
            }
        }
    }

    @Nested
    @DisplayName("getInitParameterNames()")
    class GetInitParameterNames {

        @Nested
        @DisplayName("with no original parameters")
        class WithNoOriginalParameters {

            @BeforeEach
            void initNames() {
                when(servletConfig.getInitParameterNames()).thenAnswer(i -> Collections.emptyEnumeration());
            }

            @Nested
            @DisplayName("with no additional parameters")
            class WithNoAdditionalParameters {

                private ServletConfigWrapper wrapper;

                @BeforeEach
                void initWrapper() {
                    wrapper = new ServletConfigWrapper(servletConfig);
                }

                @Test
                @DisplayName("proper flow")
                void testProperFlow() {
                    Enumeration<String> names = wrapper.getInitParameterNames();

                    assertFalse(names.hasMoreElements());
                    assertThrows(NoSuchElementException.class, names::nextElement);

                    assertFalse(names.hasMoreElements());
                }

                @Test
                @DisplayName("with extra hasMoreElements()")
                void testWithExtraHasMoreElements() {
                    Enumeration<String> names = wrapper.getInitParameterNames();

                    assertFalse(names.hasMoreElements());
                    assertFalse(names.hasMoreElements());
                    assertThrows(NoSuchElementException.class, names::nextElement);

                    assertFalse(names.hasMoreElements());
                    assertFalse(names.hasMoreElements());
                }

                @Test
                @DisplayName("without hasMoreElements()")
                void testWithoutHasMoreElements() {
                    Enumeration<String> names = wrapper.getInitParameterNames();

                    assertThrows(NoSuchElementException.class, names::nextElement);
                }
            }

            @Nested
            @DisplayName("with extra parameters")
            class WithExtraParameters {

                private ServletConfigWrapper wrapper;

                @BeforeEach
                void initWrapper() {
                    wrapper = new ServletConfigWrapper(servletConfig)
                            .withInitParameter("param1", "value1")
                            .withInitParameter("param2", "value2")
                            .withInitParameter("param3", "value3");
                }

                @Test
                @DisplayName("proper flow")
                void testProperFlow() {
                    Enumeration<String> names = wrapper.getInitParameterNames();

                    assertTrue(names.hasMoreElements());
                    assertEquals("param1", names.nextElement());

                    assertTrue(names.hasMoreElements());
                    assertEquals("param2", names.nextElement());

                    assertTrue(names.hasMoreElements());
                    assertEquals("param3", names.nextElement());

                    assertFalse(names.hasMoreElements());
                    assertThrows(NoSuchElementException.class, names::nextElement);

                    assertFalse(names.hasMoreElements());
                }

                @Test
                @DisplayName("with extra hasMoreElements()")
                void testWithExtraHasMoreElements() {
                    Enumeration<String> names = wrapper.getInitParameterNames();

                    assertTrue(names.hasMoreElements());
                    assertTrue(names.hasMoreElements());
                    assertEquals("param1", names.nextElement());

                    assertTrue(names.hasMoreElements());
                    assertTrue(names.hasMoreElements());
                    assertEquals("param2", names.nextElement());

                    assertTrue(names.hasMoreElements());
                    assertTrue(names.hasMoreElements());
                    assertEquals("param3", names.nextElement());

                    assertFalse(names.hasMoreElements());
                    assertFalse(names.hasMoreElements());
                    assertThrows(NoSuchElementException.class, names::nextElement);

                    assertFalse(names.hasMoreElements());
                    assertFalse(names.hasMoreElements());
                }

                @Test
                @DisplayName("without hasMoreElements()")
                void testWithoutHasMoreElements() {
                    Enumeration<String> names = wrapper.getInitParameterNames();

                    assertEquals("param1", names.nextElement());

                    assertEquals("param2", names.nextElement());

                    assertEquals("param3", names.nextElement());

                    assertThrows(NoSuchElementException.class, names::nextElement);
                }
            }
        }

        @Nested
        @DisplayName("with original parameters")
        class WithOriginalParameters {

            @BeforeEach
            void initNames() {
                when(servletConfig.getInitParameterNames()).thenAnswer(i -> Collections.enumeration(Arrays.asList("param1", "param2", "param3")));
            }

            @Nested
            @DisplayName("with no additional parameters")
            class WithNoAdditionalParameters {

                private ServletConfigWrapper wrapper;

                @BeforeEach
                void initWrapper() {
                    wrapper = new ServletConfigWrapper(servletConfig);
                }

                @Test
                @DisplayName("proper flow")
                void testProperFlow() {
                    Enumeration<String> names = wrapper.getInitParameterNames();

                    assertTrue(names.hasMoreElements());
                    assertEquals("param1", names.nextElement());

                    assertTrue(names.hasMoreElements());
                    assertEquals("param2", names.nextElement());

                    assertTrue(names.hasMoreElements());
                    assertEquals("param3", names.nextElement());

                    assertFalse(names.hasMoreElements());
                    assertThrows(NoSuchElementException.class, names::nextElement);

                    assertFalse(names.hasMoreElements());
                }

                @Test
                @DisplayName("with extra hasMoreElements()")
                void testWithExtraHasMoreElements() {
                    Enumeration<String> names = wrapper.getInitParameterNames();

                    assertTrue(names.hasMoreElements());
                    assertTrue(names.hasMoreElements());
                    assertEquals("param1", names.nextElement());

                    assertTrue(names.hasMoreElements());
                    assertTrue(names.hasMoreElements());
                    assertEquals("param2", names.nextElement());

                    assertTrue(names.hasMoreElements());
                    assertTrue(names.hasMoreElements());
                    assertEquals("param3", names.nextElement());

                    assertFalse(names.hasMoreElements());
                    assertFalse(names.hasMoreElements());
                    assertThrows(NoSuchElementException.class, names::nextElement);

                    assertFalse(names.hasMoreElements());
                }

                @Test
                @DisplayName("without hasMoreElements()")
                void testWithoutHasMoreElements() {
                    Enumeration<String> names = wrapper.getInitParameterNames();

                    assertEquals("param1", names.nextElement());

                    assertEquals("param2", names.nextElement());

                    assertEquals("param3", names.nextElement());

                    assertThrows(NoSuchElementException.class, names::nextElement);
                }
            }

            @Nested
            @DisplayName("with extra parameters")
            class WithExtraParameters {

                private ServletConfigWrapper wrapper;

                @BeforeEach
                void initWrapper() {
                    wrapper = new ServletConfigWrapper(servletConfig)
                            .withInitParameter("param4", "value4")
                            .withInitParameter("param5", "value5")
                            .withInitParameter("param6", "value6");
                }

                @Test
                @DisplayName("proper flow")
                void testProperFlow() {
                    Enumeration<String> names = wrapper.getInitParameterNames();

                    assertTrue(names.hasMoreElements());
                    assertEquals("param1", names.nextElement());

                    assertTrue(names.hasMoreElements());
                    assertEquals("param2", names.nextElement());

                    assertTrue(names.hasMoreElements());
                    assertEquals("param3", names.nextElement());

                    assertTrue(names.hasMoreElements());
                    assertEquals("param4", names.nextElement());

                    assertTrue(names.hasMoreElements());
                    assertEquals("param5", names.nextElement());

                    assertTrue(names.hasMoreElements());
                    assertEquals("param6", names.nextElement());

                    assertFalse(names.hasMoreElements());
                    assertThrows(NoSuchElementException.class, names::nextElement);

                    assertFalse(names.hasMoreElements());
                }

                @Test
                @DisplayName("with extra hasMoreElements()")
                void testWithExtraHasMoreElements() {
                    Enumeration<String> names = wrapper.getInitParameterNames();

                    assertTrue(names.hasMoreElements());
                    assertTrue(names.hasMoreElements());
                    assertEquals("param1", names.nextElement());

                    assertTrue(names.hasMoreElements());
                    assertTrue(names.hasMoreElements());
                    assertEquals("param2", names.nextElement());

                    assertTrue(names.hasMoreElements());
                    assertTrue(names.hasMoreElements());
                    assertEquals("param3", names.nextElement());

                    assertTrue(names.hasMoreElements());
                    assertTrue(names.hasMoreElements());
                    assertEquals("param4", names.nextElement());

                    assertTrue(names.hasMoreElements());
                    assertTrue(names.hasMoreElements());
                    assertEquals("param5", names.nextElement());

                    assertTrue(names.hasMoreElements());
                    assertTrue(names.hasMoreElements());
                    assertEquals("param6", names.nextElement());

                    assertFalse(names.hasMoreElements());
                    assertFalse(names.hasMoreElements());
                    assertThrows(NoSuchElementException.class, names::nextElement);

                    assertFalse(names.hasMoreElements());
                    assertFalse(names.hasMoreElements());
                }

                @Test
                @DisplayName("without hasMoreElements()")
                void testWithoutHasMoreElements() {
                    Enumeration<String> names = wrapper.getInitParameterNames();

                    assertEquals("param1", names.nextElement());

                    assertEquals("param2", names.nextElement());

                    assertEquals("param3", names.nextElement());

                    assertEquals("param4", names.nextElement());

                    assertEquals("param5", names.nextElement());

                    assertEquals("param6", names.nextElement());

                    assertThrows(NoSuchElementException.class, names::nextElement);
                }
            }

            @Nested
            @DisplayName("with only overwritten parameters")
            class WithOnlyOverwrittenParameters {

                private ServletConfigWrapper wrapper;

                @BeforeEach
                void initWrapper() {
                    wrapper = new ServletConfigWrapper(servletConfig)
                            .withInitParameter("param1", "value1")
                            .withInitParameter("param2", "value2")
                            .withInitParameter("param3", "value3");
                }

                @Test
                @DisplayName("proper flow")
                void testProperFlow() {
                    Enumeration<String> names = wrapper.getInitParameterNames();

                    assertTrue(names.hasMoreElements());
                    assertEquals("param1", names.nextElement());

                    assertTrue(names.hasMoreElements());
                    assertEquals("param2", names.nextElement());

                    assertTrue(names.hasMoreElements());
                    assertEquals("param3", names.nextElement());

                    assertFalse(names.hasMoreElements());
                    assertThrows(NoSuchElementException.class, names::nextElement);

                    assertFalse(names.hasMoreElements());
                }

                @Test
                @DisplayName("with extra hasMoreElements()")
                void testWithExtraHasMoreElements() {
                    Enumeration<String> names = wrapper.getInitParameterNames();

                    assertTrue(names.hasMoreElements());
                    assertTrue(names.hasMoreElements());
                    assertEquals("param1", names.nextElement());

                    assertTrue(names.hasMoreElements());
                    assertTrue(names.hasMoreElements());
                    assertEquals("param2", names.nextElement());

                    assertTrue(names.hasMoreElements());
                    assertTrue(names.hasMoreElements());
                    assertEquals("param3", names.nextElement());

                    assertFalse(names.hasMoreElements());
                    assertFalse(names.hasMoreElements());
                    assertThrows(NoSuchElementException.class, names::nextElement);

                    assertFalse(names.hasMoreElements());
                    assertFalse(names.hasMoreElements());
                }

                @Test
                @DisplayName("without hasMoreElements()")
                void testWithoutHasMoreElements() {
                    Enumeration<String> names = wrapper.getInitParameterNames();

                    assertEquals("param1", names.nextElement());

                    assertEquals("param2", names.nextElement());

                    assertEquals("param3", names.nextElement());

                    assertThrows(NoSuchElementException.class, names::nextElement);
                }
            }

            @Nested
            @DisplayName("with both extra and overwritten parameters")
            class WithExtraAndOverwrittenParameters {

                private ServletConfigWrapper wrapper;

                @BeforeEach
                void initWrapper() {
                    wrapper = new ServletConfigWrapper(servletConfig)
                            .withInitParameter("param1", "value1")
                            .withInitParameter("param2", "value2")
                            .withInitParameter("param4", "value4")
                            .withInitParameter("param5", "value5");
                }

                @Test
                @DisplayName("proper flow")
                void testProperFlow() {
                    Enumeration<String> names = wrapper.getInitParameterNames();

                    assertTrue(names.hasMoreElements());
                    assertEquals("param1", names.nextElement());

                    assertTrue(names.hasMoreElements());
                    assertEquals("param2", names.nextElement());

                    assertTrue(names.hasMoreElements());
                    assertEquals("param3", names.nextElement());

                    assertTrue(names.hasMoreElements());
                    assertEquals("param4", names.nextElement());

                    assertTrue(names.hasMoreElements());
                    assertEquals("param5", names.nextElement());

                    assertFalse(names.hasMoreElements());
                    assertThrows(NoSuchElementException.class, names::nextElement);

                    assertFalse(names.hasMoreElements());
                }

                @Test
                @DisplayName("with extra hasMoreElements()")
                void testWithExtraHasMoreElements() {
                    Enumeration<String> names = wrapper.getInitParameterNames();

                    assertTrue(names.hasMoreElements());
                    assertTrue(names.hasMoreElements());
                    assertEquals("param1", names.nextElement());

                    assertTrue(names.hasMoreElements());
                    assertTrue(names.hasMoreElements());
                    assertEquals("param2", names.nextElement());

                    assertTrue(names.hasMoreElements());
                    assertTrue(names.hasMoreElements());
                    assertEquals("param3", names.nextElement());

                    assertTrue(names.hasMoreElements());
                    assertTrue(names.hasMoreElements());
                    assertEquals("param4", names.nextElement());

                    assertTrue(names.hasMoreElements());
                    assertTrue(names.hasMoreElements());
                    assertEquals("param5", names.nextElement());

                    assertFalse(names.hasMoreElements());
                    assertFalse(names.hasMoreElements());
                    assertThrows(NoSuchElementException.class, names::nextElement);

                    assertFalse(names.hasMoreElements());
                    assertFalse(names.hasMoreElements());
                }

                @Test
                @DisplayName("without hasMoreElements()")
                void testWithoutHasMoreElements() {
                    Enumeration<String> names = wrapper.getInitParameterNames();

                    assertEquals("param1", names.nextElement());

                    assertEquals("param2", names.nextElement());

                    assertEquals("param3", names.nextElement());

                    assertEquals("param4", names.nextElement());

                    assertEquals("param5", names.nextElement());

                    assertThrows(NoSuchElementException.class, names::nextElement);
                }
            }
        }
    }
}
