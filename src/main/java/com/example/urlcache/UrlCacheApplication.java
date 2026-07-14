package com.example.urlcache;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.util.Scanner;
import java.util.logging.Logger;

// Command-line entry point. Keeps the URL as a field (as the task asks) and,
// on each run, gets the page, prints the date/URL/content, and exits.
public class UrlCacheApplication {

    private static final Logger LOG = Logger.getLogger(UrlCacheApplication.class.getName());

    // Where cache files go, relative to wherever we're run from.
    private static final Path DEFAULT_CACHE_DIRECTORY = Paths.get("cache");

    // The URL we're fetching, held as a member.
    private final URI url;
    private final Path cacheDirectory;

    public UrlCacheApplication(URI url) {
        this(url, DEFAULT_CACHE_DIRECTORY);
    }

    // Same thing but lets you pick the cache folder (used by tests).
    public UrlCacheApplication(URI url, Path cacheDirectory) {
        this.url = url;
        this.cacheDirectory = cacheDirectory;
    }

    // Builds the service with the real fetcher and clock. Pulled out so it's
    // easy to reuse or override.
    UrlCacheService createService() {
        return new UrlCacheService(
                url,
                cacheDirectory,
                new HttpPageFetcher(),
                Clock.systemUTC());
    }

    // One full run: get the page (from cache or web) and print what's required.
    public void run() throws Exception {
        LOG.info(() -> "Starting run for URL: " + url);

        CachedPage page = createService().getPage();

        // The actual required output, kept on stdout (not the logger). We print
        // the (possibly huge) page content first, then the date and URL last -
        // that way the summary is the final thing on screen and doesn't scroll
        // out of view behind a long page.
        String line = "============================================================";

        System.out.println("Page Content:");
        System.out.println(page.getContent());
        System.out.println(line);
        System.out.println("Original Date: " + page.getFetchedAt());
        System.out.println("URL: " + page.getUrl());
        System.out.println(line);

        LOG.info("Run finished successfully.");
    }

    public static void main(String[] args) {
        // You can pass the URL on the command line, e.g.
        //   -Dexec.args="https://example.com"
        // But if you just hit "Run" in the IDE there are no arguments, so in
        // that case we simply ask you to type one in.
        String urlValue;
        if (args.length == 1) {
            urlValue = args[0];
        } else if (args.length == 0) {
            urlValue = promptForUrl();
        } else {
            System.err.println("Usage: java UrlCacheApplication <url>");
            System.exit(1);
            return;
        }

        try {
            URI url = parseUrl(urlValue);
            new UrlCacheApplication(url).run();
            // Nothing else to do - returning from main lets the JVM shut down.
        } catch (Exception exception) {
            LOG.severe(() -> "Run failed: " + exception);
            // Some errors (like an unknown host) have no message, so fall back
            // to the exception itself so we never print a bare "null".
            String reason = exception.getMessage() != null
                    ? exception.getMessage()
                    : exception.toString();
            System.err.println("Unable to retrieve the page: " + reason);
            System.exit(1);
        }
    }

    // No URL was given on the command line, so just ask the user to type one.
    // Keeps asking until they enter something (or the input is closed).
    // Note: we don't close the Scanner on purpose - closing it would also close
    // System.in, and the program is about to exit anyway.
    @SuppressWarnings("resource")
    private static String promptForUrl() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("Enter a URL to fetch (http or https): ");
            if (!scanner.hasNextLine()) {
                return "";  // input was closed (e.g. end of file) - give up
            }
            String line = scanner.nextLine().trim();
            if (!line.isEmpty()) {
                return line;
            }
            System.out.println("Nothing entered - please type a URL.");
        }
    }

    // Read the URL and make sure it's http or https before we go any further.
    static URI parseUrl(String value) {
        URI url = URI.create(value);
        String scheme = url.getScheme();

        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("URL must use http or https, but was: " + value);
        }

        return url;
    }
}
