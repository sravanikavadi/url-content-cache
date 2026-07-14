package com.example.urlcache;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UrlCacheServiceTest {

    @TempDir
    Path cacheDirectory;

    @Test
    void fetchesAndSavesPageWhenCacheFileDoesNotExist() throws Exception {
        URI url = URI.create("https://example.com/news?page=1");
        Instant fetchedAt = Instant.parse("2026-07-14T18:00:00Z");
        AtomicInteger webCalls = new AtomicInteger();

        UrlCacheService service = createService(
                url,
                requestedUrl -> {
                    webCalls.incrementAndGet();
                    return "page content";
                },
                fetchedAt);

        CachedPage page = service.getPage();

        assertEquals(url, page.getUrl());
        assertEquals(fetchedAt, page.getFetchedAt());
        assertEquals("page content", page.getContent());
        assertEquals(1, webCalls.get());
        assertTrue(Files.exists(service.getCacheFile()));
    }

    @Test
    void readsPageFromFileWithoutCallingWebAgain() throws Exception {
        URI url = URI.create("https://example.com/news");
        AtomicInteger webCalls = new AtomicInteger();
        PageFetcher fetcher = requestedUrl -> "response " + webCalls.incrementAndGet();

        CachedPage firstRun = createService(
                url,
                fetcher,
                Instant.parse("2026-07-14T18:00:00Z"))
                .getPage();

        CachedPage secondRun = createService(
                url,
                fetcher,
                Instant.parse("2026-07-15T18:00:00Z"))
                .getPage();

        assertEquals(1, webCalls.get());
        assertEquals(firstRun.getFetchedAt(), secondRun.getFetchedAt());
        assertEquals("response 1", secondRun.getContent());
    }

    @Test
    void createsReadableFileNameFromUrl() {
        URI url = URI.create("https://example.com/news?page=1");
        UrlCacheService service = createService(url, requestedUrl -> "content", Instant.EPOCH);

        assertEquals(
                "https___example.com_news_page_1.cache",
                service.getCacheFile().getFileName().toString());
    }

    @Test
    void preservesContentExactlyWhenReadFromCache() throws Exception {
        URI url = URI.create("https://example.com/page");
        String originalContent = "line one\nline two\n";

        createService(url, requestedUrl -> originalContent,
                Instant.parse("2026-07-14T18:00:00Z"))
                .getPage();

        CachedPage cachedPage = createService(
                url,
                requestedUrl -> {
                    throw new AssertionError("Website should not be called");
                },
                Instant.parse("2026-07-15T18:00:00Z"))
                .getPage();

        assertEquals(originalContent, cachedPage.getContent());
    }

    @Test
    void doesNotCreateCacheFileWhenWebRequestFails() {
        URI url = URI.create("https://example.com/failure");
        UrlCacheService service = createService(
                url,
                requestedUrl -> {
                    throw new IOException("Website is unavailable");
                },
                Instant.parse("2026-07-14T18:00:00Z"));

        assertThrows(IOException.class, service::getPage);
        assertFalse(Files.exists(service.getCacheFile()));
    }

    private UrlCacheService createService(URI url, PageFetcher fetcher, Instant fetchedAt) {
        return new UrlCacheService(
                url,
                cacheDirectory,
                fetcher,
                Clock.fixed(fetchedAt, ZoneOffset.UTC));
    }
}
