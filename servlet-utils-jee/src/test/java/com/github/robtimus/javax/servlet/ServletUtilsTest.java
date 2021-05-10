/*
 * ServletUtilsTest.java
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.github.robtimus.io.function.IOFunction;
import com.github.robtimus.javax.servlet.ServletUtils.ServletInputStreamWrapper;
import com.github.robtimus.javax.servlet.ServletUtils.ServletOutputStreamWrapper;

@SuppressWarnings("nls")
class ServletUtilsTest {

    @Nested
    @DisplayName("transform(ServletInputStream, IOFunction)")
    class TransformServletInputStream {

        @Test
        @DisplayName("null ServletInputStream")
        void testNullServletInputStream() {
            IOFunction<ServletInputStream, ServletInputStream> transformation = IOFunction.identity();

            assertThrows(NullPointerException.class, () -> ServletUtils.transform((ServletInputStream) null, transformation));
        }

        @Test
        @DisplayName("null transformation")
        void testNullTransformation() throws IOException {
            try (ServletInputStream inputStream = mock(ServletInputStream.class)) {
                assertThrows(NullPointerException.class, () -> ServletUtils.transform(inputStream, null));
            }
        }

        @Test
        @DisplayName("transformation returning ServletInputStream")
        void testTransformationReturningBufferedReader() throws IOException {
            IOFunction<ServletInputStream, ServletInputStream> transformation = IOFunction.identity();

            try (ServletInputStream inputStream = mock(ServletInputStream.class);
                    ServletInputStream transformed = ServletUtils.transform(inputStream, transformation)) {

                assertSame(inputStream, transformed);
            }
        }

        @Test
        @DisplayName("transformation returning non-ServletInputStream")
        void testTransformationReturningNonServletInputStream() throws IOException {
            byte[] data = "hello world".getBytes(StandardCharsets.UTF_8);

            ByteArrayInputStream transformationResult = new ByteArrayInputStream(data);
            IOFunction<ServletInputStream, InputStream> transformation = i -> transformationResult;

            try (ServletInputStream inputStream = mock(ServletInputStream.class);
                    ServletInputStream transformed = ServletUtils.transform(inputStream, transformation)) {

                assertNotSame(inputStream, transformed);
                assertThat(transformed, instanceOf(ServletInputStreamWrapper.class));

                byte[] buffer = new byte[data.length];
                assertEquals(buffer.length, transformed.read(buffer));
                assertArrayEquals(data, buffer);
                assertEquals(-1, transformed.read());
            }
        }
    }

    @Nested
    @DisplayName("transform(BufferedReader, IOFunction)")
    class TransformBufferedReader {

        @Test
        @DisplayName("null BufferedReader")
        void testNullBufferedReader() {
            IOFunction<BufferedReader, BufferedReader> transformation = IOFunction.identity();

            assertThrows(NullPointerException.class, () -> ServletUtils.transform((BufferedReader) null, transformation));
        }

        @Test
        @DisplayName("null transformation")
        void testNullTransformation() throws IOException {
            try (BufferedReader reader = new BufferedReader(new StringReader(""))) {
                assertThrows(NullPointerException.class, () -> ServletUtils.transform(reader, null));
            }
        }

        @Test
        @DisplayName("transformation returning BufferedReader")
        void testTransformationReturningBufferedReader() throws IOException {
            IOFunction<BufferedReader, BufferedReader> transformation = IOFunction.identity();

            try (BufferedReader reader = new BufferedReader(new StringReader(""));
                    BufferedReader transformed = ServletUtils.transform(reader, transformation)) {

                assertSame(reader, transformed);
            }
        }

        @Test
        @DisplayName("transformation returning non-BufferedReader")
        void testTransformationReturningNonBufferedReader() throws IOException {
            IOFunction<BufferedReader, Reader> transformation = r -> new StringReader("transformed");

            try (BufferedReader reader = new BufferedReader(new StringReader(""));
                    BufferedReader transformed = ServletUtils.transform(reader, transformation)) {

                assertNotSame(reader, transformed);
                assertEquals("transformed", transformed.readLine());
                assertNull(transformed.readLine());
            }
        }
    }

    @Nested
    @DisplayName("transform(ServletOutputStream, IOFunction)")
    class TransformServletOutputStream {

        @Test
        @DisplayName("null ServletOutputStream")
        void testNullServletOutputStream() {
            IOFunction<ServletOutputStream, ServletOutputStream> transformation = IOFunction.identity();

            assertThrows(NullPointerException.class, () -> ServletUtils.transform((ServletOutputStream) null, transformation));
        }

        @Test
        @DisplayName("null transformation")
        void testNullTransformation() throws IOException {
            try (ServletOutputStream outputStream = mock(ServletOutputStream.class)) {
                assertThrows(NullPointerException.class, () -> ServletUtils.transform(outputStream, null));
            }
        }

        @Test
        @DisplayName("transformation returning ServletOutputStream")
        void testTransformationReturningBufferedReader() throws IOException {
            IOFunction<ServletOutputStream, ServletOutputStream> transformation = IOFunction.identity();

            try (ServletOutputStream outputStream = mock(ServletOutputStream.class);
                    ServletOutputStream transformed = ServletUtils.transform(outputStream, transformation)) {

                assertSame(outputStream, transformed);
            }
        }

        @Test
        @DisplayName("transformation returning non-ServletOutputStream")
        void testTransformationReturningNonServletOutputStream() throws IOException {
            byte[] data = "hello world".getBytes(StandardCharsets.UTF_8);

            ByteArrayOutputStream transformationResult = new ByteArrayOutputStream();
            IOFunction<ServletOutputStream, OutputStream> transformation = i -> transformationResult;

            try (ServletOutputStream outputStream = mock(ServletOutputStream.class);
                    ServletOutputStream transformed = ServletUtils.transform(outputStream, transformation)) {

                assertNotSame(outputStream, transformed);
                assertThat(transformed, instanceOf(ServletOutputStreamWrapper.class));

                transformed.write(data);
                assertArrayEquals(data, transformationResult.toByteArray());
            }
        }
    }

    @Nested
    @DisplayName("transform(PrintWriter, IOFunction)")
    class TransformPrintWriter {

        @Test
        @DisplayName("null PrintWriter")
        void testNullPrintWriter() {
            IOFunction<PrintWriter, PrintWriter> transformation = IOFunction.identity();

            assertThrows(NullPointerException.class, () -> ServletUtils.transform((PrintWriter) null, transformation));
        }

        @Test
        @DisplayName("null transformation")
        void testNullTransformation() {
            try (PrintWriter writer = new PrintWriter(new StringWriter())) {
                assertThrows(NullPointerException.class, () -> ServletUtils.transform(writer, null));
            }
        }

        @Test
        @DisplayName("transformation returning PrintWriter")
        void testTransformationReturningPrintWriter() throws IOException {
            IOFunction<PrintWriter, PrintWriter> transformation = IOFunction.identity();

            try (PrintWriter writer = new PrintWriter(new StringWriter());
                    PrintWriter transformed = ServletUtils.transform(writer, transformation)) {

                assertSame(writer, transformed);
            }
        }

        @Test
        @DisplayName("transformation returning non-PrintWriter")
        void testTransformationReturningNonPrintWriter() throws IOException {
            StringWriter originalWriter = new StringWriter();
            StringWriter transformationResult = new StringWriter();
            IOFunction<PrintWriter, Writer> transformation = w -> transformationResult;

            try (PrintWriter writer = new PrintWriter(originalWriter);
                    PrintWriter transformed = ServletUtils.transform(writer, transformation)) {

                assertNotSame(writer, transformed);
                transformed.write("transformed");
                transformed.flush();
                assertEquals("transformed", transformationResult.toString());
                assertEquals("", originalWriter.toString());
            }
        }
    }

    @Nested
    class ServletInputStreamWrapperTest {

        @Test
        @DisplayName("delegates read()")
        @SuppressWarnings("resource")
        void testDelegatesReadByte() throws IOException {
            byte[] data = "hello world".getBytes(StandardCharsets.UTF_8);

            InputStream inputStream = spy(new ByteArrayInputStream(data));
            ServletInputStream servletInputStream = mock(ServletInputStream.class);

            try (ServletInputStream wrapper = new ServletInputStreamWrapper(inputStream, servletInputStream)) {
                for (int i = 0; i < data.length; i++) {
                    assertEquals(data[i], wrapper.read());
                }
                assertEquals(-1, wrapper.read());
            }

            verify(inputStream, times(data.length + 1)).read();
            verify(inputStream).close();
            verifyNoMoreInteractions(inputStream, servletInputStream);
        }

        @Test
        @DisplayName("delegates read(byte[])")
        @SuppressWarnings("resource")
        void testDelegatesReadIntoByteArray() throws IOException {
            byte[] data = "hello world".getBytes(StandardCharsets.UTF_8);

            InputStream inputStream = spy(new ByteArrayInputStream(data));
            ServletInputStream servletInputStream = mock(ServletInputStream.class);

            byte[] buffer = new byte[data.length - 3];

            try (ServletInputStream wrapper = new ServletInputStreamWrapper(inputStream, servletInputStream)) {
                byte[] expected = new byte[buffer.length];

                assertEquals(buffer.length, wrapper.read(buffer));
                System.arraycopy(data, 0, expected, 0, expected.length);
                assertArrayEquals(expected, buffer);

                assertEquals(3, wrapper.read(buffer));
                System.arraycopy(data, buffer.length, expected, 0, 3);
                assertArrayEquals(expected, buffer);

                assertEquals(-1, wrapper.read(buffer));
            }

            verify(inputStream, times(3)).read(buffer);
            // read(buffer) possibly delegates to read(buffer, 0, buffer.length)
            verify(inputStream, atMost(3)).read(buffer, 0, buffer.length);
            verify(inputStream).close();
            verifyNoMoreInteractions(inputStream, servletInputStream);
        }

        @Test
        @DisplayName("delegates read(byte[], int, int)")
        @SuppressWarnings("resource")
        void testDelegatesReadIntoByteArrayPortion() throws IOException {
            byte[] data = "hello world".getBytes(StandardCharsets.UTF_8);

            InputStream inputStream = spy(new ByteArrayInputStream(data));
            ServletInputStream servletInputStream = mock(ServletInputStream.class);

            byte[] buffer = new byte[data.length - 3];

            try (ServletInputStream wrapper = new ServletInputStreamWrapper(inputStream, servletInputStream)) {
                byte[] expected = new byte[buffer.length];

                assertEquals(5, wrapper.read(buffer, 0, 5));
                System.arraycopy(data, 0, expected, 0, 5);
                assertArrayEquals(expected, buffer);

                assertEquals(3, wrapper.read(buffer, 0, 3));
                System.arraycopy(data, 5, expected, 0, 3);
                assertArrayEquals(expected, buffer);

                assertEquals(data.length - 8, wrapper.read(buffer, 0, buffer.length));
                System.arraycopy(data, 8, expected, 0, data.length - 8);
                assertArrayEquals(expected, buffer);

                assertEquals(-1, wrapper.read(buffer, 0, buffer.length));
            }

            verify(inputStream).read(buffer, 0, 5);
            verify(inputStream).read(buffer, 0, 3);
            verify(inputStream, times(2)).read(buffer, 0, buffer.length);
            verify(inputStream).close();
            verifyNoMoreInteractions(inputStream, servletInputStream);
        }

        @Test
        @DisplayName("delegates skip(long)")
        @SuppressWarnings("resource")
        void testDelegatesSkip() throws IOException {
            byte[] data = "hello world".getBytes(StandardCharsets.UTF_8);

            InputStream inputStream = spy(new ByteArrayInputStream(data));
            ServletInputStream servletInputStream = mock(ServletInputStream.class);

            try (ServletInputStream wrapper = new ServletInputStreamWrapper(inputStream, servletInputStream)) {
                assertEquals(3, wrapper.skip(3));
                assertEquals(data.length - 3, wrapper.skip(data.length));
                assertEquals(0, wrapper.skip(data.length));

                assertEquals(-1, wrapper.read());
            }

            verify(inputStream).skip(3);
            verify(inputStream, times(2)).skip(data.length);
            verify(inputStream).read();
            verify(inputStream).close();
            verifyNoMoreInteractions(inputStream, servletInputStream);
        }

        @Test
        @DisplayName("delegates available()")
        @SuppressWarnings("resource")
        void testDelegatesAvailable() throws IOException {
            byte[] data = "hello world".getBytes(StandardCharsets.UTF_8);

            InputStream inputStream = spy(new ByteArrayInputStream(data));
            ServletInputStream servletInputStream = mock(ServletInputStream.class);

            try (ServletInputStream wrapper = new ServletInputStreamWrapper(inputStream, servletInputStream)) {
                assertEquals(data.length, wrapper.available());
                wrapper.skip(3);
                assertEquals(data.length - 3, wrapper.available());
                wrapper.skip(data.length);
                assertEquals(0, wrapper.available());
            }

            verify(inputStream, times(3)).available();
            verify(inputStream).skip(3);
            verify(inputStream).skip(data.length);
            verify(inputStream).close();
            verifyNoMoreInteractions(inputStream, servletInputStream);
        }

        // No need to test delegation to close, that's already been done in other tests

        @Test
        @DisplayName("delegates markSupported(), mark(int) and reset()")
        @SuppressWarnings("resource")
        void testDelegatesMarkSupportedAndMarkAndReset() throws IOException {
            byte[] data = "hello world".getBytes(StandardCharsets.UTF_8);

            InputStream inputStream = spy(new ByteArrayInputStream(data));
            ServletInputStream servletInputStream = mock(ServletInputStream.class);

            byte[] buffer = new byte[data.length];

            try (ServletInputStream wrapper = new ServletInputStreamWrapper(inputStream, servletInputStream)) {
                assertTrue(wrapper.markSupported());

                wrapper.mark(buffer.length);

                assertEquals(buffer.length, wrapper.read(buffer, 0, buffer.length));
                assertArrayEquals(data, buffer);

                assertEquals(-1, wrapper.read());

                wrapper.reset();

                assertEquals(buffer.length, wrapper.read(buffer, 0, buffer.length));
                assertArrayEquals(data, buffer);

                assertEquals(-1, wrapper.read());
            }

            verify(inputStream).markSupported();
            verify(inputStream).mark(buffer.length);
            verify(inputStream).reset();
            verify(inputStream, times(2)).read(buffer, 0, buffer.length);
            verify(inputStream, times(2)).read();
            verify(inputStream).close();
            verifyNoMoreInteractions(inputStream, servletInputStream);
        }

        @Test
        @DisplayName("delegates isFinished()")
        @SuppressWarnings("resource")
        void testDelegatesIsFinished() throws IOException {
            InputStream inputStream = mock(InputStream.class);
            ServletInputStream servletInputStream = mock(ServletInputStream.class);

            when(servletInputStream.isFinished()).thenReturn(true);

            try (ServletInputStream wrapper = new ServletInputStreamWrapper(inputStream, servletInputStream)) {
                assertTrue(wrapper.isFinished());
            }

            verify(inputStream).close();
            verify(servletInputStream).isFinished();
            verifyNoMoreInteractions(inputStream, servletInputStream);
        }

        @Test
        @DisplayName("delegates isReady()")
        @SuppressWarnings("resource")
        void testDelegatesIsReady() throws IOException {
            InputStream inputStream = mock(InputStream.class);
            ServletInputStream servletInputStream = mock(ServletInputStream.class);

            when(servletInputStream.isReady()).thenReturn(true);

            try (ServletInputStream wrapper = new ServletInputStreamWrapper(inputStream, servletInputStream)) {
                assertTrue(wrapper.isReady());
            }

            verify(inputStream).close();
            verify(servletInputStream).isReady();
            verifyNoMoreInteractions(inputStream, servletInputStream);
        }

        @Test
        @DisplayName("delegates setReadListener(ReadListener)")
        @SuppressWarnings("resource")
        void testDelegatesSetReadListener() throws IOException {
            InputStream inputStream = mock(InputStream.class);
            ServletInputStream servletInputStream = mock(ServletInputStream.class);

            ReadListener listener = mock(ReadListener.class);

            try (ServletInputStream wrapper = new ServletInputStreamWrapper(inputStream, servletInputStream)) {
                wrapper.setReadListener(listener);
            }

            verify(inputStream).close();
            verify(servletInputStream).setReadListener(listener);
            verifyNoMoreInteractions(inputStream, servletInputStream);
        }

        @Test
        @DisplayName("inherits proper readLine")
        @SuppressWarnings("resource")
        void testInheritsProperReadLine() throws IOException {
            byte[] data = "line1\nline2\n".getBytes(StandardCharsets.UTF_8);

            InputStream inputStream = new ByteArrayInputStream(data);
            ServletInputStream servletInputStream = mock(ServletInputStream.class);

            try (ServletInputStream wrapper = new ServletInputStreamWrapper(inputStream, servletInputStream)) {
                byte[] buffer = new byte[data.length];
                byte[] expected = new byte[data.length];

                assertEquals(6, wrapper.readLine(buffer, 0, buffer.length));
                System.arraycopy(data, 0, expected, 0, 6);
                assertArrayEquals(expected, buffer);

                assertEquals(6, wrapper.readLine(buffer, 0, buffer.length));
                System.arraycopy(data, 6, expected, 0, 6);
                assertArrayEquals(expected, buffer);

                assertEquals(-1, wrapper.readLine(buffer, 0, buffer.length));
            }
        }
    }

    @Nested
    class ServletOutputStreamWrapperTest {

        @Test
        @DisplayName("delegates write(int)")
        @SuppressWarnings("resource")
        void testWriteSingle() throws IOException {
            byte[] data = "1234567890".getBytes(StandardCharsets.UTF_8);

            ByteArrayOutputStream outputStream = spy(new ByteArrayOutputStream());
            ServletOutputStream servletOutputStream = mock(ServletOutputStream.class);

            try (ServletOutputStream wrapper = new ServletOutputStreamWrapper(outputStream, servletOutputStream)) {
                for (byte b : data) {
                    wrapper.write(b);
                }
            }

            assertArrayEquals(data, outputStream.toByteArray());

            for (byte b : data) {
                verify(outputStream).write(b);
            }
            verify(outputStream).close();
            verify(outputStream).toByteArray();
            verifyNoMoreInteractions(outputStream, servletOutputStream);
        }

        @Test
        @DisplayName("delegates write(byte[])")
        @SuppressWarnings("resource")
        void testWriteByteArray() throws IOException {
            byte[] data = "hello world".getBytes(StandardCharsets.UTF_8);

            ByteArrayOutputStream outputStream = spy(new ByteArrayOutputStream());
            ServletOutputStream servletOutputStream = mock(ServletOutputStream.class);

            try (ServletOutputStream wrapper = new ServletOutputStreamWrapper(outputStream, servletOutputStream)) {
                wrapper.write(data);
            }

            assertArrayEquals(data, outputStream.toByteArray());

            verify(outputStream).write(data);
            // write(data) possibly delegates to write(data, 0, data.length)
            verify(outputStream, atMost(1)).write(data, 0, data.length);
            verify(outputStream).close();
            verify(outputStream).toByteArray();
            verifyNoMoreInteractions(outputStream, servletOutputStream);
        }

        @Test
        @DisplayName("delegates write(byte[], int, int)")
        @SuppressWarnings("resource")
        void testWriteByteArrayPortion() throws IOException {
            byte[] data = "hello world".getBytes(StandardCharsets.UTF_8);

            ByteArrayOutputStream outputStream = spy(new ByteArrayOutputStream());
            ServletOutputStream servletOutputStream = mock(ServletOutputStream.class);

            try (ServletOutputStream wrapper = new ServletOutputStreamWrapper(outputStream, servletOutputStream)) {
                wrapper.write(data, 0, data.length - 3);
                wrapper.write(data, data.length - 3, 3);
            }

            assertArrayEquals(data, outputStream.toByteArray());

            verify(outputStream).write(data, 0, data.length - 3);
            verify(outputStream).write(data, data.length - 3, 3);
            verify(outputStream).close();
            verify(outputStream).toByteArray();
            verifyNoMoreInteractions(outputStream, servletOutputStream);
        }

        @Test
        @DisplayName("delegates flush()")
        @SuppressWarnings("resource")
        void testFlush() throws IOException {
            OutputStream outputStream = mock(OutputStream.class);
            ServletOutputStream servletOutputStream = mock(ServletOutputStream.class);

            try (ServletOutputStream wrapper = new ServletOutputStreamWrapper(outputStream, servletOutputStream)) {
                wrapper.flush();
            }

            verify(outputStream).flush();
            verify(outputStream).close();
            verifyNoMoreInteractions(outputStream, servletOutputStream);
        }

        // No need to test delegation to close, that's already been done in other tests

        @Test
        @DisplayName("delegates isReady()")
        @SuppressWarnings("resource")
        void testDelegatesIsReady() throws IOException {
            OutputStream outputStream = mock(OutputStream.class);
            ServletOutputStream servletOutputStream = mock(ServletOutputStream.class);

            when(servletOutputStream.isReady()).thenReturn(true);

            try (ServletOutputStream wrapper = new ServletOutputStreamWrapper(outputStream, servletOutputStream)) {
                assertTrue(wrapper.isReady());
            }

            verify(outputStream).close();
            verify(servletOutputStream).isReady();
            verifyNoMoreInteractions(outputStream, servletOutputStream);
        }

        @Test
        @DisplayName("delegates setWriteListener(WriteListener)")
        @SuppressWarnings("resource")
        void testDelegatesSetReadListener() throws IOException {
            OutputStream outputStream = mock(OutputStream.class);
            ServletOutputStream servletOutputStream = mock(ServletOutputStream.class);

            WriteListener listener = mock(WriteListener.class);

            try (ServletOutputStream wrapper = new ServletOutputStreamWrapper(outputStream, servletOutputStream)) {
                wrapper.setWriteListener(listener);
            }

            verify(outputStream).close();
            verify(servletOutputStream).setWriteListener(listener);
            verifyNoMoreInteractions(outputStream, servletOutputStream);
        }

        @Test
        @DisplayName("inherits proper print and println")
        @SuppressWarnings("resource")
        void testInheritsProperPrintAndPrintln() throws IOException {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ServletOutputStream servletOutputStream = mock(ServletOutputStream.class);

            ByteArrayOutputStream expected = new ByteArrayOutputStream();

            try (ServletOutputStream wrapper = new ServletOutputStreamWrapper(outputStream, servletOutputStream);
                    PrintStream printStream = new PrintStream(expected)) {

                // print(String), println(String)
                wrapper.print("hello world");
                printStream.print("hello world");

                wrapper.println("hello world");
                printStream.println("hello world");

                // print(boolean), println(boolean)
                wrapper.print(true);
                wrapper.print(false);
                printStream.print(true);
                printStream.print(false);

                wrapper.println(true);
                wrapper.println(false);
                printStream.println(true);
                printStream.println(false);

                // print(char), println(char)
                for (char c : "hello world".toCharArray()) {
                    wrapper.print(c);
                    printStream.print(c);

                    wrapper.println(c);
                    printStream.println(c);
                }

                // print(int), println(int)
                for (int i = 0; i < 10; i++) {
                    wrapper.print(i);
                    printStream.print(i);

                    wrapper.println(i);
                    printStream.println(i);
                }

                // print(long), println(long)
                for (int i = 0; i < 10; i++) {
                    long l = Long.MAX_VALUE - i;

                    wrapper.print(l);
                    printStream.print(l);

                    wrapper.println(l);
                    printStream.println(l);
                }

                // print(float), println(float)
                wrapper.print(1F / 3F);
                wrapper.print(Float.NaN);
                wrapper.print(Float.POSITIVE_INFINITY);
                printStream.print(1F / 3F);
                printStream.print(Float.NaN);
                printStream.print(Float.POSITIVE_INFINITY);

                wrapper.println(1F / 3F);
                wrapper.println(Float.NaN);
                wrapper.println(Float.POSITIVE_INFINITY);
                printStream.println(1F / 3F);
                printStream.println(Float.NaN);
                printStream.println(Float.POSITIVE_INFINITY);

                // print(double), println(double)
                wrapper.print(1D / 3D);
                wrapper.print(Double.NaN);
                wrapper.print(Double.POSITIVE_INFINITY);
                printStream.print(1D / 3D);
                printStream.print(Double.NaN);
                printStream.print(Double.POSITIVE_INFINITY);

                wrapper.println(1D / 3D);
                wrapper.println(Double.NaN);
                wrapper.println(Double.POSITIVE_INFINITY);
                printStream.println(1D / 3D);
                printStream.println(Double.NaN);
                printStream.println(Double.POSITIVE_INFINITY);
            }

            assertArrayEquals(expected.toByteArray(), outputStream.toByteArray());
        }
    }
}
