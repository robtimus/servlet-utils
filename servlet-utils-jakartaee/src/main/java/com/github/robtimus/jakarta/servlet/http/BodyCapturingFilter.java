/*
 * BodyCapturingFilter.java
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

package com.github.robtimus.jakarta.servlet.http;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import com.github.robtimus.jakarta.servlet.AsyncUtils;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

/**
 * A filter that captures request and response bodies. When the request body is fully read, {@link #bodyRead(BodyCapturingRequest)} is called.
 * When the response is complete, {@link #bodyProduced(BodyCapturingResponse, HttpServletRequest)} is called. Both methods can be overridden to
 * perform the necessary logic, for example logging the request and/or response.
 * <p>
 * This filter supports the following initialization parameters:
 * <blockquote>
 * <table class="striped">
 *   <caption style="display:none">Supported initialization parameters</caption>
 *   <thead>
 *     <tr>
 *       <th>Name</th>
 *       <th>Type</th>
 *       <th>Description</th>
 *       <th>Default</th>
 *     </tr>
 *   </thead>
 *   <tbody>
 *     <tr>
 *       <td>initialRequestCapacity</td>
 *       <td>int</td>
 *       <td>The initial capacity for the buffer used for capturing a request's body; not negative.</td>
 *       <td>{@code 32}</td>
 *     </tr>
 *     <tr>
 *       <td>initialRequestCapacityFromContentLength</td>
 *       <td>boolean</td>
 *       <td>{@code true} to use the request's content length for the initial capacity.
 *           If {@code true}, the {@code initialRequestCapacity} initialization parameter is ignored.</td>
 *       <td>{@code false}</td>
 *     </tr>
 *     <tr>
 *       <td>requestLimit</td>
 *       <td>int</td>
 *       <td>The limit for the number of bytes or characters of a request's body to capture; not negative.</td>
 *       <td>{@link Integer#MAX_VALUE}</td>
 *     </tr>
 *     <tr>
 *       <td>considerRequestReadAfterContentLength</td>
 *       <td>boolean</td>
 *       <td>{@code true} to consider the request body as fully read once the number of bytes or characters specified in the {@code Content-Length}
 *           header has been reached.
 *           <p>
 *           Some frameworks don't fully consume all content. Instead they stop at a specific point. For instance, some JSON parsers stop reading as
 *           soon as the root object's closing closing curly brace is encountered. Ideally such a framework is configured to consume all content.
 *           This parameter can be set to {@code true} as fallback if that's not possible.</td>
 *       <td>{@code false}</td>
 *     </tr>
 *     <tr>
 *       <td>ensureRequestBodyConsumed</td>
 *       <td>boolean</td>
 *       <td>{@code true} to ensure that the request body is consumed.
 *           <p>
 *           If a downstream filter or servlet fails to consume the request's body completely, {@link #bodyRead(BodyCapturingRequest)} may never be
 *           called. This parameter can be set to {@code true} to ensure the method will be called.</td>
 *       <td>{@code false}</td>
 *     </tr>
 *     <tr>
 *       <td>initialResponseCapacity</td>
 *       <td>int</td>
 *       <td>The initial capacity for the buffer used for capturing a response's body; not negative.</td>
 *       <td>{@code 32}</td>
 *     </tr>
 *     <tr>
 *       <td>responseLimit</td>
 *       <td>int</td>
 *       <td>The limit for the number of bytes or characters of a response's body to capture; not negative.</td>
 *       <td>{@link Integer#MAX_VALUE}</td>
 *     </tr>
 *   </tbody>
 * </table>
 * </blockquote>
 * With these settings, there are two cases where the request body is not read, but {@link #bodyRead(BodyCapturingRequest)} is called immediately:
 * <ul>
 * <li>The {@code considerRequestReadAfterContentLength} initialization parameter is set to {@code true} and the request as a {@code 0} content
 *     length.</li>
 * <li>{@link #hasNoBody(String)} returns {@code true} for the request's method. By default this is the case for {@code GET}, {@code DELETE},
 *     {@code OPTIONS} and {@code HEAD}.</li>
 * </ul>
 *
 * @author Rob Spoor
 */
public abstract class BodyCapturingFilter implements Filter {

    static final String INITIAL_REQUEST_CAPACITY = "initialRequestCapacity"; //$NON-NLS-1$
    static final String INITIAL_REQUEST_CAPACITY_FROM_CONTENT_LENGTH = "initialRequestCapacityFromContentLength"; //$NON-NLS-1$
    static final String REQUEST_LIMIT = "requestLimit"; //$NON-NLS-1$
    static final String CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH = "considerRequestReadAfterContentLength"; //$NON-NLS-1$
    static final String ENSURE_REQUEST_BODY_CONSUMED = "ensureRequestBodyConsumed"; //$NON-NLS-1$

