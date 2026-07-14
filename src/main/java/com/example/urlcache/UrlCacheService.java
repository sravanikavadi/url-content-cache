package com.example.urlcache;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.logging.Logger;

// The heart of the program: "fetch it once, then always read it back from disk".
//
// We pass in the URL, the cache folder, the fetcher and the clock instead of
// creating them here. That keeps this class easy to test - a fake fetcher and a
// fixed clock mean no real network and no dependency on the real time.
//
// A cache file is just plain text: first line is the fetch date, second line is
// the URL, and everything after that is the page content.
public class UrlCacheService {

    private static final Logger LOG = Logger.getLogger(UrlCacheService.class.getName());

    private static final String CACHE_FILE_SUFFIX = ".cache";

    private final URI url;
    private final Path cacheDirectory;
    private final PageFetcher pageFetcher;
    private final Clock clock;

    public UrlCacheService(URI url, Path cacheDirectory, PageFetcher pageFetcher, Clock clock) {
        this.url = url;
        this.cacheDirectory = cacheDirectory;
        this.pageFetcher = pageFetcher;
        this.clock = clock;
    }

    // Give me the page. If we already have it on disk, read it from there;
    // otherwise go fetch it, save it, and hand it back.
    public CachedPage getPage() throws IOException, InterruptedException {
        Path cacheFile = getCacheFile();
        LOG.info(() -> "Resolved cache file for " + url + " -> " + cacheFile.toAbsolutePath());

        // Already have it - read from disk, don't touch the web.
        if (Files.exists(cacheFile)) {
            LOG.info(() -> "CACHE HIT - reading content from " + cacheFile.toAbsolutePath());
            return readFromFile(cacheFile);
        }

        // Don't have it yet - download it now.
        LOG.info(() -> "CACHE MISS - fetching content from web: " + url);
        String content = pageFetcher.fetch(url);

        // Stamp it with the time right now. This is the date every future run
        // will report as the original fetch date.
        CachedPage page = new CachedPage(url, Instant.now(clock), content);
        saveToFile(cacheFile, page);
        return page;
    }

    // Turn the URL into a safe file name - swap out anything that isn't a
    // letter, digit, dot or dash for an underscore.
    // e.g. https://example.com/news?page=1 -> https___example.com_news_page_1.cache
    Path getCacheFile() {
        String fileName = url.toString()
                .replaceAll("[^a-zA-Z0-9.-]", "_")
                + CACHE_FILE_SUFFIX;

        return cacheDirectory.resolve(fileName);
    }

    // Write the date, URL and content out to the cache file.
    private void saveToFile(Path cacheFile, CachedPage page) throws IOException {
        Files.createDirectories(cacheDirectory);

        String fileValue = page.getFetchedAt() + "\n"
                + page.getUrl() + "\n"
                + page.getContent();

        Files.write(cacheFile, fileValue.getBytes(StandardCharsets.UTF_8));
        LOG.info(() -> "Saved " + page.getContent().length()
                + " characters to cache file " + cacheFile.toAbsolutePath());
    }

    // Read back a cache file we wrote earlier.
    private CachedPage readFromFile(Path cacheFile) throws IOException {
        String fileValue = new String(Files.readAllBytes(cacheFile), StandardCharsets.UTF_8);

        // First two lines are the header (date + URL), the rest is the content.
        int firstLineEnd = fileValue.indexOf('\n');
        int secondLineEnd = fileValue.indexOf('\n', firstLineEnd + 1);

        if (firstLineEnd < 0 || secondLineEnd < 0) {
            throw new IOException("Cache file is invalid (missing header): " + cacheFile);
        }

        Instant fetchedAt = Instant.parse(fileValue.substring(0, firstLineEnd));
        URI storedUrl = URI.create(fileValue.substring(firstLineEnd + 1, secondLineEnd));
        String content = fileValue.substring(secondLineEnd + 1);

        LOG.info(() -> "Loaded cached page originally fetched at " + fetchedAt);
        return new CachedPage(storedUrl, fetchedAt, content);
    }
}
