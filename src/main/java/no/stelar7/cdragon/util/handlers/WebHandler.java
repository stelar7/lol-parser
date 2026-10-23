package no.stelar7.cdragon.util.handlers;

import no.stelar7.cdragon.types.rbun.RBUNParser;
import no.stelar7.cdragon.util.ErrorLog;
import no.stelar7.cdragon.util.types.*;

import javax.net.ssl.SSLException;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.net.http.HttpClient.Version;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class WebHandler
{
    
    // retries for failed downloads (connection errors and 5xx/429 responses), waiting a bit longer before each one
    private static final int MAX_ATTEMPTS = 5;

    // shared, as every HttpClient has its own selector thread and connection pool
    private static final HttpClient CLIENT = HttpClient.newBuilder()
                                                       .version(Version.HTTP_2)
                                                       .followRedirects(HttpClient.Redirect.NORMAL)
                                                       .connectTimeout(Duration.ofSeconds(30))
                                                       .build();
    
        public static boolean checkIfURLExists(String finalUrl)
    {
        try
        {
            HttpURLConnection con = (HttpURLConnection) new URL(finalUrl).openConnection();
            if (con.getResponseCode() == 200)
            {
                System.out.println("Found version: " + finalUrl);
                return true;
            }
            con.disconnect();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        
        return false;
    }
    
    public static boolean shouldDownloadBundle(String bundleId, Path output, long bundleSize)
    {
        if (Files.exists(output))
        {
            try
            {
                RBUNParser parser   = new RBUNParser();
                int        metaSize = parser.parse(output).getMetadataSize();
                if (Files.size(output) == (bundleSize + metaSize))
                {
                    //System.out.println("Bundle " + bundleId + " already exists");
                    return false;
                }
            } catch (IOException | RuntimeException b)
            {
                // missing, truncated or corrupt bundle; download it again
                return true;
            }
        }
        
        return true;
    }
    
    public static void downloadBundle(String bundleId, Path output)
    {
        // https://lol.dyn.riotcdn.net/channels/public/bundles/bundleid.bundle
        //System.out.println("Downloading bundle " + bundleId);
        downloadFile(output, "https://lol.dyn.riotcdn.net/channels/public/bundles/" + bundleId + ".bundle");
    }
    
    /**
     * Downloads the url to the file. The data is written to a .part file first, and only moved into place when the download succeeded,
     * so a failed download never leaves a truncated file (or an error page) behind.
     *
     * @return false if the download failed, the reason is written to the {@link ErrorLog}
     */
    public static boolean downloadFile(Path output, String url)
    {
        HttpRequest request;
        try
        {
            Files.createDirectories(output.getParent());
            request = HttpRequest.newBuilder().uri(new URI(url)).GET().build();
        } catch (IOException | URISyntaxException e)
        {
            ErrorLog.log("download failed", url, e);
            return false;
        }

        Path   temp      = output.resolveSibling(output.getFileName() + ".part");
        String lastError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++)
        {
            try
            {
                HttpResponse<Path> response = CLIENT.send(request, BodyHandlers.ofFile(temp));
                int                status   = response.statusCode();
                if (status >= 200 && status < 300)
                {
                    Files.move(temp, output, StandardCopyOption.REPLACE_EXISTING);
                    return true;
                }

                Files.deleteIfExists(temp);
                lastError = "HTTP " + status;
                if (status < 500 && status != 429)
                {
                    // eg. 404, retrying will not help
                    break;
                }
            } catch (IOException e)
            {
                lastError = e.toString();
                deleteQuietly(temp);
            } catch (InterruptedException e)
            {
                deleteQuietly(temp);
                Thread.currentThread().interrupt();
                return false;
            }

            if (attempt < MAX_ATTEMPTS && !sleep(attempt * 1000L))
            {
                return false;
            }
        }

        ErrorLog.log("download failed", url + " -> " + output + ": " + lastError);
        return false;
    }

    private static void deleteQuietly(Path path)
    {
        try
        {
            Files.deleteIfExists(path);
        } catch (IOException e)
        {
            // it is overwritten by the next attempt anyway
        }
    }

    /**
     * @return false if the thread was interrupted
     */
    private static boolean sleep(long millis)
    {
        try
        {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    public static int getMaxVersion(String url, String file, int min)
    {
        int i         = min - 1;
        int failCount = 0;
        int lastGood  = -1;
        
        System.out.println("Finding current version for: " + url.substring(url.lastIndexOf("/")) + file);
        while (failCount < 5)
        {
            String versionAsIP = ByteHandler.getIPFromLong(++i);
            String finalUrl    = String.format(url, versionAsIP) + file;
            if (!canConnect(finalUrl))
            {
                failCount++;
                System.out.println("Found bad version: " + i + " (" + failCount + "/5)");
            } else
            {
                lastGood = i;
                failCount = 0;
                System.out.println("Found good version: " + lastGood);
            }
        }
        
        System.out.println("Returning version: " + lastGood + " (" + ByteHandler.getIPFromLong(lastGood) + ")");
        return lastGood;
    }
    
    public static String[] getMaxVersion(String url, int min, int max)
    {
        String[] urlEnds = {"/default-assets.wad.compressed", "/assets.wad.compressed"};
        for (int i = max; i >= min; i--)
        {
            for (String endPart : urlEnds)
            {
                String versionAsIP = ByteHandler.getIPFromLong(i);
                String finalUrl    = String.format(url, versionAsIP) + endPart;
                System.out.println("Looking for " + finalUrl);
                
                if (WebHandler.checkIfURLExists(finalUrl))
                {
                    return new String[]{finalUrl, versionAsIP};
                }
            }
        }
        return null;
    }
    
    
    public static ByteArray readBytes(String url)
    {
        int          read;
        final byte[] buffer = new byte[4096];
        
        try (InputStream in = new URL(url).openConnection().getInputStream(); ByteArrayOutputStream bo = new ByteArrayOutputStream())
        {
            while ((read = in.read(buffer)) != -1)
            {
                bo.write(buffer, 0, read);
            }
            
            bo.flush();
            return new ByteArray(bo.toByteArray());
        } catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }
    
    public static boolean canConnect(String urlString)
    {
        try
        {
            URL               url  = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            return (conn.getResponseCode() == 200);
        } catch (IOException e)
        {
            e.printStackTrace();
            return false;
        }
    }
    
    public static List<String> readWeb(String url)
    {
        try
        {
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            for (int attempt = 1; con.getResponseCode() == 503 && attempt < MAX_ATTEMPTS; attempt++)
            {
                con.disconnect();
                Thread.sleep(500L * attempt);
                con = (HttpURLConnection) new URL(url).openConnection();
            }
            
            try (InputStreamReader isr = new InputStreamReader(con.getInputStream());
                 BufferedReader in = new BufferedReader(isr))
            {
                StringBuilder response = new StringBuilder();
                String        inputLine;
                
                while ((inputLine = in.readLine()) != null)
                {
                    response.append(inputLine).append("\n");
                }
                return Arrays.stream(response.toString().split("\n")).collect(Collectors.toList());
            }
        } catch (IOException | InterruptedException e)
        {
            return Collections.emptyList();
        }
    }
    
    public static void downloadBundleBytes(String bundleId, List<LongRange> ranges, Path outputFolder)
    {
        // https://lol.dyn.riotcdn.net/channels/public/bundles/bundleid.bundle
        try
        {
            for (LongRange range : ranges)
            {
                String url         = "https://lol.dyn.riotcdn.net/channels/public/bundles/" + bundleId + ".bundle";
                String rangeString = range.getFrom() + "-" + range.getTo();
                Path   outputFile  = outputFolder.resolve(bundleId + "-" + rangeString);
                
                int                 read;
                final byte[]        buffer = new byte[4096];
                final URLConnection uc     = new URL(url.replace(" ", "%20")).openConnection();
                uc.setRequestProperty("Range", "bytes=" + rangeString);
                try (InputStream in = uc.getInputStream(); OutputStream out = new FileOutputStream(outputFile.toFile()))
                {
                    while ((read = in.read(buffer)) != -1)
                    {
                        out.write(buffer, 0, read);
                    }
                    out.flush();
                }
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    public static ByteArrayOutputStream downloadFileToMemory(String url)
    {
        for (int attempt = 1; ; attempt++)
        {
            try
            {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                URL                   u   = new URL(url);
                try (InputStream is = u.openStream())
                {
                    is.transferTo(bos);
                }
                
                return bos;
            } catch (SSLException e)
            {
                // the connection was dropped during the handshake, try again
                if (attempt >= MAX_ATTEMPTS || !sleep(attempt * 1000L))
                {
                    ErrorLog.log("download failed", url, e);
                    return null;
                }
            } catch (IOException e)
            {
                ErrorLog.log("download failed", url, e);
                return null;
            }
        }
    }
}
