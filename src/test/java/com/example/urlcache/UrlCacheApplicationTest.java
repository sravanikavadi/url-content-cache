package com.example.urlcache;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UrlCacheApplicationTest {

    @Test
    void acceptsHttpAndHttpsUrls() {
        assertEquals("http", UrlCacheApplication.parseUrl("http://example.com").getScheme());
        assertEquals("https", UrlCacheApplication.parseUrl("https://example.com").getScheme());
    }

    @Test
    void rejectsUnsupportedUrlScheme() {
        assertThrows(
                IllegalArgumentException.class,
                () -> UrlCacheApplication.parseUrl("file:///tmp/page.txt"));
    }
}
