/*
 * ServletConsumer.java
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

package com.github.robtimus.jakarta.servlet;

import java.io.IOException;
import java.util.Objects;
import java.util.function.BiConsumer;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

/**
 *
 * An operation that takes a {@link ServletRequest} and {@link ServletResponse} and returns no result.
 * This is a specialization of {@link BiConsumer} that allows {@link IOException} to be thrown.
 *
 * @author Rob Spoor
 */
@FunctionalInterface
public interface ServletConsumer {

    /**
     * Performs this operation on the given request and response.
     *
     * @param request The request to operate on.
     * @param response The response to operate on.
     * @throws IOException If an I/O error occurs
     */
    void accept(ServletRequest request, ServletResponse response) throws IOException;

    /**
     * Returns a composed {@code ServletConsumer} that performs, in sequence, this operation followed by the {@code after} operation.
     * If performing either operation throws an exception, it is relayed to the caller of the composed operation.
     * If performing this operation throws an exception, the {@code after} operation will not be performed.
     *
     * @param after The operation to perform after this operation.
     * @return A composed {@code ServletConsumer} that performs in sequence this operation followed by the {@code after} operation.
     * @throws NullPointerException If {@code after} is {@code null}.
     */
    default ServletConsumer andThen(ServletConsumer after) {
        Objects.requireNonNull(after);

        return (req, resp) -> {
            accept(req, resp);
            after.accept(req, resp);
        };
    }
}
