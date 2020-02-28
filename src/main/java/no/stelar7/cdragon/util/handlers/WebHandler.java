package no.stelar7.cdragon.util.handlers;

import no.stelar7.cdragon.types.rbun.RBUNParser;
import no.stelar7.cdragon.util.types.*;

import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.nio.BufferUnderflowException;
import java.nio.channels.*;
import java.nio.file.*;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.stream.Collectors;

public class WebHandler
{
    
    static
    {
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager()
        {
            public java.security.cert.X509Certificate[] getAcceptedIssuers()
            {
                return null;
            }
            
            public void checkClientTrusted(X509Certificate[] certs, String authType)
            {
            }
            
            public void checkServerTrusted(X509Certificate[] certs, String authType)
            {
            }
        }
        };
        
        try
        {
            // Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            
            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = (hostname, session) -> true;
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (NoSuchAlgorithmException | KeyManagementException e)
        {
            e.printStackTrace();
        }
        
    }
    
    
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
            } catch (BufferUnderflowException | IOException b)
            {
                return true;
            }
        }
        
        return true;
    }
    
    public static void downloadBundle(String bundleId, Path output)
    {
        // https://lol.dyn.riotcdn.net/channels/public/bundles/bundleid.bundle
        //System.out.println("Downloading bundle " + bundleId);
        downloadFile(output, "http://lol.dyn.riotcdn.net/channels/public/bundles/" + bundleId + ".bundle");
    }
    
    public static void downloadFile(Path output, String url)
    {
        try
        {
            Files.createDirectories(output.getParent());
            URL u = new URL(url);
            try (InputStream is = u.openStream();
                 ReadableByteChannel rbc = Channels.newChannel(is);
                 FileOutputStream fos = new FileOutputStream(output.toFile()))
            {
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            }
        } catch (SSLException e)
        {
            // try again
            downloadFile(output, url);
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
        try
        {
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            if (con.getResponseCode() == 503)
            {
                Thread.sleep(500);
                return readWeb(url);
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
            e.printStackTrace();
            return null;
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
}
