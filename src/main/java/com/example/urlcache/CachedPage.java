package com.example.urlcache;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;

// A page we downloaded, plus when we downloaded it.
// These are exactly the three things we print on every run:
// the URL, the original fetch date, and the content.
// It's immutable, so it's safe to pass around.
public final class CachedPage {
    private final URI url;
    private final Instant fetchedAt;
    private final String content;

    public CachedPage(URI url, Instant fetchedAt, String content) {
        // None of these make sense as null, so stop early if they are.
        this.url = Objects.requireNonNull(url, "url");
        this.fetchedAt = Objects.requireNonNull(fetchedAt, "fetchedAt");
        this.content = Objects.requireNonNull(content, "content");
    }

    public URI getUrl() {
        return url;
    }

    // When we first fetched this from the web. Doesn't change on later runs.
    public Instant getFetchedAt() {
        return fetchedAt;
    }

    public String getContent() {
        return content;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CachedPage)) {
            return false;
        }
        CachedPage that = (CachedPage) other;
        return url.equals(that.url)
                && fetchedAt.equals(that.fetchedAt)
                && content.equals(that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, fetchedAt, content);
    }

    @Override
    public String toString() {
        return "CachedPage{url=" + url + ", fetchedAt=" + fetchedAt
                + ", contentLength=" + content.length() + "}";
    }
}
