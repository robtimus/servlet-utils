# servlet-utils
[![Maven Central](https://img.shields.io/maven-central/v/com.github.robtimus/servlet-utils)](https://search.maven.org/artifact/com.github.robtimus/servlet-utils)
[![Build Status](https://github.com/robtimus/servlet-utils/actions/workflows/build.yml/badge.svg)](https://github.com/robtimus/servlet-utils/actions/workflows/build.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=com.github.robtimus%3Aservlet-utils&metric=alert_status)](https://sonarcloud.io/summary/overall?id=com.github.robtimus%3Aservlet-utils)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=com.github.robtimus%3Aservlet-utils&metric=coverage)](https://sonarcloud.io/summary/overall?id=com.github.robtimus%3Aservlet-utils)
[![Known Vulnerabilities](https://snyk.io/test/github/robtimus/servlet-utils/badge.svg)](https://snyk.io/test/github/robtimus/servlet-utils)

Provides utility classes for working with servlets. Below are some examples; for a full list, see the [API](https://robtimus.github.io/servlet-utils/apidocs/).

## Init parameter reading

Package [com.github.robtimus.servlet.parameters](https://robtimus.github.io/servlet-utils/apidocs/com/github/robtimus/servlet/parameters/package-summary.html) contains several classes that make it easy to read init parameters. For instance, to read an int init parameter that cannot be negative:

    requestLimit = IntParameter.of(config, "requestLimit")
            .atLeast(0)
            .valueWithDefault(Integer.MAX_VALUE);

## Transforming input/output

Sometimes it's necessary to transform the input and/or output. Class [ServletUtils](https://robtimus.github.io/servlet-utils/apidocs/com/github/robtimus/servlet/ServletUtils.html) contains methods to transform a `ServletInputStream`, `ServletOutputStream`, `BufferedReader` or `PrintWriter`, as retrieved from a `ServletRequest` or `ServletResponse`.

In addition, classes [InputTransformingHttpServletRequestWrapper](https://robtimus.github.io/servlet-utils/apidocs/com/github/robtimus/servlet/http/InputTransformingHttpServletRequestWrapper.html) and [OutputTransformingHttpServletResponseWrapper](https://robtimus.github.io/servlet-utils/apidocs/com/github/robtimus/servlet/http/OutputTransformingHttpServletResponseWrapper.html) make it easy to wrap an `HttpServletRequest` or `HttpServletResponse` with transforming input or output. Simply override the necessary `transform` method to provide the actual transformation.

## Cookie reading

Class [CookieUtils](https://robtimus.github.io/servlet-utils/apidocs/com/github/robtimus/servlet/http/CookieUtils.html) contains methods to read cookies as optionals and streams.

## Asynchronous support for try-finally

Method [AsyncUtils.doFilter](https://robtimus.github.io/servlet-utils/apidocs/com/github/robtimus/servlet/AsyncUtils.html#doFilter-javax.servlet.ServletRequest-javax.servlet.ServletResponse-javax.servlet.FilterChain-com.github.robtimus.servlet.ServletConsumer-) can be used as a replacement for a try-finally block that also works with asynchronous request handling.

The following only works for non-asynchronous request handling; for asynchronous request handling the code in the `finally` block is often executed too early.

    try {
        chain.doFilter(request, response);
    } finally {
        doSomething(request, response);
    }

The following works both with asynchronous and non-asynchronous request handling.

    AsyncUtils.doFilter(request, response, chain, (req, res) -> {
        doSomething(req, res);
    });

## Body capturing filter

Class [BodyCapturingFilter](https://robtimus.github.io/servlet-utils/apidocs/com/github/robtimus/servlet/http/BodyCapturingFilter.html) can be used as base class for a filter that captures the request and response bodies. It captures the request body while it body is read, and provides callback methods to perform the necessary logic when the request body is read or when the response body is written. See its documentation for more details.
