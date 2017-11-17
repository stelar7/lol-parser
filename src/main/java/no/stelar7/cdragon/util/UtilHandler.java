package no.stelar7.cdragon.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.util.Pair;

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public final class UtilHandler
{
    
    private UtilHandler()
    {
        // Hide public constructor
    }
    
    private static Map<String, String>           hashNames;
    private static Map<ByteArrayWrapper, String> magicNumbers;
    
    public static Map<String, String> getKnownFileHashes()
    {
        if (hashNames == null)
        {
            System.out.println("Loading known hashes");
            try
            {
                StringBuilder sb    = new StringBuilder();
                List<String>  lines = Files.readAllLines(Paths.get("hashes.json"));
                lines.forEach(sb::append);
                
                hashNames = new Gson().fromJson(sb.toString(), new TypeToken<Map<String, String>>() {}.getType());
                
                //sortAndWriteHashes();
                
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        
        return hashNames;
    }
    
    private static void sortAndWriteHashes() throws IOException
    {
        List<Pair<String, String>> ml = new ArrayList<>();
        hashNames.forEach((k, v) -> ml.add(new Pair<>(k, v)));
        ml.sort(Comparator.comparing(Pair::getValue));
        
        Path ph = Paths.get("newHash.json");
        Files.write(ph, "{\n".getBytes(StandardCharsets.UTF_8));
        for (Pair<String, String> pair : ml)
        {
            Files.write(ph, ("\t\"" + pair.getKey() + "\": \"" + pair.getValue() + "\",\n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
        }
        Files.write(ph, "}".getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
        System.out.println("");
    }
    
    private static boolean isSame(byte a, byte b)
    {
        return a == b;
    }
    
    public static boolean isProbableJSON(byte[] data)
    {
        boolean isJSON       = (isSame(data[0], (byte) 0x7B) && (isSame(data[1], (byte) 0x22) || isSame(data[1], (byte) 0x0D)));
        boolean isEmptyJSON  = (isSame(data[0], (byte) 0x7B) && isSame(data[1], (byte) 0x7D));
        boolean isArrayJSON  = (isSame(data[0], (byte) 0x5B) && isSame(data[1], (byte) 0x7B) && isSame(data[2], (byte) 0x22));
        boolean isArrayJSON2 = (isSame(data[0], (byte) 0x5b) && isSame(data[1], (byte) 0xa) && isSame(data[2], (byte) 0x20) && isSame(data[3], (byte) 0x20));
        
        return isJSON || isArrayJSON || isArrayJSON2 || isEmptyJSON;
    }
    
    public static boolean isProbableDEFLATE(byte[] data)
    {
        boolean isNoCompress      = isSame(data[0], (byte) 0x78) && isSame(data[1], (byte) 0x01);
        boolean isBestCompress    = isSame(data[0], (byte) 0x78) && isSame(data[1], (byte) 0xDA);
        boolean isDefaultCompress = isSame(data[0], (byte) 0x78) && isSame(data[1], (byte) 0x9C);
        return isNoCompress || isBestCompress || isDefaultCompress;
        
    }
    
    public static boolean isProbableGZIP(byte[] data)
    {
        return isSame(data[0], (byte) 0x1f) && isSame(data[1], (byte) 0x8b);
    }
    
    public static boolean isProbableZSTD(byte[] data)
    {
        return isSame(data[0], (byte) 0x28) && isSame(data[1], (byte) 0xB5) && isSame(data[2], (byte) 0x2F) && isSame(data[3], (byte) 0xFD);
    }
    
    public static Map<ByteArrayWrapper, String> getMagicNumbers()
    {
        if (magicNumbers == null)
        {
            System.out.println("Loading magic numbers");
            
            ByteArrayWrapper oggMagic  = new ByteArrayWrapper(new byte[]{0x4f, 0x67, 0x67, 0x53});
            ByteArrayWrapper webmMagic = new ByteArrayWrapper(new byte[]{0x1A, 0x45, (byte) 0xDF, (byte) 0xA3});
            ByteArrayWrapper ddsMagic  = new ByteArrayWrapper(new byte[]{0x44, 0x44, 0x53, 0x20});
            ByteArrayWrapper pngMagic  = new ByteArrayWrapper(new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});
            ByteArrayWrapper jpgMagic  = new ByteArrayWrapper(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE1});
            ByteArrayWrapper jpegMagic = new ByteArrayWrapper(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0});
            
            
            magicNumbers = new HashMap<>();
            magicNumbers.put(oggMagic, "ogg");
            magicNumbers.put(webmMagic, "webm");
            magicNumbers.put(ddsMagic, "dds");
            magicNumbers.put(pngMagic, "png");
            magicNumbers.put(jpgMagic, "jpg");
            magicNumbers.put(jpegMagic, "jpg");
        }
        
        return magicNumbers;
    }
    
    public static String getMaxVersion(String url, int min, int max) throws InterruptedException
    {
        ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        
        System.out.println("Looking for highest version");
        int[]    foundMax   = {0};
        String[] versionMax = {""};
        for (int i = max; i >= min; i--)
        {
            final int tryme   = i;
            String    version = "0.0.1." + i;
            service.submit(() -> {
                try
                {
                    String            finalUrl = String.format(url, version);
                    HttpURLConnection con      = (HttpURLConnection) new URL(finalUrl).openConnection();
                    if (con.getResponseCode() == 200)
                    {
                        con.disconnect();
                        
                        if (tryme > foundMax[0])
                        {
                            foundMax[0] = tryme;
                            versionMax[0] = version;
                        }
                    }
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            });
        }
        
        service.shutdown();
        service.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        
        return versionMax[0];
    }
    
    public static void tryDownloadVersion(Path output, String url, String version) throws Exception
    {
        String finalUrl = String.format(url, version);
        
        ReadableByteChannel rbc = Channels.newChannel(new URL(finalUrl).openStream());
        FileOutputStream    fos = new FileOutputStream(output.resolve(version).toFile());
        
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
    }
}
