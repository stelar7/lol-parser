package no.stelar7.cdragon.types.rst;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * Loads the RST key name lists from CommunityDragon ("&lt;hex hash&gt; &lt;key&gt;" per line), for use with {@link RSTFile#getNamedEntries(Map)}.
 * <p>
 * Use {@link #XXH3_URL} for files from patch 14.15 and newer, {@link #XXH64_URL} for older files.
 */
public final class RSTKeyNames
{
    public static final String XXH3_URL  = "https://raw.communitydragon.org/data/hashes/lol/hashes.rst.xxh3.txt";
    public static final String XXH64_URL = "https://raw.communitydragon.org/data/hashes/lol/hashes.rst.xxh64.txt";

    private RSTKeyNames()
    {
    }

    public static Map<Long, String> load(Path file) throws IOException
    {
        try (Stream<String> lines = Files.lines(file, StandardCharsets.UTF_8))
        {
            return parse(lines);
        }
    }

    public static Map<Long, String> load(URL url) throws IOException
    {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8)))
        {
            return parse(in.lines());
        }
    }

    public static Map<Long, String> load(String url) throws IOException
    {
        return load(URI.create(url).toURL());
    }

    /**
     * Parses "hash key" lines. The key is everything after the first space, kept as is (some keys start with a space).
     * Lines that do not start with a hex hash are skipped.
     */
    public static Map<Long, String> parse(Stream<String> lines)
    {
        Map<Long, String> names = new HashMap<>();
        lines.forEach(line -> {
            int split = line.indexOf(' ');
            if (split <= 0)
            {
                return;
            }

            try
            {
                names.put(Long.parseUnsignedLong(line, 0, split, 16), line.substring(split + 1));
            } catch (NumberFormatException e)
            {
                // not a hash line
            }
        });
        return names;
    }
}
