package no.stelar7.cdragon.util.handlers;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import net.jpountz.xxhash.*;
import no.stelar7.cdragon.util.readers.types.Vector2;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
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
    private static Map<String, Map<String, String>> wadHashNames = new HashMap<>();
    
    private static Gson gson;
    
    private static       XXHashFactory xxHashFactory = XXHashFactory.fastestInstance();
    private static final char[]        hexArray      = "0123456789ABCDEF".toCharArray();
    
    public static final Path WAD_HASH_STORE = Paths.get("src\\main\\java\\no\\stelar7\\cdragon\\types\\wad\\hashes");
    public static final Path BIN_HASH_STORE = Paths.get("src\\main\\java\\no\\stelar7\\cdragon\\types\\bin\\data\\binhash.json");
    public static final Path INI_HASH_STORE = Paths.get("src\\main\\java\\no\\stelar7\\cdragon\\types\\inibin\\data\\inihash.json");
    
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
            Map<Long, String> pluginData = getGson().fromJson(sb, new TypeToken<Map<Long, String>>() {}.getType());
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
            Map<Long, String> pluginData = getGson().fromJson(sb, new TypeToken<Map<Long, String>>() {}.getType());
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
            Map<String, String> pluginData = getGson().fromJson(sb, new TypeToken<Map<String, String>>() {}.getType());
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
    
    public static <X, Y> void pairPrintout(Path outputFile, List<Vector2<X, Y>> data)
    {
        StringBuilder sb = new StringBuilder("{\n");
        for (Vector2<X, Y> pair : data)
        {
            sb.append("\t\"").append(pair.getX()).append("\": \"").append(pair.getY()).append("\",\n");
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
            Files.createDirectories(output.getParent());
            
            int          read;
            final byte[] buffer = new byte[4096];
            
            final URLConnection uc       = new URL(url).openConnection();
            long                fileSize = uc.getContentLengthLong();
            
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
    
    public static String readAsString(Path path)
    {
        StringBuilder sb = new StringBuilder();
        try
        {
            List<String> lines = Files.readAllLines(path);
            lines.forEach(l -> sb.append(l).append("\n"));
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        return sb.toString();
    }
    
    public static String readInternalAsString(String filename)
    {
        InputStream   file   = UtilHandler.class.getClassLoader().getResourceAsStream(filename);
        StringBuilder result = new StringBuilder();
        
        try (Scanner scanner = new Scanner(file))
        {
            while (scanner.hasNextLine())
            {
                String line = scanner.nextLine();
                result.append(line).append("\n");
            }
        }
        
        return result.toString();
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
    
    public static Gson getGson()
    {
        if (gson == null)
        {
            gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        }
        
        return gson;
    }
    
    public static boolean isOutsideRange(int x, int min, int max)
    {
        return x < min || x >= max;
    }
    
    
    public static int iLog(int i)
    {
        int ret = 0;
        
        while (i != 0)
        {
            ret++;
            i >>= 1;
        }
        return ret;
    }
    
    private static final String HEXES = "0123456789ABCDEF";
    
    public static String getHex(byte[] raw)
    {
        final StringBuilder hex = new StringBuilder();
        for (final byte b : raw)
        {
            hex.append("0x").append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F))).append(", ");
        }
        hex.reverse().deleteCharAt(0).deleteCharAt(0).reverse();
        
        return hex.toString();
    }
    
    public static long computeCCITT32(byte[] buffer, int size)
    {
        int crc = 0;
        
        ByteBuffer data = ByteBuffer.wrap(buffer);
        
        for (int i = 0; i < size; i++)
        {
            int value          = data.get(i) & 0xFF;
            int crcLookupIndex = ((crc >> 24) & 0xFF) ^ value;
            crc = ((crc << 8) & 0xFFFFFF00) ^ CRC_LOOKUP[crcLookupIndex];
        }
        
        return Integer.toUnsignedLong(crc);
    }
    
    private static final int[] CRC_LOOKUP = new int[]{
            0x00000000, 0x04C11DB7, 0x09823B6E, 0x0D4326D9,
            0x130476DC, 0x17C56B6B, 0x1A864DB2, 0x1E475005,
            0x2608EDB8, 0x22C9F00F, 0x2F8AD6D6, 0x2B4BCB61,
            0x350C9B64, 0x31CD86D3, 0x3C8EA00A, 0x384FBDBD,
            0x4C11DB70, 0x48D0C6C7, 0x4593E01E, 0x4152FDA9,
            0x5F15ADAC, 0x5BD4B01B, 0x569796C2, 0x52568B75,
            0x6A1936C8, 0x6ED82B7F, 0x639B0DA6, 0x675A1011,
            0x791D4014, 0x7DDC5DA3, 0x709F7B7A, 0x745E66CD,
            0x9823B6E0, 0x9CE2AB57, 0x91A18D8E, 0x95609039,
            0x8B27C03C, 0x8FE6DD8B, 0x82A5FB52, 0x8664E6E5,
            0xBE2B5B58, 0xBAEA46EF, 0xB7A96036, 0xB3687D81,
            0xAD2F2D84, 0xA9EE3033, 0xA4AD16EA, 0xA06C0B5D,
            0xD4326D90, 0xD0F37027, 0xDDB056FE, 0xD9714B49,
            0xC7361B4C, 0xC3F706FB, 0xCEB42022, 0xCA753D95,
            0xF23A8028, 0xF6FB9D9F, 0xFBB8BB46, 0xFF79A6F1,
            0xE13EF6F4, 0xE5FFEB43, 0xE8BCCD9A, 0xEC7DD02D,
            0x34867077, 0x30476DC0, 0x3D044B19, 0x39C556AE,
            0x278206AB, 0x23431B1C, 0x2E003DC5, 0x2AC12072,
            0x128E9DCF, 0x164F8078, 0x1B0CA6A1, 0x1FCDBB16,
            0x018AEB13, 0x054BF6A4, 0x0808D07D, 0x0CC9CDCA,
            0x7897AB07, 0x7C56B6B0, 0x71159069, 0x75D48DDE,
            0x6B93DDDB, 0x6F52C06C, 0x6211E6B5, 0x66D0FB02,
            0x5E9F46BF, 0x5A5E5B08, 0x571D7DD1, 0x53DC6066,
            0x4D9B3063, 0x495A2DD4, 0x44190B0D, 0x40D816BA,
            0xACA5C697, 0xA864DB20, 0xA527FDF9, 0xA1E6E04E,
            0xBFA1B04B, 0xBB60ADFC, 0xB6238B25, 0xB2E29692,
            0x8AAD2B2F, 0x8E6C3698, 0x832F1041, 0x87EE0DF6,
            0x99A95DF3, 0x9D684044, 0x902B669D, 0x94EA7B2A,
            0xE0B41DE7, 0xE4750050, 0xE9362689, 0xEDF73B3E,
            0xF3B06B3B, 0xF771768C, 0xFA325055, 0xFEF34DE2,
            0xC6BCF05F, 0xC27DEDE8, 0xCF3ECB31, 0xCBFFD686,
            0xD5B88683, 0xD1799B34, 0xDC3ABDED, 0xD8FBA05A,
            0x690CE0EE, 0x6DCDFD59, 0x608EDB80, 0x644FC637,
            0x7A089632, 0x7EC98B85, 0x738AAD5C, 0x774BB0EB,
            0x4F040D56, 0x4BC510E1, 0x46863638, 0x42472B8F,
            0x5C007B8A, 0x58C1663D, 0x558240E4, 0x51435D53,
            0x251D3B9E, 0x21DC2629, 0x2C9F00F0, 0x285E1D47,
            0x36194D42, 0x32D850F5, 0x3F9B762C, 0x3B5A6B9B,
            0x0315D626, 0x07D4CB91, 0x0A97ED48, 0x0E56F0FF,
            0x1011A0FA, 0x14D0BD4D, 0x19939B94, 0x1D528623,
            0xF12F560E, 0xF5EE4BB9, 0xF8AD6D60, 0xFC6C70D7,
            0xE22B20D2, 0xE6EA3D65, 0xEBA91BBC, 0xEF68060B,
            0xD727BBB6, 0xD3E6A601, 0xDEA580D8, 0xDA649D6F,
            0xC423CD6A, 0xC0E2D0DD, 0xCDA1F604, 0xC960EBB3,
            0xBD3E8D7E, 0xB9FF90C9, 0xB4BCB610, 0xB07DABA7,
            0xAE3AFBA2, 0xAAFBE615, 0xA7B8C0CC, 0xA379DD7B,
            0x9B3660C6, 0x9FF77D71, 0x92B45BA8, 0x9675461F,
            0x8832161A, 0x8CF30BAD, 0x81B02D74, 0x857130C3,
            0x5D8A9099, 0x594B8D2E, 0x5408ABF7, 0x50C9B640,
            0x4E8EE645, 0x4A4FFBF2, 0x470CDD2B, 0x43CDC09C,
            0x7B827D21, 0x7F436096, 0x7200464F, 0x76C15BF8,
            0x68860BFD, 0x6C47164A, 0x61043093, 0x65C52D24,
            0x119B4BE9, 0x155A565E, 0x18197087, 0x1CD86D30,
            0x029F3D35, 0x065E2082, 0x0B1D065B, 0x0FDC1BEC,
            0x3793A651, 0x3352BBE6, 0x3E119D3F, 0x3AD08088,
            0x2497D08D, 0x2056CD3A, 0x2D15EBE3, 0x29D4F654,
            0xC5A92679, 0xC1683BCE, 0xCC2B1D17, 0xC8EA00A0,
            0xD6AD50A5, 0xD26C4D12, 0xDF2F6BCB, 0xDBEE767C,
            0xE3A1CBC1, 0xE760D676, 0xEA23F0AF, 0xEEE2ED18,
            0xF0A5BD1D, 0xF464A0AA, 0xF9278673, 0xFDE69BC4,
            0x89B8FD09, 0x8D79E0BE, 0x803AC667, 0x84FBDBD0,
            0x9ABC8BD5, 0x9E7D9662, 0x933EB0BB, 0x97FFAD0C,
            0xAFB010B1, 0xAB710D06, 0xA6322BDF, 0xA2F33668,
            0xBCB4666D, 0xB8757BDA, 0xB5365D03, 0xB1F740B4
    };
    
    public static String replaceEnding(String name, String original, String other)
    {
        if (!name.endsWith(original))
        {
            return name;
        }
        
        String pre = name.substring(0, name.lastIndexOf('.') + 1);
        return pre + other;
    }
    
    public static float scale(final float valueIn, final float baseMin, final float baseMax, final float limitMin, final float limitMax)
    {
        return ((limitMax - limitMin) * (valueIn - baseMin) / (baseMax - baseMin)) + limitMin;
    }
    
    public static void logToFile(String file, String text)
    {
        Path out = Paths.get("C:\\Users\\Steffen\\Downloads").resolve(file);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(out.toFile(), true)))
        {
            bw.append(text);
            bw.newLine();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    public static String bufferToString(ByteBuffer data)
    {
        StringBuilder result = new StringBuilder("(");
        
        data.mark();
        
        while (data.remaining() > 0)
        {
            result.append(data.get()).append(", ");
        }
        
        result.reverse().deleteCharAt(0).deleteCharAt(0).reverse().append(")");
        data.reset();
        
        return result.toString();
    }
}
