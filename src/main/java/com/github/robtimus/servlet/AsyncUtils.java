/*
 * AsyncUtils.java
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

package com.github.robtimus.servlet;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility classes for dealing with asynchronous request handling.
 *
 * @author Rob Spoor
 */
public final class AsyncUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncUtils.class);

    private AsyncUtils() {
    }

    /**
     * Proceeds in the filter chain and executes an action after the chain ends. This method can be used to ensure the given action is executed at the
     * end of the request, regardless of whether or not the request is handled asynchronously.
     * <p>
     * If the request has not been put into asynchronous mode, this method will execute the following:
     * <pre><code>
     * try {
     *     chain.doFilter(request, response);
     * } finally {
     *     action.run(request, response);
     * }
     * </code></pre>
     * However, if the request has been put into asynchronous mode, this method will ensure that the given action will execute once the asynchronous
     * operation completes, successfully or not.
     *
     * @param request The request to use in both {@link FilterChain#doFilter(ServletRequest, ServletResponse) chain.doFilter} and the action.
     * @param response The response to use in both {@link FilterChain#doFilter(ServletRequest, ServletResponse) chain.doFilter} and the action.
     * @param chain The filter chain to call {@link FilterChain#doFilter(ServletRequest, ServletResponse)} on.
     * @param action The action to execute after the chain ends.
     * @throws IOException Re-thrown from the call to {@link FilterChain#doFilter(ServletRequest, ServletResponse) chain.doFilter} or the action.
     * @throws ServletException Re-thrown from the call to {@link FilterChain#doFilter(ServletRequest, ServletResponse) chain.doFilter}.
     * @throws NullPointerException If the request, response filter chain or action is {@code null}.
     */
    public static void doFilter(ServletRequest request, ServletResponse response, FilterChain chain, ServletConsumer action)
            throws IOException, ServletException {

        Objects.requireNonNull(request);
        Objects.requireNonNull(response);
        Objects.requireNonNull(chain);
        Objects.requireNonNull(action);

        try {
            chain.doFilter(request, response);
        } finally {
            if (request.isAsyncStarted()) {
                try {
                    request.getAsyncContext().addListener(new ServletConsumerAsyncListener(action, request, response));
                } catch (IllegalStateException e) {
                    LOGGER.warn(Messages.AsyncUtils.addListenerError.get(), e);
                    action.accept(request, response);
                }
            } else {
                action.accept(request, response);
            }
        }
    }

    private static final class ServletConsumerAsyncListener implements AsyncListener {

        private final AtomicReference<ServletConsumer> actionReference;
        private final ServletRequest request;
        private final ServletResponse response;

        private ServletConsumerAsyncListener(ServletConsumer action, ServletRequest request, ServletResponse response) {
            actionReference = new AtomicReference<>(action);
            this.request = request;
            this.response = response;
        }

        @Override
        public void onComplete(AsyncEvent event) throws IOException {
            runAction();
        }

        @Override
        public void onTimeout(AsyncEvent event) throws IOException {
            runAction();
        }

        @Override
        public void onError(AsyncEvent event) throws IOException {
            runAction();
        }

        @Override
        public void onStartAsync(AsyncEvent event) throws IOException {
            // Don't do anything
        }

        private void runAction() throws IOException {
            ServletConsumer action = actionReference.getAndSet(null);
            if (action != null) {
                action.accept(request, response);
            }
        }
    }
}