    static final String INITIAL_RESPONSE_CAPACITY = "initialResponseCapacity"; //$NON-NLS-1$
    static final String RESPONSE_LIMIT = "responseLimit"; //$NON-NLS-1$

    // Value taken from ByteArrayInputStream's default capacity
    static final int DEFAULT_INITIAL_CAPACITY = 32;

    @SuppressWarnings("nls")
    private static final Set<String> METHODS_WITHOUT_BODY = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("GET", "DELETE", "OPTIONS", "HEAD")));

    private FilterConfig filterConfig;

    private int initialRequestCapacity;
    private boolean initialRequestCapacityFromContentLength;
    private int requestLimit;
    private boolean considerRequestReadAfterContentLength;
    private boolean ensureRequestBodyConsumed;

    private int initialResponseCapacity;
    private int responseLimit;

    @Override
    public void init(FilterConfig filterConfig) {
        this.filterConfig = filterConfig;

        initialRequestCapacity = readIntParameter(INITIAL_REQUEST_CAPACITY, DEFAULT_INITIAL_CAPACITY);
        initialRequestCapacityFromContentLength = readBooleanParameter(INITIAL_REQUEST_CAPACITY_FROM_CONTENT_LENGTH, false);
        requestLimit = readIntParameter(REQUEST_LIMIT, Integer.MAX_VALUE);
        considerRequestReadAfterContentLength = readBooleanParameter(CONSIDER_REQUEST_READ_AFTER_CONTENT_LENGTH, false);
        ensureRequestBodyConsumed = readBooleanParameter(ENSURE_REQUEST_BODY_CONSUMED, false);

        initialResponseCapacity = readIntParameter(INITIAL_RESPONSE_CAPACITY, DEFAULT_INITIAL_CAPACITY);
        responseLimit = readIntParameter(RESPONSE_LIMIT, Integer.MAX_VALUE);
    }

    private int readIntParameter(String name, int defaultValue) {
        String rawValue = filterConfig.getInitParameter(name);
        if (rawValue == null) {
            return defaultValue;
        }
        int value = Integer.parseInt(rawValue);
        if (value < 0) {
            throw new IllegalArgumentException(Messages.BodyCapturingFilter.invalidIntParameter.get(name, rawValue));
        }
        return value;
    }

    private boolean readBooleanParameter(String name, boolean defaultValue) {
        String rawValue = filterConfig.getInitParameter(name);
        if (rawValue == null) {
            return defaultValue;
        }
        switch (rawValue) {
        case "true": //$NON-NLS-1$
            return true;
        case "false": //$NON-NLS-1$
            return false;
        default:
            throw new IllegalArgumentException(Messages.BodyCapturingFilter.invalidBooleanParameter.get(name, rawValue));
        }
    }

    int initialRequestCapacity() {
        return initialRequestCapacity;
    }

    boolean initialRequestCapacityFromContentLength() {
        return initialRequestCapacityFromContentLength;
    }

    int requestLimit() {
        return requestLimit;
    }

    boolean considerRequestReadAfterContentLength() {
        return considerRequestReadAfterContentLength;
    }

    boolean ensureRequestBodyConsumed() {
        return ensureRequestBodyConsumed;
    }

    int initialResponseCapacity() {
        return initialResponseCapacity;
    }

    int responseLimit() {
        return responseLimit;
    }

    @Override
    public void destroy() {
        filterConfig = null;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        BodyCapturingRequest bodyCapturingRequest;
        if ((considerRequestReadAfterContentLength && request.getContentLengthLong() == 0) || hasNoBody(httpRequest.getMethod())) {
            bodyCapturingRequest = new BodyCapturingRequest(httpRequest, false);
            bodyRead(bodyCapturingRequest);
        } else {
            bodyCapturingRequest = new BodyCapturingRequest(httpRequest, true);
        }
        BodyCapturingResponse bodyCapturingResponse = new BodyCapturingResponse(httpRequest, httpResponse);

        AsyncUtils.doFilter(bodyCapturingRequest, bodyCapturingResponse, chain, (req, resp) -> {
            if (ensureRequestBodyConsumed) {
                bodyCapturingRequest.consume(true);
            }
            bodyProduced(bodyCapturingResponse, httpRequest);
        });
    }

    /**
     * Returns whether or not an HTTP method has no body. This will prevent (unnecessary) capturing of the body completely.
     * <p>
     * This implementation returns whether or not the method is one of {@code GET}, {@code DELETE}, {@code OPTIONS} or {@code HEAD}.
     *
     * @param method The method to check.
     * @return {@code true} if the HTTP method has no body, or {@code false} otherwise.
     */
    protected boolean hasNoBody(String method) {
        return method != null && METHODS_WITHOUT_BODY.contains(method.toUpperCase());
    }

    /**
     * Ensures that the body of a request body is consumed. This can be used in downstream filters or servlets to ensure that
     * {@link #bodyRead(BodyCapturingRequest)} will be called.
     * <p>
     * This method is shorthand for {@link #ensureBodyConsumed(HttpServletRequest, boolean) ensureBodyConsumed(request, true)}.
     *
     * @param request The request for which to ensure the request body is consumed.
     * @throws IOException If an I/O error occurs.
     */
    public static void ensureBodyConsumed(HttpServletRequest request) throws IOException {
        ensureBodyConsumed(request, true);
    }

    /**
     * Ensures that the body of a request body is consumed. This can be used in downstream filters or servlets to ensure that
     * {@link #bodyRead(BodyCapturingRequest)} will be called.
     *
     * @param request The request for which to ensure the request body is consumed.
     * @param preferReader {@code true} to prefer using {@link ServletRequest#getReader()} over {@link ServletRequest#getInputStream()}.
     *                         Although this method works both when {@link ServletRequest#getReader()} or {@link ServletRequest#getInputStream()} has
     *                         been called, it may need to do so using a try-catch statement. This flag can be used to increase performance in two
     *                         ways:
     *                         <ul>
     *                         <li>If it's known whether {@link ServletRequest#getReader()} or {@link ServletRequest#getInputStream()} is called, the
     *                             {@link IllegalStateException} thrown by the other can be prevented.</li>
     *                         <li>If the captured body is needed as a string, {@link BodyCapturingRequest#capturedTextBody()} is more efficient than
     *                             {@link BodyCapturingRequest#capturedBinaryBodyAsString()}.</li>
     *                         </ul>
     * @throws IOException If an I/O error occurs.
     */
    public static void ensureBodyConsumed(HttpServletRequest request, boolean preferReader) throws IOException {
        ServletRequest current = request;
        while (current instanceof HttpServletRequestWrapper && !(current instanceof BodyCapturingRequest)) {
            current = ((HttpServletRequestWrapper) current).getRequest();
        }
        if (current instanceof BodyCapturingRequest) {
            BodyCapturingRequest bodyCapturingRequest = (BodyCapturingRequest) current;
            bodyCapturingRequest.consume(preferReader);
        } else {
            consumeBody(request, preferReader);
        }
    }

    @SuppressWarnings("resource")
    private static void consumeBody(HttpServletRequest request, boolean preferReader) throws IOException {
        if (preferReader) {
            try {
                consume(request.getReader());
            } catch (@SuppressWarnings("unused") IllegalStateException e) {
                consume(request.getInputStream());
            }
        } else {
            try {
                consume(request.getInputStream());
            } catch (@SuppressWarnings("unused") IllegalStateException e) {
                consume(request.getReader());
            }
        }
    }

    private static void consume(InputStream inputStream) throws IOException {
        byte[] buffer = new byte[1024];
        while (inputStream.read(buffer) != -1) {
            // discard contents
        }
    }

    private static void consume(Reader reader) throws IOException {
        char[] buffer = new char[1024];
        while (reader.read(buffer) != -1) {
            // discard contents
        }
    }

    /**
     * Called when the capture limit for a request's body is reached.
     * This method will be called at most once for each request.
     *
     * @param request The request for which the capture limit is reached.
     */
    protected void limitReached(BodyCapturingRequest request) {
        // does nothing
    }

    /**
     * Called when a request's body has been read. This will be the case either if the request's body has been fully
     * {@link BodyCapturingRequest#bodyIsConsumed() consumed}, or if its {@link ServletRequest#getInputStream() input stream} or
     * {@link ServletRequest#getReader() reader} is closed.
     * This method will be called at most once for each request. If all downstream filters and servlets fail to (completely) consume the request,
     * this method will only be called if initialization parameter {@code ensureRequestBodyConsumed} is set to {@code true}.
     *
     * @param request The request for which the body has been read.
     */
    protected void bodyRead(BodyCapturingRequest request) {
        // does nothing
    }

    /**
     * Called when the capture limit for a response's body is reached.
     * This method will be called at most once for each response, unless {@link ServletResponse#reset()} or {@link ServletResponse#resetBuffer()} is
     * called on the response.
     *
     * @param response The response for which the capture limit is reached.
     * @param request The request that lead to the response. This is provided to provide access to any request attributes.
     */
    protected void limitReached(BodyCapturingResponse response, HttpServletRequest request) {
        // does nothing
    }

    /**
     * Called when a response's body has been produced.
     * This method will be called exactly once for each response.
     *
     * @param response The response for which the body has been produced.
     * @param request The request that lead to the response. This is provided to provide access to any request attributes.
     */
    protected void bodyProduced(BodyCapturingResponse response, HttpServletRequest request) {
        // does nothing
    }

    /**
     * Returns the initial capacity for the buffer used for capturing a request's body.
     * This implementation will return the given request's content length if the {@code initialRequestCapacityFromContentLength} initialization
     * parameter is set to {@code true} and the request has a content length defined. Otherwise it will return the value of the
     * {@code initialRequestCapacity} initialization parameter. If that's not given, {@code 32} will be used.
     * <p>
     * This method can be overridden to return something else depending on the request, for example based on the content type.
     *
     * @param request The request for which to return the initial buffer capacity.
     * @return The initial buffer capacity for the given request.
     */
    protected int initialRequestCapacity(HttpServletRequest request) {
        if (initialRequestCapacityFromContentLength) {
            long contentLength = request.getContentLengthLong();
            if (contentLength != -1) {
                return (int) Math.min(contentLength, Integer.MAX_VALUE);
            }
        }
        return initialRequestCapacity;
    }

    /**
     * Returns the limit for the number of bytes or characters of a request's body to capture.
     * This implementation will return the value of the {@code requestLimit} initialization parameter.
     * <p>
     * This method can be overridden to return something else depending on the request, for example based on the content type.
     *
     * @param request The request for which to return the capture limit.
     * @return The capture limit for the given request.
     */
    protected int requestLimit(HttpServletRequest request) {
        return requestLimit;
    }

    /**
     * Returns the initial capacity for the buffer used for capturing a response's body.
     * This implementation will return the value of the {@code initialResponseCapacity} initialization parameter. If that's not given, {@code 32} will
     * be used.
     * <p>
     * This method can be overridden to return something else depending on the request, for example based on the path. Since this method is called
     * before the response is populated, the response cannot be used and is therefore not provided.
     *
     * @param request The request that leads to the response for which to return the initial buffer capacity.
     * @return The initial buffer capacity for the response to the given request.
     */
    protected int initialResponseCapacity(HttpServletRequest request) {
        return initialResponseCapacity;
    }

    /**
     * Returns the limit for the number of bytes or characters of a response's body to capture.
     * This implementation will return the value of the {@code responseLimit} initialization parameter.
     * <p>
     * This method can be overridden to return something else depending on the request, for example based on the path. Since this method is called
     * before the response is populated, the response cannot be used and is therefore not provided.
     *
     * @param request The request that leads to the response for which to return the capture limit.
     * @return The capture limit for the response to the given request.
     */
    protected int responseLimit(HttpServletRequest request) {
        return responseLimit;
    }

    /**
     * The possible modes for capturing request and response bodies.
     *
     * @author Rob Spoor
     */
    protected enum CaptureMode {
        /** Indicates a request or response body is captured as bytes. */
        BYTES,

        /** Indicates a request or response body is captured as text. */
        TEXT,

        /** Indicates no request or response body has been captured. */
        NONE,
    }

    /**
     * An {@link HttpServletRequest} wrapper that captures the request's body as it's read.
     *
     * @author Rob Spoor
     */
    protected final class BodyCapturingRequest extends InputTransformingHttpServletRequestWrapper {

        private BodyCapturingInputStream bodyCapturingInputStream;
        private BodyCapturingReader bodyCapturingReader;

        private final int initialCapacity;
        private final int limit;
        private final long doneAfter;

        private final Runnable doneCallback;
        private final Runnable limitReachedCallback;

        BodyCapturingRequest(HttpServletRequest request, boolean useDoneCallback) {
            super(request);

            initialCapacity = initialRequestCapacity(request);
            limit = requestLimit(request);
            doneAfter = doneAfter(request);

            doneCallback = useDoneCallback
                    ? () -> bodyRead(this)
                    : null;
            limitReachedCallback = () -> limitReached(this);
        }

        private long doneAfter(HttpServletRequest request) {
            if (considerRequestReadAfterContentLength) {
                long contentLength = request.getContentLengthLong();
                if (contentLength != -1) {
                    return contentLength;
                }
            }
            return Long.MAX_VALUE;
        }

        @Override
        protected InputStream transform(ServletInputStream inputStream) throws IOException {
            bodyCapturingInputStream = new BodyCapturingInputStream(inputStream, initialCapacity, limit, doneAfter,
                    doneCallback, limitReachedCallback);
            return bodyCapturingInputStream;
        }

        @Override
        protected Reader transform(BufferedReader reader) throws IOException {
            bodyCapturingReader = new BodyCapturingReader(reader, initialCapacity, limit, doneAfter, doneCallback, limitReachedCallback);
            return bodyCapturingReader;
        }

        /**
         * Returns the capture mode.
         *
         * @return The capture mode.
         */
        public CaptureMode captureMode() {
            if (bodyCapturingInputStream != null) {
                return CaptureMode.BYTES;
            }
            if (bodyCapturingReader != null) {
                return CaptureMode.TEXT;
            }
            return CaptureMode.NONE;
        }

        /**
         * Returns the captured binary body.
         *
         * @return A byte array with the captured binary body.
         * @throws IllegalStateException If the body has not been captured as bytes.
         * @see #captureMode()
         */
        public byte[] capturedBinaryBody() {
            if (bodyCapturingInputStream != null) {
                return bodyCapturingInputStream.captured();
            }
            throw new IllegalStateException(Messages.BodyCapturingFilter.noBytesCaptured.get());
        }

        /**
         * Returns the captured binary body as a string.
         * This method will use the request's {@link ServletRequest#getCharacterEncoding() character encoding} if available;
         * otherwise the {@link ServletContext}'s {@link ServletContext#getRequestCharacterEncoding() request character encoding} if available;
         * otherwise {@link StandardCharsets#UTF_8}.
         *
         * @return The captured binary body as a string.
         * @throws IllegalStateException If the body has not been captured as bytes.
         * @see #captureMode()
         */
        public String capturedBinaryBodyAsString() {
            if (bodyCapturingInputStream != null) {
                String encoding = getCharacterEncoding();
                if (encoding == null) {
                    encoding = filterConfig.getServletContext().getRequestCharacterEncoding();
                }
                Charset charset = encoding != null ? Charset.forName(encoding) : StandardCharsets.UTF_8;
                return bodyCapturingInputStream.captured(charset);
            }
            throw new IllegalStateException(Messages.BodyCapturingFilter.noBytesCaptured.get());
        }

        /**
         * Returns the captured text body.
         *
         * @return The captured text body.
         * @throws IllegalStateException If the body has not been captured as text.
         * @see #captureMode()
         */
        public String capturedTextBody() {
            if (bodyCapturingReader != null) {
                return bodyCapturingReader.captured();
            }
            throw new IllegalStateException(Messages.BodyCapturingFilter.noTextCaptured.get());
        }

        /**
         * Returns the total body size, as far as the body has already been read. This can be a number of bytes or characters, depending on the
         * {@link #captureMode() capture mode}.
         *
         * @return The total body size.
         */
        public long totalBodySize() {
            if (bodyCapturingInputStream != null) {
                return bodyCapturingInputStream.totalBytes();
            }
            if (bodyCapturingReader != null) {
                return bodyCapturingReader.totalChars();
            }
            return 0;
        }

        /**
         * Returns whether or not the request body has been fully consumed.
         * This will be the case if one of the {@code read} methods of either the {@link ServletRequest#getInputStream() input stream} or the
         * {@link ServletRequest#getReader() reader} returns {@code -1}.
         *
         * @return {@code true} if the request body has been fully consumed, or {@code false} otherwise.
         */
        public boolean bodyIsConsumed() {
            if (bodyCapturingInputStream != null) {
                return bodyCapturingInputStream.isConsumed();
            }
            if (bodyCapturingReader != null) {
                return bodyCapturingReader.isConsumed();
            }
            return false;
        }

        @SuppressWarnings("resource")
        private void consume(boolean preferReader) throws IOException {
            if (bodyCapturingInputStream != null) {
                bodyCapturingInputStream.consume();
            } else if (bodyCapturingReader != null) {
                bodyCapturingReader.consume();
            } else if (preferReader) {
                // getReader() initializes bodyCapturingReader
                getReader();
                bodyCapturingReader.consume();
            } else {
                // getInputStream() initializes bodyCapturingInputStream
                getInputStream();
                bodyCapturingInputStream.consume();
            }
        }
    }

    /**
     * An {@link HttpServletResponse} wrapper that captures the response's body as it's written.
     *
     * @author Rob Spoor
     */
    protected final class BodyCapturingResponse extends OutputTransformingHttpServletResponseWrapper {

        private BodyCapturingOutputStream bodyCapturingOutputStream;
        private BodyCapturingWriter bodyCapturingWriter;

        private final int initialCapacity;
        private final int limit;

        private final Runnable limitReachedCallback;

        private BodyCapturingResponse(HttpServletRequest request, HttpServletResponse response) {
            super(response);

            initialCapacity = initialResponseCapacity(request);
            limit = responseLimit(request);

            limitReachedCallback = () -> limitReached(this, request);
        }

        @Override
        protected OutputStream transform(ServletOutputStream outputStream) throws IOException {
            bodyCapturingOutputStream = new BodyCapturingOutputStream(outputStream, initialCapacity, limit, limitReachedCallback);
            return bodyCapturingOutputStream;
        }

        @Override
        protected Writer transform(PrintWriter writer) throws IOException {
            bodyCapturingWriter = new BodyCapturingWriter(writer, initialCapacity, limit, limitReachedCallback);
            return bodyCapturingWriter;
        }

        @Override
        public void resetBuffer() {
            super.resetBuffer();
            bodyCapturingOutputStream = null;
            bodyCapturingWriter = null;
        }

        @Override
        public void reset() {
            super.reset();
            bodyCapturingOutputStream = null;
            bodyCapturingWriter = null;
        }

        /**
         * Returns the capture mode.
         *
         * @return The capture mode.
         */
        public CaptureMode captureMode() {
            if (bodyCapturingOutputStream != null) {
                return CaptureMode.BYTES;
            }
            if (bodyCapturingWriter != null) {
                return CaptureMode.TEXT;
            }
            return CaptureMode.NONE;
        }

        /**
         * Returns the captured binary body.
         *
         * @return A byte array with the captured binary body.
         * @throws IllegalStateException If the body has not been captured as bytes.
         * @see #captureMode()
         */
        public byte[] capturedBinaryBody() {
            if (bodyCapturingOutputStream != null) {
                return bodyCapturingOutputStream.captured();
            }
            throw new IllegalStateException(Messages.BodyCapturingFilter.noBytesCaptured.get());
        }

        /**
         * Returns the captured binary body as a string.
         * This method will use the request's {@link ServletRequest#getCharacterEncoding() character encoding} if available;
         * otherwise the {@link ServletContext}'s {@link ServletContext#getResponseCharacterEncoding() request character encoding} if available;
         * otherwise {@link StandardCharsets#UTF_8}.
         *
         * @return The captured binary body as a string.
         * @throws IllegalStateException If the body has not been captured as bytes.
         * @see #captureMode()
         */
        public String capturedBinaryBodyAsString() {
            if (bodyCapturingOutputStream != null) {
                String encoding = getCharacterEncoding();
                if (encoding == null) {
                    encoding = filterConfig.getServletContext().getResponseCharacterEncoding();
                }
                Charset charset = encoding != null ? Charset.forName(encoding) : StandardCharsets.UTF_8;
                return bodyCapturingOutputStream.captured(charset);
            }
            throw new IllegalStateException(Messages.BodyCapturingFilter.noBytesCaptured.get());
        }

        /**
         * Returns the captured text body.
         *
         * @return The captured text body.
         * @throws IllegalStateException If the body has not been captured as text.
         * @see #captureMode()
         */
        public String capturedTextBody() {
            if (bodyCapturingWriter != null) {
                return bodyCapturingWriter.captured();
            }
            throw new IllegalStateException(Messages.BodyCapturingFilter.noTextCaptured.get());
        }

        /**
         * Returns the total body size, as far as the body has already been read. This can be a number of bytes or characters, depending on the
         * {@link #captureMode() capture mode}.
         *
         * @return The total body size.
         */
        public long totalBodySize() {
            if (bodyCapturingOutputStream != null) {
                return bodyCapturingOutputStream.totalBytes();
            }
            if (bodyCapturingWriter != null) {
                return bodyCapturingWriter.totalChars();
            }
            return 0;
        }
    }

    static final class BodyCapturingInputStream extends InputStream {

        private final InputStream inputStream;

        private final ByteCaptor captor;
        private final int limit;
        private final long doneAfter;

        private long totalBytes = 0;
        private long mark = 0;

        private boolean consumed = false;

        private Consumer<BodyCapturingInputStream> doneCallback;
        private Consumer<BodyCapturingInputStream> limitReachedCallback;

        private BodyCapturingInputStream(InputStream inputStream, int initialCapacity, int limit, long doneAfter,
                Runnable doneCallback, Runnable limitReachedCallback) {

            this(inputStream, initialCapacity, limit, doneAfter, consumer(doneCallback), consumer(limitReachedCallback));
        }

        BodyCapturingInputStream(InputStream inputStream, int initialCapacity, int limit, long doneAfter,
                Consumer<BodyCapturingInputStream> doneCallback, Consumer<BodyCapturingInputStream> limitReachedCallback) {

            this.inputStream = inputStream;

            captor = new ByteCaptor(Math.min(initialCapacity, limit));
            this.limit = limit;
            this.doneAfter = doneAfter;

            this.doneCallback = doneCallback;
            this.limitReachedCallback = limitReachedCallback;
        }

        @Override
        public int read() throws IOException {
            int b = inputStream.read();
            if (b == -1) {
                onConsumed();
            } else {
                totalBytes++;
                if (captor.size() < limit) {
                    captor.write(b);
                    checkLimitReached();
                }
                checkDone();
            }
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int n = inputStream.read(b, off, len);
            if (n == -1) {
                onConsumed();
            } else {
                totalBytes += n;
                int allowed = Math.min(limit - captor.size(), n);
                if (allowed > 0) {
                    captor.write(b, off, allowed);
                    checkLimitReached();
                }
                checkDone();
            }
            return n;
        }

        // don't delegate skip, so it uses read and no content is lost

        @Override
        public void close() throws IOException {
            inputStream.close();
            onClosed();
        }

        @Override
        public synchronized void mark(int readlimit) {
            inputStream.mark(readlimit);
            mark = totalBytes;
        }

        @Override
        public synchronized void reset() throws IOException {
            inputStream.reset();
            captor.reset((int) Math.min(mark, limit));
            totalBytes = mark;
            consumed = false;
        }

        @Override
        public boolean markSupported() {
            return inputStream.markSupported();
        }

        private void onConsumed() {
            consumed = true;
            if (doneCallback != null) {
                doneCallback.accept(this);
                doneCallback = null;
            }
        }

        private void onClosed() {
            if (doneCallback != null) {
                doneCallback.accept(this);
                doneCallback = null;
            }
        }

        private void checkLimitReached() {
            if (totalBytes >= limit && limitReachedCallback != null) {
                limitReachedCallback.accept(this);
                limitReachedCallback = null;
            }
        }

        private void checkDone() {
            if (totalBytes >= doneAfter && doneCallback != null) {
                doneCallback.accept(this);
                doneCallback = null;
            }
        }

        byte[] captured() {
            return captor.captured();
        }

        String captured(Charset charset) {
            return captor.captured(charset);
        }

        long totalBytes() {
            return totalBytes;
        }

        boolean isConsumed() {
            return consumed;
        }

        private void consume() throws IOException {
            if (!consumed) {
                BodyCapturingFilter.consume(this);
            }
        }
    }

    static final class BodyCapturingReader extends Reader {

        private final Reader reader;

        private final StringBuilder captor;
        private final int limit;
        private final long doneAfter;

        private long totalChars = 0;
        private long mark = 0;

        private boolean consumed = false;

        private Consumer<BodyCapturingReader> doneCallback;
        private Consumer<BodyCapturingReader> limitReachedCallback;

        private BodyCapturingReader(Reader input, int initialCapacity, int limit, long doneAfter,
                Runnable doneCallback, Runnable limitReachedCallback) {

            this(input, initialCapacity, limit, doneAfter, consumer(doneCallback), consumer(limitReachedCallback));
        }

        BodyCapturingReader(Reader input, int initialCapacity, int limit, long doneAfter,
                Consumer<BodyCapturingReader> doneCallback, Consumer<BodyCapturingReader> limitReachedCallback) {

            reader = Objects.requireNonNull(input);

            captor = new StringBuilder(Math.min(initialCapacity, limit));
            this.limit = limit;
            this.doneAfter = doneAfter;

            this.doneCallback = doneCallback;
            this.limitReachedCallback = limitReachedCallback;
        }

        // don't delegate read(CharBuffer), the default implementation is good enough

        @Override
        public int read() throws IOException {
            int c = reader.read();
            if (c == -1) {
                onConsumed();
            } else {
                totalChars++;

                if (captor.length() < limit) {
                    captor.append((char) c);
                    checkLimitReached();
                }
                checkDone();
            }
            return c;
        }

        @Override
        public int read(char[] c, int off, int len) throws IOException {
            int n = reader.read(c, off, len);
            if (n == -1) {
                onConsumed();
            } else {
                totalChars += n;

                int allowed = Math.min(limit - captor.length(), n);
                if (allowed > 0) {
                    captor.append(c, off, allowed);
                    checkLimitReached();
                }
                checkDone();
            }
            return n;
        }

        // don't delegate skip, so it uses read and no content is lost

        @Override
        public void close() throws IOException {
            reader.close();
            onClosed();
        }

        @Override
        public void mark(int readlimit) throws IOException {
            reader.mark(readlimit);
            mark = totalChars;
        }

        @Override
        public void reset() throws IOException {
            reader.reset();
            captor.delete((int) Math.min(mark, limit), captor.length());
            totalChars = mark;
            consumed = false;
        }

        @Override
        public boolean markSupported() {
            return reader.markSupported();
        }

        private void onConsumed() {
            consumed = true;
            if (doneCallback != null) {
                doneCallback.accept(this);
                doneCallback = null;
            }
        }

        private void onClosed() {
            if (doneCallback != null) {
                doneCallback.accept(this);
                doneCallback = null;
            }
        }

        private void checkLimitReached() {
            if (totalChars >= limit && limitReachedCallback != null) {
                limitReachedCallback.accept(this);
                limitReachedCallback = null;
            }
        }

        private void checkDone() {
            if (totalChars >= doneAfter && doneCallback != null) {
                doneCallback.accept(this);
                doneCallback = null;
            }
        }

        String captured() {
            return captor.toString();
        }

        long totalChars() {
            return totalChars;
        }

        boolean isConsumed() {
            return consumed;
        }

        private void consume() throws IOException {
            if (!consumed) {
                BodyCapturingFilter.consume(this);
            }
        }
    }

    static final class BodyCapturingOutputStream extends OutputStream {

        private final OutputStream outputStream;

        private final ByteCaptor captor;
        private final int limit;

        private long totalBytes = 0;

        private Consumer<BodyCapturingOutputStream> limitReachedCallback;

        private BodyCapturingOutputStream(OutputStream outputStream, int initialCapacity, int limit, Runnable limitReachedCallback) {
            this(outputStream, initialCapacity, limit, consumer(limitReachedCallback));
        }

        BodyCapturingOutputStream(OutputStream outputStream, int initialCapacity, int limit,
                Consumer<BodyCapturingOutputStream> limitReachedCallback) {

            this.outputStream = outputStream;

            captor = new ByteCaptor(Math.min(initialCapacity, limit));
            this.limit = limit;

            this.limitReachedCallback = limitReachedCallback;
        }

        @Override
        public void write(int b) throws IOException {
            outputStream.write(b);

            totalBytes++;
            if (captor.size() < limit) {
                captor.write(b);
                checkLimitReached();
            }
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            outputStream.write(b, off, len);

            totalBytes += len;

            int allowed = Math.min(limit - captor.size(), len);
            if (allowed > 0) {
                captor.write(b, off, allowed);
                checkLimitReached();
            }
        }

        @Override
        public void flush() throws IOException {
            outputStream.flush();
        }

        @Override
        public void close() throws IOException {
            outputStream.close();
        }

        private void checkLimitReached() {
            if (totalBytes >= limit && limitReachedCallback != null) {
                limitReachedCallback.accept(this);
                limitReachedCallback = null;
            }
        }

        byte[] captured() {
            return captor.captured();
        }

        String captured(Charset charset) {
            return captor.captured(charset);
        }

        long totalBytes() {
            return totalBytes;
        }
    }

    static final class BodyCapturingWriter extends Writer {

        private final Writer writer;

        private final StringBuilder captor;
        private final int limit;

        private long totalChars = 0;

        private Consumer<BodyCapturingWriter> limitReachedCallback;

        private BodyCapturingWriter(Writer output, int initialCapacity, int limit, Runnable limitReachedCallback) {
            this(output, initialCapacity, limit, consumer(limitReachedCallback));
        }

        BodyCapturingWriter(Writer output, int initialCapacity, int limit, Consumer<BodyCapturingWriter> limitReachedCallback) {
            writer = Objects.requireNonNull(output);

            captor = new StringBuilder(Math.min(initialCapacity, limit));
            this.limit = limit;

            this.limitReachedCallback = limitReachedCallback;
        }

        @Override
        public void write(int c) throws IOException {
            writer.write(c);

            totalChars++;
            if (captor.length() < limit) {
                captor.append((char) c);
                checkLimitReached();
            }
        }

        @Override
        public void write(char[] c, int off, int len) throws IOException {
            writer.write(c, off, len);

            totalChars += len;

            int allowed = Math.min(limit - captor.length(), len);
            if (allowed > 0) {
                captor.append(c, off, allowed);
                checkLimitReached();
            }
        }

        @Override
        public void write(String str, int off, int len) throws IOException {
            writer.write(str, off, len);

            totalChars += len;

            int allowed = Math.min(limit - captor.length(), len);
            if (allowed > 0) {
                captor.append(str, off, off + allowed);
                checkLimitReached();
            }
        }

        @Override
        public Writer append(CharSequence csq) throws IOException {
            writer.append(csq);

            CharSequence cs = csq != null ? csq : "null"; //$NON-NLS-1$

            totalChars += cs.length();

            int allowed = Math.min(limit - captor.length(), cs.length());
            if (allowed > 0) {
                captor.append(csq, 0, allowed);
                checkLimitReached();
            }

            return this;
        }

        @Override
        public Writer append(CharSequence csq, int start, int end) throws IOException {
            writer.append(csq, start, end);

            totalChars += end - start;

            int allowed = Math.min(limit - captor.length(), end - start);
            if (allowed > 0) {
                captor.append(csq, start, start + allowed);
                checkLimitReached();
            }

            return this;
        }

        @Override
        public Writer append(char c) throws IOException {
            writer.append(c);

            totalChars++;
            if (captor.length() < limit) {
                captor.append(c);
                checkLimitReached();
            }

            return this;
        }

        @Override
        public void flush() throws IOException {
            writer.flush();
        }

        @Override
        public void close() throws IOException {
            writer.close();
        }

        private void checkLimitReached() {
            if (totalChars >= limit && limitReachedCallback != null) {
                limitReachedCallback.accept(this);
                limitReachedCallback = null;
            }
        }

        String captured() {
            return captor.toString();
        }

        long totalChars() {
            return totalChars;
        }
    }

    private static <T> Consumer<T> consumer(Runnable callback) {
        return callback != null ? t -> callback.run() : null;
    }

    private static final class ByteCaptor extends ByteArrayOutputStream {

        private ByteCaptor(int initialCapacity) {
            super(initialCapacity);
        }

        private void reset(int mark) {
            count = mark;
        }

        private byte[] captured() {
            return toByteArray();
        }

        private String captured(Charset charset) {
            return new String(buf, 0, count, charset);
        }
    }
}
