/*
 * ServletConsumerTest.java
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import java.io.IOException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@SuppressWarnings("nls")
class ServletConsumerTest {

    @Nested
    @DisplayName("andThen(ServletConsumer)")
    class AndThen {

        @Test
        @DisplayName("null argument")
        void testNullArgument() {
            ServletConsumer consumer = (req, resp) -> { /* does nothing */ };

            assertThrows(NullPointerException.class, () -> consumer.andThen(null));
        }

        @Test
        @DisplayName("accepts and accepts")
        void testAcceptsAndAccepts() throws IOException {
            ServletRequest request = mock(ServletRequest.class);
            ServletResponse response = mock(ServletResponse.class);

            ServletConsumer consumer = spy(new SpyableServletConsumer());
            ServletConsumer after = spy(new SpyableServletConsumer());
            ServletConsumer combined = consumer.andThen(after);

            combined.accept(request, response);

            verify(consumer).accept(request, response);
            verify(consumer).andThen(after);
            verify(after).accept(request, response);
            verifyNoMoreInteractions(consumer, after);
        }

        @Test
        @DisplayName("accepts and throws")
        void testAcceptsAndThrows() throws IOException {
            ServletRequest request = mock(ServletRequest.class);
            ServletResponse response = mock(ServletResponse.class);

            ServletConsumer consumer = spy(new SpyableServletConsumer());
            ServletConsumer after = (req, resp) -> {
                throw new IOException("after");
            };
            ServletConsumer combined = consumer.andThen(after);

            IOException exception = assertThrows(IOException.class, () -> combined.accept(request, response));
            assertEquals("after", exception.getMessage());

            verify(consumer).accept(request, response);
            verify(consumer).andThen(after);
            verifyNoMoreInteractions(consumer);
        }

        @Test
        @DisplayName("throws and accepts")
        void testThrowsAndAccepts() {
            ServletRequest request = mock(ServletRequest.class);
            ServletResponse response = mock(ServletResponse.class);

            ServletConsumer consumer = (req, resp) -> {
                throw new IOException("consumer");
            };
            ServletConsumer after = spy(new SpyableServletConsumer());
            ServletConsumer combined = consumer.andThen(after);

            IOException exception = assertThrows(IOException.class, () -> combined.accept(request, response));
            assertEquals("consumer", exception.getMessage());

            verifyNoInteractions(after);
        }

        @Test
        @DisplayName("throws and throws")
        void testThrowsAndThrows() {
            ServletRequest request = mock(ServletRequest.class);
            ServletResponse response = mock(ServletResponse.class);

            ServletConsumer consumer = (req, resp) -> {
                throw new IOException("consumer");
            };
            ServletConsumer after = (req, resp) -> {
                throw new IOException("after");
            };
            ServletConsumer combined = consumer.andThen(after);

            IOException exception = assertThrows(IOException.class, () -> combined.accept(request, response));
            assertEquals("consumer", exception.getMessage());
        }
    }

    static class SpyableServletConsumer implements ServletConsumer {

        @Override
        public void accept(ServletRequest request, ServletResponse response) throws IOException {
            // Do nothing
        }
    }
}
