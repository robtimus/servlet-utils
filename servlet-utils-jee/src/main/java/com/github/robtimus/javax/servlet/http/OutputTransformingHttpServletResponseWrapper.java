/*
 * OutputTransformingHttpServletResponseWrapper.java
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

package com.github.robtimus.javax.servlet.http;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import com.github.robtimus.javax.servlet.ServletUtils;

/**
 * A sub class of {@link HttpServletResponseWrapper} that applies a transformation on its {@link ServletResponse#getOutputStream() output stream}
 * and/or {@link ServletResponse#getWriter() writer}.
 *
 * @author Rob Spoor
 */
public class OutputTransformingHttpServletResponseWrapper extends HttpServletResponseWrapper {

    private ServletOutputStream originalOutputStream;
    private ServletOutputStream transformedOutputStream;

    private PrintWriter originalWriter;
    private PrintWriter transformedWriter;

    /**
     * Creates a new {@link HttpServletResponse} wrapper.
     *
     * @param response The response to wrap.
     * @throws IllegalArgumentException If the response is {@code null}.
     */
    public OutputTransformingHttpServletResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    /**
     * Transforms the original {@link ServletOutputStream}.
     * <p>
     * This default implementation returns the unmodified argument.
     *
     * @param outputStream The original {@link ServletOutputStream} to transform.
     * @return An {@link OutputStream} that is the result of transforming the given {@link ServletOutputStream}.
     *         It does not necessary have to be a {@link ServletOutputStream}.
     * @throws IOException If an I/O error occurred during the transformation.
     */
    protected OutputStream transform(ServletOutputStream outputStream) throws IOException {
        return outputStream;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        ServletOutputStream outputStream = super.getOutputStream();
        if (outputStream != originalOutputStream) {
            transformedOutputStream = ServletUtils.transform(outputStream, this::transform);
            originalOutputStream = outputStream;
        }
        return transformedOutputStream;
    }

    /**
     * Transforms the original {@link PrintWriter}.
     * <p>
     * This default implementation returns the unmodified argument.
     *
     * @param writer The original {@link PrintWriter} to transform.
     * @return A {@link Writer} that is the result of transforming the given {@link PrintWriter}.
     *         It does not necessary have to be a {@link PrintWriter}.
     * @throws IOException If an I/O error occurred during the transformation.
     */
    protected Writer transform(PrintWriter writer) throws IOException {
        return writer;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        PrintWriter writer = super.getWriter();
        if (writer != originalWriter) {
            transformedWriter = ServletUtils.transform(writer, this::transform);
            originalWriter = writer;
        }
        return transformedWriter;
    }

    @Override
    public void resetBuffer() {
        super.resetBuffer();
        // When the buffer is reset, the contents of the output stream or writer are cleared.
        // Reset the original and reset streams, so a new transformed output stream / writer will be used to write content from scratch.
        clear();
    }

    @Override
    public void reset() {
        super.reset();
        // When the response is reset, the contents of the output stream or writer are cleared.
        // Reset the original and reset streams, so a new transformed output stream / writer will be used to write content from scratch.
        clear();
    }

    private void clear() {
        originalOutputStream = null;
        transformedOutputStream = null;

        originalWriter = null;
        transformedWriter = null;
    }
}
