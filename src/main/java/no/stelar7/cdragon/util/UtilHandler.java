package no.stelar7.cdragon.util;

import com.google.gson.reflect.TypeToken;
import javafx.util.Pair;
import net.jpountz.xxhash.*;
import no.stelar7.api.l4j8.basic.utils.Utils;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public final class UtilHandler
{
    
    static
    {
        System.setProperty("joml.format", "false");
    }
    
    private UtilHandler()
    {
        // Hide public constructor
    }
    
    private static Map<Long, String> binHashNames;
    private static Map<Long, String> iniHashNames;
    
    private static       Map<String, Map<String, String>> wadHashNames   = new HashMap<>();
    private static       XXHashFactory                    xxHashFactory  = XXHashFactory.fastestInstance();
    private static final char[]                           hexArray       = "0123456789ABCDEF".toCharArray();
    public static final  Path                             WAD_HASH_STORE = Paths.get("src\\main\\java\\no\\stelar7\\cdragon\\types\\wad\\hashes");
    public static final  Path                             BIN_HASH_STORE = Paths.get("src\\main\\java\\no\\stelar7\\cdragon\\types\\bin\\data\\binhash.json");
    public static final  Path                             INI_HASH_STORE = Paths.get("src\\main\\java\\no\\stelar7\\cdragon\\types\\inibin\\data\\inihash.json");
    
    public static String getBINHash(int hash)
    {
        long val = Integer.toUnsignedLong(hash);
        
        if (binHashNames != null)
        {
            return binHashNames.getOrDefault(val, String.valueOf(val));
        }
        
        try
        {
            binHashNames = new HashMap<>();
            String            sb         = new String(Files.readAllBytes(BIN_HASH_STORE), StandardCharsets.UTF_8);
            Map<Long, String> pluginData = Utils.getGson().fromJson(sb, new TypeToken<Map<Long, String>>() {}.getType());
            binHashNames.putAll(pluginData);
            
            System.out.println("Loaded known bin hashes");
        } catch (IOException e)
        {
            binHashNames = Collections.emptyMap();
            System.err.println("File not found: " + e.getMessage());
        }
        
        return getBINHash(hash);
    }
    
    public static String getINIHash(int hash)
    {
        Long val = Integer.toUnsignedLong(hash);
        
        if (iniHashNames != null)
        {
            return iniHashNames.getOrDefault(val, String.valueOf(val));
        }
        
        try
        {
            iniHashNames = new HashMap<>();
            String            sb         = new String(Files.readAllBytes(INI_HASH_STORE), StandardCharsets.UTF_8);
            Map<Long, String> pluginData = Utils.getGson().fromJson(sb, new TypeToken<Map<Long, String>>() {}.getType());
            iniHashNames.putAll(pluginData);
            
            System.out.println("Loaded known bin hashes");
        } catch (IOException e)
        {
            iniHashNames = Collections.emptyMap();
            System.err.println("File not found: " + e.getMessage());
        }
        
        return getINIHash(hash);
    }
    
    
    public static Map<String, String> getKnownWADFileHashes(String pluginName)
    {
        if (wadHashNames.get(pluginName) != null)
        {
            return wadHashNames.get(pluginName);
        }
        
        try
        {
            String              sb         = new String(Files.readAllBytes(WAD_HASH_STORE.resolve(pluginName + ".json")), StandardCharsets.UTF_8);
            Map<String, String> pluginData = Utils.getGson().fromJson(sb, new TypeToken<Map<String, String>>() {}.getType());
            wadHashNames.put(pluginName, pluginData);
            
            System.out.println("Loaded known hashes for " + pluginName);
        } catch (IOException e)
        {
            wadHashNames.put(pluginName, Collections.emptyMap());
            System.err.println("File not found: " + e.getMessage());
        }
        
        return getKnownWADFileHashes(pluginName);
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
    
    
    public static String generateXXHash64(String text)
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
    
    public static long generateBINHash(String input)
    {
        String toHash = input.toLowerCase(Locale.ENGLISH);
        int    hash   = Integer.parseUnsignedInt("2166136261");
        int    mask   = Integer.parseUnsignedInt("16777619");
        
        for (int i = 0; i < toHash.length(); i++)
        {
            hash = hash ^ toHash.charAt(i);
            hash = hash * mask;
        }
        
        return Integer.toUnsignedLong(hash);
    }
    
    public static <T, Y> void pairPrintout(Path outputFile, List<Pair<T, Y>> data)
    {
        StringBuilder sb = new StringBuilder("{\n");
        for (Pair<?, ?> pair : data)
        {
            sb.append("\t\"").append(pair.getKey()).append("\": \"").append(pair.getValue()).append("\",\n");
        }
        sb.reverse().delete(0, 2).reverse().append("\n}");
        
        try
        {
            Files.createDirectories(outputFile.getParent());
            Files.write(outputFile, sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e)
        {
            e.printStackTrace();
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
    
    
    public static void downloadFile(Path output, String url)
    {
        try
        {
            if (Files.exists(output))
            {
                System.err.println("This file already exists: " + output.toString());
                return;
            }
            Files.createDirectories(output.getParent());
            
            int          read;
            final byte[] buffer = new byte[4096];
            
            // TODO Fake being a browser "better"
            final URLConnection uc = new URL(url).openConnection();
            //uc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            //uc.setRequestProperty("Content-Language", "en-US");
            //uc.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36");
            //uc.setRequestProperty("Host", "l3cdn.riotgames.com");
            
            try (InputStream in = uc.getInputStream(); OutputStream out = new FileOutputStream(output.toFile()))
            {
                while ((read = in.read(buffer)) != -1)
                {
                    out.write(buffer, 0, read);
                }
                out.flush();
            } catch (SocketTimeoutException e)
            {
                downloadFile(output, url);
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
    
    public static boolean notInRange(int x, int min, int max)
    {
        return x < min || x >= max;
    }
}
