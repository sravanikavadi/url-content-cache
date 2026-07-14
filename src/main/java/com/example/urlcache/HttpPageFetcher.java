package com.example.urlcache;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Logger;

// The real fetcher. Grabs the page over HTTP/HTTPS using the built-in HttpClient.
public class HttpPageFetcher implements PageFetcher {

    private static final Logger LOG = Logger.getLogger(HttpPageFetcher.class.getName());

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

    // One client, reused for every call - that's the recommended way to use it.
    private final HttpClient httpClient;

    public HttpPageFetcher() {
        this(HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build());
    }

    // Lets you pass in your own client - handy for sharing one across the app,
    // or swapping in a fake during tests.
    public HttpPageFetcher(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public String fetch(URI url) throws IOException, InterruptedException {
        LOG.info(() -> "Sending GET request to " + url);

        HttpRequest request = HttpRequest.newBuilder(url)
                .GET()
                .timeout(REQUEST_TIMEOUT)
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString());

        int status = response.statusCode();
        LOG.info(() -> "Received HTTP status " + status + " from " + url);

        // Only 2xx counts as success. Anything else and we bail out so we don't
        // end up caching an error page.
        if (status < 200 || status >= 300) {
            throw new IOException("Request to " + url + " failed with HTTP status " + status);
        }

        String body = response.body();
        LOG.info(() -> "Downloaded " + body.length() + " characters from " + url);
        return body;
    }
}
