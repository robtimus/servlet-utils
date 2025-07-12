/*
 * CookieUtilsTest.java
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.support.ParameterDeclarations;

@SuppressWarnings("nls")
class CookieUtilsTest {

    private static final int MIN_COOKIE_NUMBER = 5;
    private static final int MAX_COOKIE_NUMBER = 10;

    private HttpServletRequest request;

    @BeforeEach
    void initRequest() {
        request = mock(HttpServletRequest.class);

        Cookie[] cookies = IntStream.range(MIN_COOKIE_NUMBER, MAX_COOKIE_NUMBER)
                .mapToObj(i -> new Cookie(cookieName(i), "value" + i))
                .toArray(Cookie[]::new);

        when(request.getCookies()).thenReturn(cookies);
    }

    @Nested
    @DisplayName("findCookie(HttpServletRequest, String)")
    class FindCookie {

        @Test
        @DisplayName("no cookie array")
        void testNoCookieArray() {
            when(request.getCookies()).thenReturn(null);

            Optional<Cookie> cookie = CookieUtils.findCookie(request, cookieName(MIN_COOKIE_NUMBER));
            assertEquals(Optional.empty(), cookie);
        }

        @Test
        @DisplayName("empty cookie array")
        void testEmptyCookieArray() {
            when(request.getCookies()).thenReturn(new Cookie[0]);

            Optional<Cookie> cookie = CookieUtils.findCookie(request, cookieName(MIN_COOKIE_NUMBER));
            assertEquals(Optional.empty(), cookie);
        }

        @ParameterizedTest(name = "name: {0}")
        @ArgumentsSource(ExistingCookieNameProvider.class)
        @DisplayName("cookie exists")
        void testCookieExists(String name) {
            Optional<Cookie> cookie = CookieUtils.findCookie(request, name);
            assertNotEquals(Optional.empty(), cookie);
            cookie.ifPresent(c -> {
                assertEquals(name, c.getName());
            });
        }

        @ParameterizedTest(name = "name: {0}")
        @ArgumentsSource(NonExistingCookieNameProvider.class)
        @DisplayName("cookie does not exist")
        void testCookieDoesNotExist(String name) {
            Optional<Cookie> cookie = CookieUtils.findCookie(request, name);
            assertEquals(Optional.empty(), cookie);
        }
    }

    @Nested
    @DisplayName("cookieStream(HttpServletRequest)")
    class CookieStream {

        @Test
        @DisplayName("no cookie array")
        void testNoCookieArray() {
            when(request.getCookies()).thenReturn(null);

            List<String> names = CookieUtils.cookieStream(request)
                    .map(Cookie::getName)
                    .collect(Collectors.toList());
            assertEquals(Collections.emptyList(), names);
        }

        @Test
        @DisplayName("empty cookie array")
        void testEmptyCookieArray() {
            when(request.getCookies()).thenReturn(new Cookie[0]);

            List<String> names = CookieUtils.cookieStream(request)
                    .map(Cookie::getName)
                    .collect(Collectors.toList());
            assertEquals(Collections.emptyList(), names);
        }

        @Test
        @DisplayName("non-empty cookie array")
        void testNonEmptyCookieArray() {
            List<String> names = CookieUtils.cookieStream(request)
                    .map(Cookie::getName)
                    .collect(Collectors.toList());

            List<String> expectedNames = IntStream.range(MIN_COOKIE_NUMBER, MAX_COOKIE_NUMBER)
                    .mapToObj(CookieUtilsTest::cookieName)
                    .collect(Collectors.toList());

            assertEquals(expectedNames, names);
        }
    }

    private static String cookieName(int number) {
        return "name" + number;
    }

    private static final class ExistingCookieNameProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ParameterDeclarations parameters, ExtensionContext context) {
            return IntStream.range(MIN_COOKIE_NUMBER, MAX_COOKIE_NUMBER)
                    .mapToObj(CookieUtilsTest::cookieName)
                    .map(Arguments::arguments);
        }
    }

    private static final class NonExistingCookieNameProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ParameterDeclarations parameters, ExtensionContext context) {
            IntStream before = IntStream.range(MIN_COOKIE_NUMBER - 5, MIN_COOKIE_NUMBER);
            IntStream after = IntStream.range(MAX_COOKIE_NUMBER, MAX_COOKIE_NUMBER + 5);

            return IntStream.concat(before, after)
                    .mapToObj(CookieUtilsTest::cookieName)
                    .map(Arguments::arguments);
        }
    }
}
