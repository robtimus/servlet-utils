/*
 * InputTransformingHttpServletRequestWrapper.java
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import com.github.robtimus.servlet.ServletUtils;

/**
 * A sub class of {@link HttpServletRequestWrapper} that applies a transformation on its {@link ServletRequest#getInputStream() input stream} and/or
 * {@link ServletRequest#getReader() reader}.
 *
 * @author Rob Spoor
 */
public class InputTransformingHttpServletRequestWrapper extends HttpServletRequestWrapper {

    private ServletInputStream originalInputStream;
    private ServletInputStream transformedInputStream;

    private BufferedReader originalReader;
    private BufferedReader transformedReader;

    /**
     * Creates a new {@link HttpServletRequest} wrapper.
     *
     * @param request The request to wrap.
     * @throws IllegalArgumentException If the request is {@code null}.
     */
    public InputTransformingHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    /**
     * Transforms the original {@link ServletInputStream}.
     * <p>
     * This default implementation returns the unmodified argument.
     *
     * @param inputStream The original {@link ServletInputStream} to transform.
     * @return An {@link InputStream} that is the result of transforming the given {@link ServletInputStream}.
     *         It does not necessary have to be a {@link ServletInputStream}.
     * @throws IOException If an I/O error occurred during the transformation.
     */
    protected InputStream transform(ServletInputStream inputStream) throws IOException {
        return inputStream;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        ServletInputStream inputStream = super.getInputStream();
        if (inputStream != originalInputStream) {
            transformedInputStream = ServletUtils.transform(inputStream, this::transform);
            originalInputStream = inputStream;
        }
        return transformedInputStream;
    }

    /**
     * Transforms the original {@link BufferedReader}.
     * <p>
     * This default implementation returns the unmodified argument.
     *
     * @param reader The original {@link BufferedReader} to transform.
     * @return A {@link Reader} that is the result of transforming the given {@link BufferedReader}.
     *         It does not necessary have to be a {@link BufferedReader}.
     * @throws IOException If an I/O error occurred during the transformation.
     */
    protected Reader transform(BufferedReader reader) throws IOException {
        return reader;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        BufferedReader reader = super.getReader();
        if (reader != originalReader) {
            transformedReader = ServletUtils.transform(reader, this::transform);
            originalReader = reader;
        }
        return transformedReader;
    }
}
