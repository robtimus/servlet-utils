/*
 * CookieUtils.java
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

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

/**
 * A utility class for cookies.
 *
 * @author Rob Spoor
 */
public final class CookieUtils {

    private CookieUtils() {
        throw new IllegalStateException("cannot create instances of " + getClass().getName()); //$NON-NLS-1$
    }

    /**
     * Tries to find a specific cookie in a request.
     *
     * @param request The request to find the cookie in.
     * @param name The name of the cookie to find. This is case sensitive.
     * @return An {@link Optional} describing the cookie with the given name, or {@link Optional#empty()} if the request does not have such a cookie.
     * @throws NullPointerException If the given request is {@code null}.
     */
    public static Optional<Cookie> findCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            return Optional.empty();
        }
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(name)) {
                return Optional.of(cookie);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns all cookies in a request as a stream.
     *
     * @param request The request to return all cookies of.
     * @return A stream with all cookies in the given request; never {@code null} but possibly empty.
     * @throws NullPointerException If the given request is {@code null}.
     */
    public static Stream<Cookie> cookieStream(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        return cookies != null && cookies.length > 0 ? Arrays.stream(cookies) : Stream.empty();
    }
}
