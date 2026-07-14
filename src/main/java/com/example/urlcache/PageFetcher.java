package com.example.urlcache;

import java.io.IOException;
import java.net.URI;

// How we get a page's content. Kept as an interface on purpose:
// the real one uses HTTP, but tests can just hand back a fixed string
// instead of hitting the network.
@FunctionalInterface
public interface PageFetcher {

    // Download the page and return its content as text.
    String fetch(URI url) throws IOException, InterruptedException;
}
