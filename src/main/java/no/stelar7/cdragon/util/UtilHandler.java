package no.stelar7.cdragon.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.util.Pair;
import net.jpountz.xxhash.*;

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class UtilHandler
{
    
    private UtilHandler()
    {
        // Hide public constructor
    }
    
    private static       Map<String, Map<String, String>> hashNames     = new HashMap<>();
    private static       XXHashFactory                    xxHashFactory = XXHashFactory.fastestInstance();
    private static final char[]                           hexArray      = "0123456789ABCDEF".toCharArray();
    
    
    public static Map<String, String> getKnownFileHashes(String pluginName)
    {
        if (hashNames.get(pluginName) != null)
        {
            return hashNames.get(pluginName);
        }
        
        try
        {
            StringBuilder sb    = new StringBuilder();
            List<String>  lines = Files.readAllLines(Paths.get("hashes", pluginName + ".json"));
            lines.forEach(sb::append);
            
            Map<String, String> pluginData = new Gson().fromJson(sb.toString(), new TypeToken<Map<String, String>>() {}.getType());
            hashNames.put(pluginName, pluginData);
            
            sortAndWriteHashes(pluginName);
            System.out.println("Loaded known hashes for " + pluginName);
        } catch (IOException e)
        {
            hashNames.put(pluginName, Collections.emptyMap());
            System.err.println("File not found: " + e.getMessage());
        }
        
        return hashNames.get(pluginName);
    }
    
    private static void sortAndWriteHashes(String pluginName) throws IOException
    {
        List<Pair<String, String>> ml = new ArrayList<>();
        hashNames.get(pluginName).forEach((k, v) -> ml.add(new Pair<>(k, v)));
        ml.sort(Comparator.comparing(Pair::getValue, new NaturalOrderComparator()));
        
        Path          pho = Paths.get("hashes", pluginName + ".json");
        StringBuilder sb  = new StringBuilder("{\n");
        for (Pair<String, String> pair : ml)
        {
            sb.append("\t\"").append(pair.getKey()).append("\": \"").append(pair.getValue()).append("\",\n");
        }
        sb.reverse().delete(0, 2).reverse().append("\n}");
        
        Files.write(pho, sb.toString().getBytes(StandardCharsets.UTF_8));
    }
    
    public static String pathToFilename(Path path)
    {
        return path.getFileName().toString().substring(0, path.getFileName().toString().lastIndexOf('.'));
    }
    
    public static BitSet longToBitSet(long value)
    {
        BitSet bits  = new BitSet();
        int    index = 0;
        while (value != 0L)
        {
            if (value % 2L != 0)
            {
                bits.set(index);
            }
            ++index;
            value = value >>> 1;
        }
        return bits;
    }
    
    public static long bitsetToLong(BitSet bits)
    {
        long value = 0L;
        for (int i = 0; i < bits.length(); ++i)
        {
            value += bits.get(i) ? (1L << i) : 0L;
        }
        return value;
    }
    
    public static int getLongFromIP(String ipAddress)
    {
        long     result           = 0;
        String[] ipAddressInArray = ipAddress.split("\\.");
        
        for (int i = 3; i >= 0; i--)
        {
            long ip = Long.parseLong(ipAddressInArray[3 - i]);
            
            //left shifting 24,16,8,0 and bitwise OR
            //1. 192 << 24
            //1. 168 << 16
            //1. 1   << 8
            //1. 2   << 0
            result |= ip << (i * 8);
        }
        
        return (int) result;
    }
    
    public static String getIPFromLong(long ip)
    {
        return String.format("%d.%d.%d.%d", (ip >> 24) & 0xFF, (ip >> 16) & 0xFF, (ip >> 8) & 0xFF, ip & 0xFF);
    }
    
    
    public static String getHash(String text)
    {
        try
        {
            byte[]               data = text.getBytes(StandardCharsets.UTF_8);
            ByteArrayInputStream in   = new ByteArrayInputStream(data);
            
            StreamingXXHash64 hash64 = xxHashFactory.newStreamingHash64(0);
            byte[]            buf    = new byte[8];
            int               read;
            while ((read = in.read(buf)) != -1)
            {
                hash64.update(buf, 0, read);
            }
            return String.format("%016X", hash64.getValue()).toLowerCase(Locale.ENGLISH);
        } catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }
    
    public static String[] getMaxVersion(String url, int min, int max)
    {
        String[] urlEnds = {"/default-assets.wad.compressed", "/assets.wad.compressed"};
        for (int i = max; i >= min; i--)
        {
            for (String endPart : urlEnds)
            {
                String versionAsIP = getIPFromLong(i);
                String finalUrl    = String.format(url, versionAsIP) + endPart;
                System.out.println("Looking for " + finalUrl);
                
                if (checkIfURLExists(finalUrl))
                {
                    return new String[]{finalUrl, versionAsIP};
                }
            }
        }
        return null;
    }
    
    private static boolean checkIfURLExists(String finalUrl)
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
    
    
    public static void tryDownloadVersion(Path output, String url, String version)
    {
        String finalUrl = String.format(url, version);
        
        downloadEfficient(output, finalUrl);
    }
    
    private static void downloadEfficient(Path output, String url)
    {
        try (ReadableByteChannel rbc = Channels.newChannel(new URL(url).openStream());
             FileOutputStream fos = new FileOutputStream(output.toFile()))
        {
            long bytes = fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            if (bytes < 20 * 1028 * 1028)
            {
                fos.close();
                rbc.close();
                
                System.out.println("Efficient download failed, trying old version");
                Files.deleteIfExists(output);
                downloadOldWay(output, url);
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    
    private static void downloadOldWay(Path output, String url)
    {
        try
        {
            final String        USER_AGENT_VALUE = "Mozilla/5.0 (Windows NT 5.1) AppleWebKit/535.11 (KHTML, like Gecko) Chrome/17.0.963.56 Safari/535.11";
            final byte[]        buffer           = new byte[1024];
            int                 read;
            final URLConnection uc               = new URL(url).openConnection();
            uc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            uc.setRequestProperty("Content-Language", "en-US");
            uc.setRequestProperty("User-Agent", USER_AGENT_VALUE);
            uc.setUseCaches(false);
            uc.setDoInput(true);
            uc.setDoOutput(true);
            try (InputStream in = uc.getInputStream(); OutputStream out = new FileOutputStream(output.toFile()))
            {
                while ((read = in.read(buffer)) != -1)
                {
                    out.write(buffer, 0, read);
                }
                out.flush();
            } catch (final FileNotFoundException e)
            {
                e.printStackTrace();
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    public static String readAsString(Path path)
    {
        StringBuilder sb = new StringBuilder();
        try
        {
            List<String> lines = Files.readAllLines(path);
            lines.forEach(sb::append);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        return sb.toString();
    }
    
    
    public static String toHex(byte[] bytes)
    {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++)
        {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
