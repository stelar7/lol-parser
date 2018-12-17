package no.stelar7.cdragon.util.handlers;

import no.stelar7.cdragon.util.types.ByteArray;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class WebHandler
{
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
    
    public static void downloadBundle(String bundleId, Path output)
    {
        // https://lol.dyn.riotcdn.net/channels/public/bundles/bundleid.bundle
        if (Files.exists(output))
        {
            return;
        }
        
        downloadFile(output, "https://lol.dyn.riotcdn.net/channels/public/bundles/" + bundleId + ".bundle");
    }
    
    
    public static void downloadFile(Path output, String url)
    {
        try
        {
            Files.createDirectories(output.getParent());
            
            int          read;
            final byte[] buffer = new byte[4096];
            
            final URLConnection uc       = new URL(url.replace(" ", "%20")).openConnection();
            long                fileSize = uc.getContentLengthLong();
            
            try (InputStream in = uc.getInputStream(); OutputStream out = new FileOutputStream(output.toFile()))
            {
                while ((read = in.read(buffer)) != -1)
                {
                    out.write(buffer, 0, read);
                }
                out.flush();
            } catch (Exception e)
            {
                e.printStackTrace();
                downloadFile(output, url);
            }
            
            long localSize = Files.size(output);
            
            if (localSize < fileSize)
            {
                System.out.format("files are different size, trying again. %s != %s%n", fileSize, localSize);
                downloadFile(output, url);
            }
        } catch (IOException e)
        {
            e.printStackTrace();
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
            String versionAsIP = UtilHandler.getIPFromLong(++i);
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
        
        System.out.println("Returning version: " + lastGood + " (" + UtilHandler.getIPFromLong(lastGood) + ")");
        return lastGood;
    }
    
    public static String[] getMaxVersion(String url, int min, int max)
    {
        String[] urlEnds = {"/default-assets.wad.compressed", "/assets.wad.compressed"};
        for (int i = max; i >= min; i--)
        {
            for (String endPart : urlEnds)
            {
                String versionAsIP = UtilHandler.getIPFromLong(i);
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
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new URL(url).openConnection().getInputStream(), StandardCharsets.UTF_8)))
        {
            
            StringBuilder response = new StringBuilder();
            String        inputLine;
            
            while ((inputLine = in.readLine()) != null)
            {
                response.append(inputLine).append("\n");
            }
            return Arrays.stream(response.toString().split("\n")).collect(Collectors.toList());
        } catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }
    
}
