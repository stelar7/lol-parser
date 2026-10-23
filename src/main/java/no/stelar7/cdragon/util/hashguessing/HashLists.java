package no.stelar7.cdragon.util.hashguessing;

import no.stelar7.cdragon.util.ProgressBar;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Where the hash guessers get their hashes from: the CommunityDragon hash lists, and exported lists of unknown hashes
 */
public final class HashLists
{
    private HashLists()
    {
        // Hide public constructor
    }
    
    /**
     * The maintained hash lists from CommunityDragon
     */
    public static final String CDRAGON_HASH_URL = "https://raw.communitydragon.org/data/hashes/lol/";
    /**
     * Mirror of the same lists on GitHub, large lists are split into name.0, name.1, ...
     */
    public static final String GITHUB_HASH_URL  = "https://raw.githubusercontent.com/CommunityDragon/Data/master/hashes/lol/";

    /**
     * Streams a CommunityDragon hash list (eg. "hashes.game.txt") line by line into the consumer.
     * Uses raw.communitydragon.org, falling back to the GitHub mirror (and its split parts) if that fails.
     *
     * @return false if the list could not be loaded from anywhere
     */
    public static boolean readCDragonHashList(String name, Consumer<String> consumer)
    {
        if (readWebLines(CDRAGON_HASH_URL + name, consumer))
        {
            return true;
        }

        System.out.println("Unable to load " + name + " from raw.communitydragon.org, trying GitHub");
        if (readWebLines(GITHUB_HASH_URL + name, consumer))
        {
            return true;
        }

        boolean any = false;
        for (int part = 0; readWebLines(GITHUB_HASH_URL + name + "." + part, consumer); part++)
        {
            any = true;
        }

        if (!any)
        {
            System.out.println("Unable to load hash list " + name);
        }
        return any;
    }

    /**
     * Streams the lines of a text url without holding the whole file in memory (hashes.game.txt is >200MB)
     *
     * @return false if the url did not answer with 200
     */
    public static boolean readWebLines(String url, Consumer<String> consumer)
    {
        try
        {
            HttpURLConnection con = (HttpURLConnection) URI.create(url).toURL().openConnection();
            con.setConnectTimeout(30_000);
            con.setReadTimeout(120_000);
            if (con.getResponseCode() != 200)
            {
                con.disconnect();
                return false;
            }

            // content length is -1 if the server does not send it, the bar then only shows the amount read
            ProgressBar progress = new ProgressBar("Reading " + url.substring(url.lastIndexOf('/') + 1), con.getContentLengthLong(), true);
            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8)))
            {
                String line;
                while ((line = in.readLine()) != null)
                {
                    // the lists are ascii, so characters + newline is the byte count
                    progress.step(line.length() + 1);
                    if (!line.isEmpty())
                    {
                        consumer.accept(line);
                    }
                }
            }
            progress.finish();
            return true;
        } catch (IOException | IllegalArgumentException e)
        {
            System.err.println("Unable to read " + url + ": " + e.getMessage());
            return false;
        }
    }
    
    public static Set<String> unknownFromExportWAD(Path unknownFile)
    {
        try
        {
            return Files.readAllLines(unknownFile).stream().map(a -> a.substring(0, 16)).collect(Collectors.toSet());
            
        } catch (IOException e)
        {
            System.err.println(e.getMessage());
            System.out.println("Unable to load unknown hash file");
        }
        
        return Collections.emptySet();
    }
    
    public static Set<String> unknownFromExportBIN(Path unknownFile)
    {
        try
        {
            return new HashSet<>(Files.readAllLines(unknownFile));
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        
        return Collections.emptySet();
    }
}
