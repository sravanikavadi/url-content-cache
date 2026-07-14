package com.example.urlcache;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Tests the real HTTP fetcher against a tiny local server we start up here,
// so we can check actual requests and status codes without needing the internet.
class HttpPageFetcherTest {

    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void startServer() throws IOException {
        // Port 0 just means "pick any free port".
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

        // One path returns a normal page, the other returns a 404.
        server.createContext("/ok", exchange -> respond(exchange, 200, "hello from server"));
        server.createContext("/missing", exchange -> respond(exchange, 404, "nope"));

        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void returnsBodyOnSuccessfulResponse() throws Exception {
        HttpPageFetcher fetcher = new HttpPageFetcher();

        String body = fetcher.fetch(URI.create(baseUrl + "/ok"));

        assertEquals("hello from server", body);
    }

    @Test
    void throwsIOExceptionOnNonSuccessStatus() {
        HttpPageFetcher fetcher = new HttpPageFetcher();

        IOException error = assertThrows(
                IOException.class,
                () -> fetcher.fetch(URI.create(baseUrl + "/missing")));

        // The 404 should show up in the message so it's obvious what went wrong.
        assertTrue(error.getMessage().contains("404"),
                "Expected the HTTP status in the message, was: " + error.getMessage());
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange,
                                int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
