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
                
                sortAndWriteHashes();
            } catch (IOException e)
            {
                hashNames = Collections.emptyMap();
                System.err.println("File not found: " + e.getMessage());
            }
        }
        
        return hashNames;
    }
    
    private static void sortAndWriteHashes() throws IOException
    {
        List<Pair<String, String>> ml = new ArrayList<>();
        hashNames.forEach((k, v) -> ml.add(new Pair<>(k, v)));
        ml.sort(Comparator.comparing(Pair::getValue, new NaturalOrderComparator()));
        
        Path          pho = Paths.get("hashes.json");
        StringBuilder sb  = new StringBuilder("{\n");
        for (Pair<String, String> pair : ml)
        {
            sb.append("\t\"").append(pair.getKey()).append("\": \"").append(pair.getValue()).append("\",\n");
        }
        sb.reverse().delete(0, 2).reverse().append("\n}");
        
        Files.write(pho, sb.toString().getBytes(StandardCharsets.UTF_8));
    }
    
    public static Map<ByteArrayWrapper, String> getMagicNumbers()
    {
        if (magicNumbers == null)
        {
            System.out.println("Loading magic numbers");
            
            ByteArrayWrapper oggMagic  = new ByteArrayWrapper(new byte[]{(byte) 0x4f, (byte) 0x67, (byte) 0x67, (byte) 0x53});
            ByteArrayWrapper webmMagic = new ByteArrayWrapper(new byte[]{(byte) 0x1A, (byte) 0x45, (byte) 0xDF, (byte) 0xA3});
            ByteArrayWrapper ddsMagic  = new ByteArrayWrapper(new byte[]{(byte) 0x44, (byte) 0x44, (byte) 0x53, (byte) 0x20});
            ByteArrayWrapper pngMagic  = new ByteArrayWrapper(new byte[]{(byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47});
            ByteArrayWrapper jpgMagic  = new ByteArrayWrapper(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0});
            ByteArrayWrapper jpg2Magic = new ByteArrayWrapper(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE1});
            ByteArrayWrapper jpg3Magic = new ByteArrayWrapper(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xEC});
            ByteArrayWrapper jpg4Magic = new ByteArrayWrapper(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xDB});
            ByteArrayWrapper bnkMagic  = new ByteArrayWrapper(new byte[]{(byte) 0x42, (byte) 0x4B, (byte) 0x48, (byte) 0x44});
            ByteArrayWrapper anmMagic  = new ByteArrayWrapper(new byte[]{(byte) 0x72, (byte) 0x33, (byte) 0x64, (byte) 0x32});
            ByteArrayWrapper cgcMagic  = new ByteArrayWrapper(new byte[]{(byte) 0x06, (byte) 0x00, (byte) 0x00, (byte) 0x00});
            ByteArrayWrapper rmvbMagic = new ByteArrayWrapper(new byte[]{(byte) 0x50, (byte) 0x52, (byte) 0x4F, (byte) 0x50});
            ByteArrayWrapper lcovMagic = new ByteArrayWrapper(new byte[]{(byte) 0x54, (byte) 0x4E, (byte) 0x3A, (byte) 0x0A});
            ByteArrayWrapper gifMagic  = new ByteArrayWrapper(new byte[]{(byte) 0x47, (byte) 0x49, (byte) 0x46, (byte) 0x38});
            ByteArrayWrapper zipMagic  = new ByteArrayWrapper(new byte[]{(byte) 0x50, (byte) 0x4B, (byte) 0x03, (byte) 0x04});
            ByteArrayWrapper ttfMagic  = new ByteArrayWrapper(new byte[]{(byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00});
            ByteArrayWrapper otfMagic  = new ByteArrayWrapper(new byte[]{(byte) 0x4F, (byte) 0x54, (byte) 0x54, (byte) 0x4F});
            ByteArrayWrapper matMagic  = new ByteArrayWrapper(new byte[]{(byte) 0x33, (byte) 0x22, (byte) 0x11, (byte) 0x00});
            ByteArrayWrapper objMagic  = new ByteArrayWrapper(new byte[]{(byte) 0x5B, (byte) 0x4F, (byte) 0x62, (byte) 0x6A});
            ByteArrayWrapper unkMagic  = new ByteArrayWrapper(new byte[]{(byte) 0x74, (byte) 0x22, (byte) 0x00, (byte) 0x00});
            ByteArrayWrapper luaMagic  = new ByteArrayWrapper(new byte[]{(byte) 0x1B, (byte) 0x4C, (byte) 0x75, (byte) 0x61});
            ByteArrayWrapper hlslMagic = new ByteArrayWrapper(new byte[]{(byte) 0x23, (byte) 0x70, (byte) 0x72, (byte) 0x61});
            
            
            magicNumbers = new HashMap<>();
            // Sound
            magicNumbers.put(oggMagic, "ogg");
            
            // Video
            magicNumbers.put(webmMagic, "webm");
            
            // Image
            magicNumbers.put(pngMagic, "png");
            magicNumbers.put(jpgMagic, "jpg");
            magicNumbers.put(jpg2Magic, "jpg");
            magicNumbers.put(jpg3Magic, "jpg");
            magicNumbers.put(jpg4Magic, "jpg");
            magicNumbers.put(gifMagic, "gif");
            
            // Fonts
            magicNumbers.put(ttfMagic, "ttf");
            magicNumbers.put(otfMagic, "otf");
            
            // Div
            magicNumbers.put(zipMagic, "zip");
            magicNumbers.put(luaMagic, "lua");
            magicNumbers.put(hlslMagic, "hlsl");
            
            // 3D model
            magicNumbers.put(bnkMagic, "bnk");
            magicNumbers.put(ddsMagic, "dds");
            magicNumbers.put(anmMagic, "anm");
            magicNumbers.put(cgcMagic, "cgc");
            magicNumbers.put(rmvbMagic, "rmvb");
            magicNumbers.put(lcovMagic, "info");
            magicNumbers.put(matMagic, "mat");
            magicNumbers.put(objMagic, "obj");
            
            // i dont know...?
            magicNumbers.put(unkMagic, "unk");
        }
        
        return magicNumbers;
    }
    
    public static String getHash(String text)
    {
        try
        {
            XXHashFactory        factory = XXHashFactory.fastestInstance();
            byte[]               data    = text.getBytes(StandardCharsets.UTF_8);
            ByteArrayInputStream in      = new ByteArrayInputStream(data);
            
            StreamingXXHash64 hash64 = factory.newStreamingHash64(0);
            byte[]            buf    = new byte[8];
            for (; ; )
            {
                int read = in.read(buf);
                if (read == -1)
                {
                    break;
                }
                hash64.update(buf, 0, read);
            }
            return String.format("%016X", hash64.getValue()).toLowerCase(Locale.ENGLISH);
        } catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
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
    
    private static boolean isSame(byte a, byte b)
    {
        return a == b;
    }
    
    //<editor-fold desc="Decent-ish byte comparisons">
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
    
    public static boolean isProbableBOM(byte[] data)
    {
        boolean isUTF8BOM  = isSame(data[0], (byte) 0xEF) && isSame(data[1], (byte) 0xBB) && isSame(data[2], (byte) 0xBF);
        boolean isUTF16BOM = isSame(data[0], (byte) 0xFE) && isSame(data[1], (byte) 0xFF);
        boolean isUTF32BOM = isSame(data[0], (byte) 0x00) && isSame(data[1], (byte) 0x00) && isSame(data[2], (byte) 0xFE) && isSame(data[3], (byte) 0xFF);
        
        return isUTF8BOM || isUTF16BOM || isUTF32BOM;
    }
    //</editor-fold>
    
    
    //<editor-fold desc="Shitty byte-comparisons, need to fix..">
    
    public static boolean isProbableJSON(byte[] data)
    {
        boolean isJSON = (isSame(data[0], (byte) 0x7B) && (isSame(data[1], (byte) 0x22) || isSame(data[1], (byte) 0x0D)));
        isJSON |= (isSame(data[0], (byte) 0x7B) && isSame(data[1], (byte) 0x0A) && isSame(data[2], (byte) 0x20) && isSame(data[3], (byte) 0x20));
        isJSON |= (isSame(data[0], (byte) 0x7B) && isSame(data[1], (byte) 0x7D));
        isJSON |= (isSame(data[0], (byte) 0x5B) && isSame(data[1], (byte) 0x5D));
        isJSON |= (isSame(data[0], (byte) 0x7B) && isSame(data[1], (byte) 0x0A) && isSame(data[2], (byte) 0x7D) && isSame(data[3], (byte) 0x0A));
        isJSON |= (isSame(data[0], (byte) 0x7B) && isSame(data[1], (byte) 0x0A) && isSame(data[2], (byte) 0x0A) && isSame(data[3], (byte) 0x7D));
        isJSON |= (isSame(data[0], (byte) 0x5B) && isSame(data[1], (byte) 0x7B) && isSame(data[2], (byte) 0x22));
        isJSON |= (isSame(data[0], (byte) 0x5B) && isSame(data[1], (byte) 0x0A) && isSame(data[2], (byte) 0x20) && isSame(data[3], (byte) 0x20));
        
        isJSON |= (isSame(data[0], (byte) 0x5B) && new String(Arrays.copyOfRange(data, 1, 4), StandardCharsets.UTF_8).matches("\\d*,?\\d*"));
        
        return isJSON;
    }
    
    public static boolean isProbableCSS(byte[] data)
    {
        boolean isCSS = isSame(data[0], (byte) 0x2E) && isSame(data[1], (byte) 0x62) && isSame(data[2], (byte) 0x6F) && isSame(data[3], (byte) 0x6F);
        isCSS |= isSame(data[0], (byte) 0x2E) && isSame(data[1], (byte) 0x70) && isSame(data[2], (byte) 0x6C) && isSame(data[3], (byte) 0x6E);
        isCSS |= isSame(data[0], (byte) 0x62) && isSame(data[1], (byte) 0x6F) && isSame(data[2], (byte) 0x64) && isSame(data[3], (byte) 0x79);
        isCSS |= isSame(data[0], (byte) 0x2F) && isSame(data[1], (byte) 0x2A) && isSame(data[2], (byte) 0x40) && isSame(data[3], (byte) 0x69);
        isCSS |= isSame(data[0], (byte) 0x73) && isSame(data[1], (byte) 0x70) && isSame(data[2], (byte) 0x61) && isSame(data[3], (byte) 0x6E);
        isCSS |= isSame(data[0], (byte) 0x40) && isSame(data[1], (byte) 0x69) && isSame(data[2], (byte) 0x6D) && isSame(data[3], (byte) 0x70);
        isCSS |= isSame(data[0], (byte) 0x2F) && isSame(data[1], (byte) 0x2A) && isSame(data[2], (byte) 0x20) && isSame(data[3], (byte) 0x53);
        isCSS |= isSame(data[0], (byte) 0x2E) && isSame(data[1], (byte) 0x68) && isSame(data[2], (byte) 0x6F) && isSame(data[3], (byte) 0x6E);
        isCSS |= isSame(data[0], (byte) 0x2E) && isSame(data[1], (byte) 0x6C) && isSame(data[2], (byte) 0x6F) && isSame(data[3], (byte) 0x79);
        isCSS |= isSame(data[0], (byte) 0x2E) && isSame(data[1], (byte) 0x63) && isSame(data[2], (byte) 0x68) && isSame(data[3], (byte) 0x65);
        isCSS |= isSame(data[0], (byte) 0x2E) && isSame(data[1], (byte) 0x72) && isSame(data[2], (byte) 0x65) && isSame(data[3], (byte) 0x70);
        isCSS |= isSame(data[0], (byte) 0x2E) && isSame(data[1], (byte) 0x75) && isSame(data[2], (byte) 0x70) && isSame(data[3], (byte) 0x64);
        isCSS |= isSame(data[0], (byte) 0x40) && isSame(data[1], (byte) 0x66) && isSame(data[2], (byte) 0x6F) && isSame(data[3], (byte) 0x6E);
        isCSS |= isSame(data[0], (byte) 0x0A) && isSame(data[1], (byte) 0x3A) && isSame(data[2], (byte) 0x72) && isSame(data[3], (byte) 0x6F);
        isCSS |= isSame(data[0], (byte) 0x3A) && isSame(data[1], (byte) 0x72) && isSame(data[2], (byte) 0x6F) && isSame(data[3], (byte) 0x6F);
        
        return isCSS;
    }
    
    public static boolean isProbableJavascript(byte[] data)
    {
        boolean isJS = isSame(data[0], (byte) 0x21) && isSame(data[1], (byte) 0x66) && isSame(data[2], (byte) 0x75) && isSame(data[3], (byte) 0x6E);
        isJS |= isSame(data[0], (byte) 0x77) && isSame(data[1], (byte) 0x65) && isSame(data[2], (byte) 0x62) && isSame(data[3], (byte) 0x70);
        isJS |= isSame(data[0], (byte) 0x76) && isSame(data[1], (byte) 0x61) && isSame(data[2], (byte) 0x72) && isSame(data[3], (byte) 0x20);
        isJS |= isSame(data[0], (byte) 0x77) && isSame(data[1], (byte) 0x69) && isSame(data[2], (byte) 0x6E) && isSame(data[3], (byte) 0x64);
        isJS |= isSame(data[0], (byte) 0x22) && isSame(data[1], (byte) 0x75) && isSame(data[2], (byte) 0x73) && isSame(data[3], (byte) 0x65);
        isJS |= isSame(data[0], (byte) 0x50) && isSame(data[1], (byte) 0x72) && isSame(data[2], (byte) 0x65) && isSame(data[3], (byte) 0x4C);
        
        
        return isJS;
    }
    
    public static boolean isProbableHTML(byte[] data)
    {
        boolean isHTML = isSame(data[0], (byte) 0x3C) && isSame(data[1], (byte) 0x73) && isSame(data[2], (byte) 0x63) && isSame(data[3], (byte) 0x72);
        isHTML |= isSame(data[0], (byte) 0x3C) && isSame(data[1], (byte) 0x21) && isSame(data[2], (byte) 0x64) && isSame(data[3], (byte) 0x6F);
        isHTML |= isSame(data[0], (byte) 0x3C) && isSame(data[1], (byte) 0x6C) && isSame(data[2], (byte) 0x69) && isSame(data[3], (byte) 0x6E);
        isHTML |= isSame(data[0], (byte) 0x3C) && isSame(data[1], (byte) 0x3F) && isSame(data[2], (byte) 0x78) && isSame(data[3], (byte) 0x6D);
        isHTML |= isSame(data[0], (byte) 0x3C) && isSame(data[1], (byte) 0x74) && isSame(data[2], (byte) 0x65) && isSame(data[3], (byte) 0x6D);
        isHTML |= isSame(data[0], (byte) 0x3C) && isSame(data[1], (byte) 0x6C) && isSame(data[2], (byte) 0x6F) && isSame(data[3], (byte) 0x6C);
        isHTML |= isSame(data[0], (byte) 0x3C) && isSame(data[1], (byte) 0x73) && isSame(data[2], (byte) 0x76) && isSame(data[3], (byte) 0x67);
        isHTML |= isSame(data[0], (byte) 0x3C) && isSame(data[1], (byte) 0x21) && isSame(data[2], (byte) 0x2D) && isSame(data[3], (byte) 0x2D);
        isHTML |= isSame(data[0], (byte) 0x3C) && isSame(data[1], (byte) 0x64) && isSame(data[2], (byte) 0x69) && isSame(data[3], (byte) 0x76);
        isHTML |= isSame(data[0], (byte) 0x3C) && isSame(data[1], (byte) 0x69) && isSame(data[2], (byte) 0x6D) && isSame(data[3], (byte) 0x67);
        isHTML |= isSame(data[0], (byte) 0x3C) && isSame(data[1], (byte) 0x68) && isSame(data[2], (byte) 0x74) && isSame(data[3], (byte) 0x6D);
        isHTML |= isSame(data[0], (byte) 0x3C) && isSame(data[1], (byte) 0x73) && isSame(data[2], (byte) 0x74) && isSame(data[3], (byte) 0x79);
        isHTML |= isSame(data[0], (byte) 0x7B) && isSame(data[1], (byte) 0x7B) && isSame(data[2], (byte) 0x23) && isSame(data[3], (byte) 0x75);
        
        return isHTML;
    }
    
    private static List<ByteArrayWrapper> possibleTextTargets = loadTextTargets();
    
    public static boolean isProbableTXT(byte[] data)
    {
        if (isProbableBOM(data))
        {
            return true;
        }
        
        ByteArrayWrapper checkTarget = new ByteArrayWrapper(Arrays.copyOf(data, 3));
        boolean          isTXT       = new String(data, StandardCharsets.UTF_8).isEmpty();
        
        return isTXT || possibleTextTargets.contains(checkTarget);
    }
    
    private static List<ByteArrayWrapper> loadTextTargets()
    {
        List<ByteArrayWrapper> list = new ArrayList<>();
        
        byte[] magicText3Wide = {
                (byte) 0x63, (byte) 0x60, (byte) 0x63, (byte) 0x21, (byte) 0x30, (byte) 0x31,
                (byte) 0x61, (byte) 0x73, (byte) 0x73, (byte) 0x68, (byte) 0x70, (byte) 0x0D,
                (byte) 0x32, (byte) 0x67, (byte) 0x69, (byte) 0x70, (byte) 0x6F, (byte) 0x0F,
                (byte) 0x62, (byte) 0x65, (byte) 0x6E, (byte) 0x70, (byte) 0x6F, (byte) 0x73,
                (byte) 0x63, (byte) 0x61, (byte) 0x63, (byte) 0x62, (byte) 0x6C, (byte) 0x6F,
                (byte) 0x34, (byte) 0x6D, (byte) 0x6F, (byte) 0x63, (byte) 0x75, (byte) 0x6D,
                (byte) 0x70, (byte) 0x6F, (byte) 0x6E, (byte) 0x72, (byte) 0x6F, (byte) 0x61,
                (byte) 0xEA, (byte) 0xB0, (byte) 0x80, (byte) 0x6A, (byte) 0x61, (byte) 0x0D,
                (byte) 0x6C, (byte) 0x80, (byte) 0x3C, (byte) 0x61, (byte) 0x6C, (byte) 0x6C,
                (byte) 0x67, (byte) 0x61, (byte) 0x72, (byte) 0x72, (byte) 0x69, (byte) 0x6F,
                (byte) 0x62, (byte) 0x69, (byte) 0x6D, (byte) 0x72, (byte) 0x65, (byte) 0x67,
                (byte) 0x21, (byte) 0x69, (byte) 0x0D, (byte) 0x21, (byte) 0x20, (byte) 0x22,
                (byte) 0x61, (byte) 0x6D, (byte) 0x0D, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0xC3, (byte) 0x80, (byte) 0xC3, (byte) 0x61, (byte) 0x6D, (byte) 0x0D,
                (byte) 0x62, (byte) 0x61, (byte) 0x6E, (byte) 0x61, (byte) 0x73, (byte) 0x61,
                (byte) 0x63, (byte) 0x6F, (byte) 0x77, (byte) 0x63, (byte) 0x64, (byte) 0x63,
                (byte) 0x61, (byte) 0x72, (byte) 0x73, (byte) 0x62, (byte) 0x6F, (byte) 0x62,
                (byte) 0x70, (byte) 0x65, (byte) 0x64, (byte) 0x63, (byte) 0x38, (byte) 0x0D,
                (byte) 0x6D, (byte) 0x61, (byte) 0x64, (byte) 0x6B, (byte) 0x75, (byte) 0x6E,
                (byte) 0x73, (byte) 0x6C, (byte) 0x75, (byte) 0x36, (byte) 0x39, (byte) 0x0D,
                (byte) 0x62, (byte) 0x64, (byte) 0x6D, (byte) 0x70, (byte) 0x69, (byte) 0x73,
                (byte) 0x36, (byte) 0x39, (byte) 0x0D, (byte) 0x61, (byte) 0x63, (byte) 0x63,
                (byte) 0x62, (byte) 0x61, (byte) 0x6B, (byte) 0xC4, (byte) 0x8D, (byte) 0x75,
                (byte) 0x21, (byte) 0x20, (byte) 0x27, (byte) 0x30, (byte) 0x31, (byte) 0x32,
                (byte) 0x61, (byte) 0x64, (byte) 0x6D, (byte) 0x37, (byte) 0x33, (byte) 0x37,
                (byte) 0x21, (byte) 0x22, (byte) 0x23, (byte) 0x2D, (byte) 0xE8, (byte) 0x83,
                (byte) 0x62, (byte) 0x6F, (byte) 0x6D, (byte) 0x63, (byte) 0x75, (byte) 0x0D,
                (byte) 0x62, (byte) 0x75, (byte) 0x67, (byte) 0x62, (byte) 0x79, (byte) 0x6D,
                (byte) 0x30, (byte) 0x6F, (byte) 0x0D, (byte) 0x30, (byte) 0x6F, (byte) 0x0D,
                (byte) 0x72, (byte) 0x65, (byte) 0x63, (byte) 0x6C, (byte) 0x6F, (byte) 0x6F,
                (byte) 0x54, (byte) 0x45, (byte) 0x52, (byte) 0x45, (byte) 0x4E, (byte) 0x44,
                (byte) 0x45, (byte) 0x6E, (byte) 0x64, (byte) 0x45, (byte) 0x6C, (byte) 0x20,
                (byte) 0x54, (byte) 0x45, (byte) 0x52, (byte) 0xD0, (byte) 0xA3, (byte) 0xD0,
                (byte) 0xD0, (byte) 0x9B, (byte) 0xD0, (byte) 0xEC, (byte) 0x8B, (byte) 0x9C,
                (byte) 0x43, (byte) 0x6F, (byte) 0x6E, (byte) 0x09, (byte) 0x0D, (byte) 0x0A,
                (byte) 0x4C, (byte) 0x45, (byte) 0x41,
                };
        
        
        for (int i = 0; i < magicText3Wide.length; i += 3)
        {
            byte[] temp = Arrays.copyOfRange(magicText3Wide, i, i + 3);
            list.add(new ByteArrayWrapper(temp));
        }
        
        return list;
    }
    
    public static boolean isProbableIDX(byte[] data)
    {
        return !isSame(data[0], (byte) 0x00) && isSame(data[1], (byte) 0x00) && isSame(data[2], (byte) 0x00) && isSame(data[3], (byte) 0x00);
    }
    
    public static boolean isProbable3DModelStuff(byte[] data)
    {
        return isSame(data[2], (byte) 0x00) && isSame(data[3], (byte) 0x00);
    }
    
    //</editor-fold>
}
