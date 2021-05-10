/*
 * ServletUtils.java
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Objects;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import com.github.robtimus.io.function.IOFunction;

/**
 * A utility class for servlets.
 *
 * @author Rob Spoor
 */
public final class ServletUtils {

    private ServletUtils() {
        throw new IllegalStateException("cannot create instances of " + getClass().getName()); //$NON-NLS-1$
    }

    /**
     * Transforms a {@link ServletInputStream} into a different {@link ServletInputStream}.
     * <p>
     * The given transformation does not necessarily need to return a {@link ServletInputStream}.
     * If it returns an {@link InputStream} that is not a {@link ServletInputStream}, it will be wrapped automatically in a new
     * {@link ServletInputStream}.
     *
     * @param input The {@link ServletInputStream} to transform.
     * @param transformation The transformation to apply.
     * @return A {@link ServletInputStream} that is the result of applying the given transformation to the given {@link ServletInputStream}.
     * @throws NullPointerException If the given {@link ServletInputStream} or transformation is {@code null}.
     * @throws IOException If an I/O error occurred during the transformation.
     */
    @SuppressWarnings("resource")
    public static ServletInputStream transform(ServletInputStream input,
            IOFunction<? super ServletInputStream, ? extends InputStream> transformation) throws IOException {

        Objects.requireNonNull(input);
        Objects.requireNonNull(transformation);

        InputStream transformed = transformation.apply(input);
        return transformed instanceof ServletInputStream
                ? (ServletInputStream) transformed
                : new ServletInputStreamWrapper(transformed, input);
    }

    static final class ServletInputStreamWrapper extends ServletInputStream {

        private final InputStream inputStream;
        private final ServletInputStream servletInputStream;

        ServletInputStreamWrapper(InputStream inputStream, ServletInputStream servletInputStream) {
            this.inputStream = Objects.requireNonNull(inputStream);
            this.servletInputStream = Objects.requireNonNull(servletInputStream);
        }

        @Override
        public int read() throws IOException {
            return inputStream.read();
        }

        @Override
        public int read(byte[] b) throws IOException {
            return inputStream.read(b);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return inputStream.read(b, off, len);
        }

        @Override
        public long skip(long n) throws IOException {
            return inputStream.skip(n);
        }

        @Override
        public int available() throws IOException {
            return inputStream.available();
        }

        @Override
        public void close() throws IOException {
            inputStream.close();
        }

        @Override
        public synchronized void mark(int readlimit) {
            inputStream.mark(readlimit);
        }

        @Override
        public synchronized void reset() throws IOException {
            inputStream.reset();
        }

        @Override
        public boolean markSupported() {
            return inputStream.markSupported();
        }

        @Override
        public boolean isFinished() {
            return servletInputStream.isFinished();
        }

        @Override
        public boolean isReady() {
            return servletInputStream.isReady();
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            servletInputStream.setReadListener(readListener);
        }
    }

    /**
     * Transforms a {@link BufferedReader} into a different {@link BufferedReader}.
     * <p>
     * The given transformation does not necessarily need to return a {@link BufferedReader}.
     * If it returns a {@link Reader} that is not a {@link BufferedReader}, it will be wrapped automatically in a new {@link BufferedReader}.
     *
     * @param input The {@link BufferedReader} to transform.
     * @param transformation The transformation to apply.
     * @return A {@link BufferedReader} that is the result of applying the given transformation to the given {@link BufferedReader}.
     * @throws NullPointerException If the given {@link BufferedReader} or transformation is {@code null}.
     * @throws IOException If an I/O error occurred during the transformation.
     */
    @SuppressWarnings("resource")
    public static BufferedReader transform(BufferedReader input,
            IOFunction<? super BufferedReader, ? extends Reader> transformation) throws IOException {

        Objects.requireNonNull(input);
        Objects.requireNonNull(transformation);

        Reader transformed = transformation.apply(input);
        return transformed instanceof BufferedReader
                ? (BufferedReader) transformed
                : new BufferedReader(transformed);
    }

    /**
     * Transforms a {@link ServletOutputStream} into a different {@link ServletOutputStream}.
     * <p>
     * The given transformation does not necessarily need to return a {@link ServletOutputStream}.
     * If it returns an {@link OutputStream} that is not a {@link ServletOutputStream}, it will be wrapped automatically in a new
     * {@link ServletOutputStream}.
     *
     * @param output The {@link ServletOutputStream} to transform.
     * @param transformation The transformation to apply.
     * @return A {@link ServletOutputStream} that is the result of applying the given transformation to the given {@link ServletOutputStream}.
     * @throws NullPointerException If the given {@link ServletOutputStream} or transformation is {@code null}.
     * @throws IOException If an I/O error occurred during the transformation.
     */
    @SuppressWarnings("resource")
    public static ServletOutputStream transform(ServletOutputStream output,
            IOFunction<? super ServletOutputStream, ? extends OutputStream> transformation) throws IOException {

        Objects.requireNonNull(output);
        Objects.requireNonNull(transformation);

        OutputStream transformed = transformation.apply(output);
        return transformed instanceof ServletOutputStream
                ? (ServletOutputStream) transformed
                : new ServletOutputStreamWrapper(transformed, output);
    }

    static final class ServletOutputStreamWrapper extends ServletOutputStream {

        private final OutputStream outputStream;
        private final ServletOutputStream servletOutputStream;

        ServletOutputStreamWrapper(OutputStream outputStream, ServletOutputStream servletOutputStream) {
            this.outputStream = Objects.requireNonNull(outputStream);
            this.servletOutputStream = Objects.requireNonNull(servletOutputStream);
        }

        @Override
        public void write(int b) throws IOException {
            outputStream.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            outputStream.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            outputStream.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            outputStream.flush();
        }

        @Override
        public void close() throws IOException {
            outputStream.close();
        }

        @Override
        public boolean isReady() {
            return servletOutputStream.isReady();
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
            servletOutputStream.setWriteListener(writeListener);
        }
    }

    /**
     * Transforms a {@link PrintWriter} into a different {@link PrintWriter}.
     * <p>
     * The given transformation does not necessarily need to return a {@link PrintWriter}.
     * If it returns a {@link Writer} that is not a {@link PrintWriter}, it will be wrapped automatically in a new {@link PrintWriter}.
     *
     * @param output The {@link PrintWriter} to transform.
     * @param transformation The transformation to apply.
     * @return A {@link PrintWriter} that is the result of applying the given transformation to the given {@link PrintWriter}.
     * @throws NullPointerException If the given {@link PrintWriter} or transformation is {@code null}.
     * @throws IOException If an I/O error occurred during the transformation.
     */
    @SuppressWarnings("resource")
    public static PrintWriter transform(PrintWriter output, IOFunction<? super PrintWriter, ? extends Writer> transformation) throws IOException {
        Objects.requireNonNull(output);
        Objects.requireNonNull(transformation);

        Writer transformed = transformation.apply(output);
        return transformed instanceof PrintWriter
                ? (PrintWriter) transformed
                : new PrintWriter(transformed);
    }
}
