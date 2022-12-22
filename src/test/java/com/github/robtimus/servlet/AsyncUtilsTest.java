/*
 * AsyncUtilsTest.java
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import java.io.IOException;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AsyncUtilsTest {

    @Nested
    @DisplayName("doFilter(ServletRequest, ServletResponse, FilterChain, ServletConsumer)")
    class DoFilterTest {

        @Test
        @DisplayName("synchronous requests")
        void testSynchronous() throws IOException, ServletException {
            ServletRequest request = mock(ServletRequest.class);
            ServletResponse response = mock(ServletResponse.class);
            FilterChain chain = mock(FilterChain.class);
            ServletConsumer action = mock(ServletConsumer.class);

            doReturn(false).when(request).isAsyncStarted();

            AsyncUtils.doFilter(request, response, chain, action);

            verify(request).isAsyncStarted();
            verify(chain).doFilter(request, response);
            verify(action).accept(request, response);
            verifyNoMoreInteractions(request, response, chain, action);
        }

        @Test
        @DisplayName("successful asynchronous request")
        void testAsynchronous() throws IOException, ServletException {
            ServletRequest request = mock(ServletRequest.class);
            ServletResponse response = mock(ServletResponse.class);
            FilterChain chain = mock(FilterChain.class);
            ServletConsumer action = mock(ServletConsumer.class);

            AsyncContext asyncContext = mock(AsyncContext.class);

            doReturn(true).when(request).isAsyncStarted();
            doReturn(asyncContext).when(request).getAsyncContext();
            doAnswer(i -> {
                AsyncListener listener = i.getArgument(0, AsyncListener.class);
                listener.onComplete(new AsyncEvent(asyncContext, request, response));
                return null;
            }).when(asyncContext).addListener(any());

            AsyncUtils.doFilter(request, response, chain, action);

            verify(request).isAsyncStarted();
            verify(request).getAsyncContext();
            verify(chain).doFilter(request, response);
            verify(asyncContext).addListener(any());
            verify(action).accept(request, response);
            verifyNoMoreInteractions(request, response, chain, asyncContext, action);
        }

        @Test
        @DisplayName("asynchronous request with error")
        void testAsynchronousWithError() throws IOException, ServletException {
            ServletRequest request = mock(ServletRequest.class);
            ServletResponse response = mock(ServletResponse.class);
            FilterChain chain = mock(FilterChain.class);
            ServletConsumer action = mock(ServletConsumer.class);

            AsyncContext asyncContext = mock(AsyncContext.class);

            doReturn(true).when(request).isAsyncStarted();
            doReturn(asyncContext).when(request).getAsyncContext();
            doAnswer(i -> {
                AsyncListener listener = i.getArgument(0, AsyncListener.class);
                listener.onError(new AsyncEvent(asyncContext, request, response));
                return null;
            }).when(asyncContext).addListener(any());

            AsyncUtils.doFilter(request, response, chain, action);

            verify(request).isAsyncStarted();
            verify(request).getAsyncContext();
            verify(chain).doFilter(request, response);
            verify(asyncContext).addListener(any());
            verify(action).accept(request, response);
            verifyNoMoreInteractions(request, response, chain, asyncContext, action);
        }

        @Test
        @DisplayName("asynchronous request with timeout")
        void testAsynchronousTimeout() throws IOException, ServletException {
            ServletRequest request = mock(ServletRequest.class);
            ServletResponse response = mock(ServletResponse.class);
            FilterChain chain = mock(FilterChain.class);
            ServletConsumer action = mock(ServletConsumer.class);

            AsyncContext asyncContext = mock(AsyncContext.class);

            doReturn(true).when(request).isAsyncStarted();
            doReturn(asyncContext).when(request).getAsyncContext();
            doAnswer(i -> {
                AsyncListener listener = i.getArgument(0, AsyncListener.class);
                listener.onTimeout(new AsyncEvent(asyncContext, request, response));
                return null;
            }).when(asyncContext).addListener(any());

            AsyncUtils.doFilter(request, response, chain, action);

            verify(request).isAsyncStarted();
            verify(request).getAsyncContext();
            verify(chain).doFilter(request, response);
            verify(asyncContext).addListener(any());
            verify(action).accept(request, response);
            verifyNoMoreInteractions(request, response, chain, asyncContext, action);
        }

        @Test
        @DisplayName("asynchronous request with multiple events")
        void testAsynchronousMultipleEvents() throws IOException, ServletException {
            ServletRequest request = mock(ServletRequest.class);
            ServletResponse response = mock(ServletResponse.class);
            FilterChain chain = mock(FilterChain.class);
            ServletConsumer action = mock(ServletConsumer.class);

            AsyncContext asyncContext = mock(AsyncContext.class);

            doReturn(true).when(request).isAsyncStarted();
            doReturn(asyncContext).when(request).getAsyncContext();
            doAnswer(i -> {
                AsyncListener listener = i.getArgument(0, AsyncListener.class);
                listener.onError(new AsyncEvent(asyncContext, request, response));
                listener.onComplete(new AsyncEvent(asyncContext, request, response));
                return null;
            }).when(asyncContext).addListener(any());

            AsyncUtils.doFilter(request, response, chain, action);

            verify(request).isAsyncStarted();
            verify(request).getAsyncContext();
            verify(chain).doFilter(request, response);
            verify(asyncContext).addListener(any());
            verify(action).accept(request, response);
            verifyNoMoreInteractions(request, response, chain, asyncContext, action);
        }

        @Test
        @DisplayName("asynchronous request with exception")
        void testAsynchronousWithException() throws IOException, ServletException {
            ServletRequest request = mock(ServletRequest.class);
            ServletResponse response = mock(ServletResponse.class);
            FilterChain chain = mock(FilterChain.class);
            ServletConsumer action = mock(ServletConsumer.class);

            AsyncContext asyncContext = mock(AsyncContext.class);

            doReturn(true).when(request).isAsyncStarted();
            doReturn(asyncContext).when(request).getAsyncContext();
            doThrow(IllegalStateException.class).when(asyncContext).addListener(any());

            AsyncUtils.doFilter(request, response, chain, action);

            verify(request).isAsyncStarted();
            verify(request).getAsyncContext();
            verify(chain).doFilter(request, response);
            verify(asyncContext).addListener(any());
            verify(action).accept(request, response);
            verifyNoMoreInteractions(request, response, chain, asyncContext, action);
        }
    }
}
