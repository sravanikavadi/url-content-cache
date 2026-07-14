# URL Content Cache

A small Java command-line program that downloads a web page once and saves it in a local cache file. Later runs for the same URL read the saved file instead of calling the website again.

## Build and test

```bash
mvn test
```

## Run

There are two ways to give the program a URL.

**1. Pass it as an argument** (good for scripts and the command line):

```bash
mvn compile exec:java -Dexec.args="https://example.com"
```

**2. Just run it and type the URL when asked.** If no argument is given
(for example when you click *Run* in your IDE), the program prompts you:

```text
Enter a URL to fetch (http or https):
```

Either way, the URL is stored as a member of `UrlCacheApplication`.

Cache files are created under the `cache` directory. The URL is converted to a readable file name by replacing characters that are not safe for a file name with underscores.

Example:

```text
https://example.com/news?page=1
```

becomes:

```text
cache/https___example.com_news_page_1.cache
```

## Assumptions

- One HTTP or HTTPS URL is passed to the program.
- The response is text content.
- A cached page does not expire because the requirement says each URL is fetched only once.
- The original fetch date is stored in UTC.
- A failed web request does not create a cache file.
- URL length is reasonable for use in a local file name.
- The same local cache directory is used between runs.
