/*
 * InputTransformingHttpServletRequestWrapperTest.java
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
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@SuppressWarnings("nls")
class InputTransformingHttpServletRequestWrapperTest {

    @Nested
    @DisplayName("getInputStream()")
    class GetInputStream {

        @Nested
        @DisplayName("without transformation")
        class WithoutTransformation {

            @Test
            @DisplayName("throws IllegalStateException if wrapped throws")
            @SuppressWarnings("resource")
            void testThrowsIllegalStateException() throws IOException {
                HttpServletRequest request = mock(HttpServletRequest.class);

                when(request.getInputStream()).thenThrow(IllegalArgumentException.class);

                HttpServletRequest wrapper = new InputTransformingHttpServletRequestWrapper(request);

                assertThrows(IllegalArgumentException.class, wrapper::getInputStream);
            }

            @Test
            @DisplayName("returns same input stream")
            @SuppressWarnings("resource")
            void testReturnsSameInputStream() throws IOException {
                HttpServletRequest request = mock(HttpServletRequest.class);

                ServletInputStream inputStream = mock(ServletInputStream.class);

                when(request.getInputStream()).thenReturn(inputStream);

                HttpServletRequest wrapper = new InputTransformingHttpServletRequestWrapper(request);

                assertSame(inputStream, wrapper.getInputStream());
            }
        }

        @Nested
        @DisplayName("with transformation")
        class WithTransformation {

            @Test
            @DisplayName("throws IllegalStateException if wrapped throws")
            @SuppressWarnings("resource")
            void testThrowsIllegalStateException() throws IOException {
                HttpServletRequest request = mock(HttpServletRequest.class);

                when(request.getInputStream()).thenThrow(IllegalArgumentException.class);

                HttpServletRequest wrapper = createWrapper(request);

                assertThrows(IllegalArgumentException.class, wrapper::getInputStream);
            }

            @Test
            @DisplayName("returns same input stream when called multiple times")
            @SuppressWarnings("resource")
            void testReturnsSameInputStream() throws IOException {
                HttpServletRequest request = mock(HttpServletRequest.class);

                ServletInputStream inputStream = mock(ServletInputStream.class);

                when(request.getInputStream()).thenReturn(inputStream);

                HttpServletRequest wrapper = createWrapper(request);

                InputStream wrappedStream = wrapper.getInputStream();
                assertNotSame(inputStream, wrappedStream);

                assertSame(wrappedStream, wrapper.getInputStream());
                assertSame(wrappedStream, wrapper.getInputStream());
            }

            @Test
            @DisplayName("returns different input stream when backing stream changes")
            @SuppressWarnings("resource")
            void testReturnsDifferentInputStream() throws IOException {
                HttpServletRequest request = mock(HttpServletRequest.class);

                ServletInputStream inputStream = mock(ServletInputStream.class);

                when(request.getInputStream()).thenReturn(inputStream, inputStream, mock(ServletInputStream.class));

                HttpServletRequest wrapper = createWrapper(request);

                InputStream wrappedStream = wrapper.getInputStream();
                assertNotSame(inputStream, wrappedStream);

                assertSame(wrappedStream, wrapper.getInputStream());

                // The third call returns a different input stream
                assertNotSame(wrappedStream, wrapper.getInputStream());
            }

            private HttpServletRequest createWrapper(HttpServletRequest request) {
                return new InputTransformingHttpServletRequestWrapper(request) {
                    @Override
                    protected InputStream transform(ServletInputStream inputStream) throws IOException {
                        return new ByteArrayInputStream("hello world".getBytes());
                    }
                };
            }
        }
    }

    @Nested
    @DisplayName("getReader()")
    class GetReader {

        @Nested
        @DisplayName("without transformation")
        class WithoutTransformation {

            @Test
            @DisplayName("throws IllegalStateException if wrapped throws")
            @SuppressWarnings("resource")
            void testThrowsIllegalStateException() throws IOException {
                HttpServletRequest request = mock(HttpServletRequest.class);

                when(request.getReader()).thenThrow(IllegalArgumentException.class);

                HttpServletRequest wrapper = new InputTransformingHttpServletRequestWrapper(request);

                assertThrows(IllegalArgumentException.class, wrapper::getReader);
            }

            @Test
            @DisplayName("returns same reader")
            @SuppressWarnings("resource")
            void testReturnsSameReader() throws IOException {
                HttpServletRequest request = mock(HttpServletRequest.class);

                BufferedReader reader = mock(BufferedReader.class);

                when(request.getReader()).thenReturn(reader);

                HttpServletRequest wrapper = new InputTransformingHttpServletRequestWrapper(request);

                assertSame(reader, wrapper.getReader());
            }
        }

        @Nested
        @DisplayName("with transformation")
        class WithTransformation {

            @Test
            @DisplayName("throws IllegalStateException if wrapped throws")
            @SuppressWarnings("resource")
            void testThrowsIllegalStateException() throws IOException {
                HttpServletRequest request = mock(HttpServletRequest.class);

                when(request.getReader()).thenThrow(IllegalArgumentException.class);

                HttpServletRequest wrapper = createWrapper(request);

                assertThrows(IllegalArgumentException.class, wrapper::getReader);
            }

            @Test
            @DisplayName("returns same reader when called multiple times")
            @SuppressWarnings("resource")
            void testReturnsSameReader() throws IOException {
                HttpServletRequest request = mock(HttpServletRequest.class);

                BufferedReader reader = mock(BufferedReader.class);

                when(request.getReader()).thenReturn(reader);

                HttpServletRequest wrapper = createWrapper(request);

                BufferedReader wrappedReader = wrapper.getReader();
                assertNotSame(reader, wrappedReader);

                assertSame(wrappedReader, wrapper.getReader());
                assertSame(wrappedReader, wrapper.getReader());
            }

            @Test
            @DisplayName("returns different reader when backing reader changes")
            @SuppressWarnings("resource")
            void testReturnsDifferentReader() throws IOException {
                HttpServletRequest request = mock(HttpServletRequest.class);

                BufferedReader reader = mock(BufferedReader.class);

                when(request.getReader()).thenReturn(reader, reader, mock(BufferedReader.class));

                HttpServletRequest wrapper = createWrapper(request);

                BufferedReader wrappedReader = wrapper.getReader();
                assertNotSame(reader, wrappedReader);

                assertSame(wrappedReader, wrapper.getReader());

                // The third call returns a different reader
                assertNotSame(wrappedReader, wrapper.getReader());
            }

            private HttpServletRequest createWrapper(HttpServletRequest request) {
                return new InputTransformingHttpServletRequestWrapper(request) {
                    @Override
                    protected Reader transform(BufferedReader reader) throws IOException {
                        return new StringReader("hello world");
                    }
                };
            }
        }
    }
}
