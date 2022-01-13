/*
 * OutputTransformingHttpServletResponseWrapperTest.java
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

package com.github.robtimus.servlet.http;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OutputTransformingHttpServletResponseWrapperTest {

    @Nested
    @DisplayName("getOutputStream()")
    class GetOutputStream {

        @Nested
        @DisplayName("without transformation")
        class WithoutTransformation {

            @Test
            @DisplayName("throws IllegalStateException if wrapped throws")
            @SuppressWarnings("resource")
            void testThrowsIllegalStateException() throws IOException {
                HttpServletResponse response = mock(HttpServletResponse.class);

                when(response.getOutputStream()).thenThrow(IllegalArgumentException.class);

                HttpServletResponse wrapper = new OutputTransformingHttpServletResponseWrapper(response);

                assertThrows(IllegalArgumentException.class, wrapper::getOutputStream);
            }

            @Test
            @DisplayName("returns same output stream")
            @SuppressWarnings("resource")
            void testReturnsSameOutputStream() throws IOException {
                HttpServletResponse response = mock(HttpServletResponse.class);

                ServletOutputStream outputStream = mock(ServletOutputStream.class);

                when(response.getOutputStream()).thenReturn(outputStream);

                HttpServletResponse wrapper = new OutputTransformingHttpServletResponseWrapper(response);

                assertSame(outputStream, wrapper.getOutputStream());
            }
        }

        @Nested
        @DisplayName("with transformation")
        class WithTransformation {

            @Test
            @DisplayName("throws IllegalStateException if wrapped throws")
            @SuppressWarnings("resource")
            void testThrowsIllegalStateException() throws IOException {
                HttpServletResponse response = mock(HttpServletResponse.class);

                when(response.getOutputStream()).thenThrow(IllegalArgumentException.class);

                HttpServletResponse wrapper = createWrapper(response);

                assertThrows(IllegalArgumentException.class, wrapper::getOutputStream);
            }

            @Test
            @DisplayName("returns same output stream when called multiple times")
            @SuppressWarnings("resource")
            void testReturnsSameOutputStream() throws IOException {
                HttpServletResponse response = mock(HttpServletResponse.class);

                ServletOutputStream outputStream = mock(ServletOutputStream.class);

                when(response.getOutputStream()).thenReturn(outputStream);

                HttpServletResponse wrapper = createWrapper(response);

                OutputStream wrappedStream = wrapper.getOutputStream();
                assertNotSame(outputStream, wrappedStream);

                assertSame(wrappedStream, wrapper.getOutputStream());
                assertSame(wrappedStream, wrapper.getOutputStream());
            }

            @Test
            @DisplayName("returns different output stream when backing stream changes")
            @SuppressWarnings("resource")
            void testReturnsDifferentOutputStream() throws IOException {
                HttpServletResponse response = mock(HttpServletResponse.class);

                ServletOutputStream outputStream = mock(ServletOutputStream.class);

                when(response.getOutputStream()).thenReturn(outputStream, outputStream, mock(ServletOutputStream.class));

                HttpServletResponse wrapper = createWrapper(response);

                OutputStream wrappedStream = wrapper.getOutputStream();
                assertNotSame(outputStream, wrappedStream);

                assertSame(wrappedStream, wrapper.getOutputStream());

                // The third call returns a different output stream
                assertNotSame(wrappedStream, wrapper.getOutputStream());
            }
        }
    }

    @Nested
    @DisplayName("getWriter()")
    class GetWriter {

        @Nested
        @DisplayName("without transformation")
        class WithoutTransformation {

            @Test
            @DisplayName("throws IllegalStateException if wrapped throws")
            @SuppressWarnings("resource")
            void testThrowsIllegalStateException() throws IOException {
                HttpServletResponse response = mock(HttpServletResponse.class);

                when(response.getWriter()).thenThrow(IllegalArgumentException.class);

                HttpServletResponse wrapper = new OutputTransformingHttpServletResponseWrapper(response);

                assertThrows(IllegalArgumentException.class, wrapper::getWriter);
            }

            @Test
            @DisplayName("returns same writer")
            @SuppressWarnings("resource")
            void testReturnsSameWriter() throws IOException {
                HttpServletResponse response = mock(HttpServletResponse.class);

                PrintWriter writer = mock(PrintWriter.class);

                when(response.getWriter()).thenReturn(writer);

                HttpServletResponse wrapper = new OutputTransformingHttpServletResponseWrapper(response);

                assertSame(writer, wrapper.getWriter());
            }
        }

        @Nested
        @DisplayName("with transformation")
        class WithTransformation {

            @Test
            @DisplayName("throws IllegalStateException if wrapped throws")
            @SuppressWarnings("resource")
            void testThrowsIllegalStateException() throws IOException {
                HttpServletResponse response = mock(HttpServletResponse.class);

                when(response.getWriter()).thenThrow(IllegalArgumentException.class);

                HttpServletResponse wrapper = createWrapper(response);

                assertThrows(IllegalArgumentException.class, wrapper::getWriter);
            }

            @Test
            @DisplayName("returns same writer when called multiple times")
            @SuppressWarnings("resource")
            void testReturnsSameWriter() throws IOException {
                HttpServletResponse response = mock(HttpServletResponse.class);

                PrintWriter writer = mock(PrintWriter.class);

                when(response.getWriter()).thenReturn(writer);

                HttpServletResponse wrapper = createWrapper(response);

                PrintWriter wrappedWriter = wrapper.getWriter();
                assertNotSame(writer, wrappedWriter);

                assertSame(wrappedWriter, wrapper.getWriter());
                assertSame(wrappedWriter, wrapper.getWriter());
            }

            @Test
            @DisplayName("returns different writer when backing writer changes")
            @SuppressWarnings("resource")
            void testReturnsDifferentWriter() throws IOException {
                HttpServletResponse response = mock(HttpServletResponse.class);

                PrintWriter writer = mock(PrintWriter.class);

                when(response.getWriter()).thenReturn(writer, writer, mock(PrintWriter.class));

                HttpServletResponse wrapper = createWrapper(response);

                PrintWriter wrappedWriter = wrapper.getWriter();
                assertNotSame(writer, wrappedWriter);

                assertSame(wrappedWriter, wrapper.getWriter());

                // The third call returns a different writer
                assertNotSame(wrappedWriter, wrapper.getWriter());
            }
        }
    }

    @Nested
    @DisplayName("resetBuffer()")
    class ResetBuffer {

        @Test
        @DisplayName("with output stream")
        @SuppressWarnings("resource")
        void testWithOutputStream() throws IOException {
            HttpServletResponse response = mock(HttpServletResponse.class);

            when(response.getOutputStream()).thenReturn(mock(ServletOutputStream.class));

            HttpServletResponse wrapper = createWrapper(response);

            ServletOutputStream wrappedStream = wrapper.getOutputStream();

            assertSame(wrappedStream, wrapper.getOutputStream());

            wrapper.resetBuffer();

            assertNotSame(wrappedStream, wrapper.getOutputStream());
        }

        @Test
        @DisplayName("with writer")
        @SuppressWarnings("resource")
        void testWithWriter() throws IOException {
            HttpServletResponse response = mock(HttpServletResponse.class);

            when(response.getWriter()).thenReturn(mock(PrintWriter.class));

            HttpServletResponse wrapper = createWrapper(response);

            PrintWriter wrappedWriter = wrapper.getWriter();

            assertSame(wrappedWriter, wrapper.getWriter());

            wrapper.resetBuffer();

            assertNotSame(wrappedWriter, wrapper.getWriter());
        }
    }

    @Nested
    @DisplayName("reset()")
    class Reset {

        @Test
        @DisplayName("with output stream")
        @SuppressWarnings("resource")
        void testWithOutputStream() throws IOException {
            HttpServletResponse response = mock(HttpServletResponse.class);

            when(response.getOutputStream()).thenReturn(mock(ServletOutputStream.class));

            HttpServletResponse wrapper = createWrapper(response);

            ServletOutputStream wrappedStream = wrapper.getOutputStream();

            assertSame(wrappedStream, wrapper.getOutputStream());

            wrapper.reset();

            assertNotSame(wrappedStream, wrapper.getOutputStream());
        }

        @Test
        @DisplayName("with writer")
        @SuppressWarnings("resource")
        void testWithWriter() throws IOException {
            HttpServletResponse response = mock(HttpServletResponse.class);

            when(response.getWriter()).thenReturn(mock(PrintWriter.class));

            HttpServletResponse wrapper = createWrapper(response);

            PrintWriter wrappedWriter = wrapper.getWriter();

            assertSame(wrappedWriter, wrapper.getWriter());

            wrapper.reset();

            assertNotSame(wrappedWriter, wrapper.getWriter());
        }
    }

    private HttpServletResponse createWrapper(HttpServletResponse response) {
        return new OutputTransformingHttpServletResponseWrapper(response) {
            @Override
            protected OutputStream transform(ServletOutputStream outputStream) throws IOException {
                return new ByteArrayOutputStream();
            }

            @Override
            protected Writer transform(PrintWriter writer) throws IOException {
                return new StringWriter();
            }
        };
    }
}
