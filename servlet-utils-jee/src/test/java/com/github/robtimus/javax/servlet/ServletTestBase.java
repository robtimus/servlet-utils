/*
 * ServletTestBase.java
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

package com.github.robtimus.javax.servlet;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import java.security.SecureRandom;
import java.util.Random;
import java.util.function.Consumer;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

@SuppressWarnings({ "nls", "javadoc" })
public abstract class ServletTestBase {

    private static final Random RANDOM = new SecureRandom();

    private Server server;
    private ServerConnector serverConnector;

    protected void withServer(Consumer<ServletContextHandler> containerConfigurer, Runnable action) {
        startServer(containerConfigurer);
        try {
            action.run();
        } finally {
            stopServer();
        }
    }

    private void startServer(Consumer<ServletContextHandler> containerConfigurer) {
        QueuedThreadPool pool = new QueuedThreadPool();
        pool.setName("server");
        server = new Server(pool);

        serverConnector = new ServerConnector(server);
        server.addConnector(serverConnector);

        ServletContextHandler servletContext = new ServletContextHandler(server, "/");
        containerConfigurer.accept(servletContext);

        assertDoesNotThrow(server::start);
    }

    private void stopServer() {
        assertDoesNotThrow(server::stop);
    }

    protected void withClientRequest(Consumer<Request> action) {
        QueuedThreadPool pool = new QueuedThreadPool();
        pool.setName("client");

        HttpClient client = new HttpClient();
        client.setExecutor(pool);
        assertDoesNotThrow(client::start);
        try {
            Request request = client.newRequest("localhost", serverConnector.getLocalPort());
            action.accept(request);
        } finally {
            assertDoesNotThrow(client::stop);
        }
    }

    protected String randomBody(int length) {
        char[] body = new char[length];
        for (int i = 0; i < length; i++) {
            //include from space to ~ (inclusive), which are all the printable ASCII characters
            body[i] = (char) (' ' + RANDOM.nextInt('~' + 1 - ' '));
        }
        return new String(body);
    }
}
