package no.stelar7.cdragon.util.handlers;

import com.google.gson.reflect.TypeToken;
import net.jpountz.xxhash.*;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.function.Function;

public class HashHandler
{
    private static final String HEXES    = "0123456789ABCDEF";
    private static final char[] hexArray = HEXES.toCharArray();
    
    private static final XXHashFactory xxHashFactory = XXHashFactory.fastestInstance();
    
    public static final Path WAD_HASH_STORE = Paths.get("src\\main\\resources\\hashes\\wad");
    public static final Path BIN_HASH_STORE = Paths.get("src\\main\\resources\\hashes\\bin\\binhash.json");
    public static final Path INI_HASH_STORE = Paths.get("src\\main\\resources\\hashes\\inibin\\inihash.json");
    
    private static Map<String, String> binHashNames;
    private static Map<Long, String>   iniHashNames;
    private static Map<String, String> wadHashNames;
    
    /*
    public static String toFormattedHex(byte[] bytes)
    {
        char[] hexChars = new char[bytes.length * 3];
        for (int j = 0; j < bytes.length; j++)
        {
            int v = bytes[j] & 0xFF;
            hexChars[j * 3] = hexArray[v >>> 4];
            hexChars[j * 3 + 1] = hexArray[v & 0x0F];
            hexChars[j * 3 + 2] = ' ';
        }
        return new String(hexChars);
    }
    */
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
    
    public static String toHex(String str, int minLength)
    {
        return toHex(str.getBytes(StandardCharsets.UTF_8));
    }
    
    public static String toHex(Long str, int minLength)
    {
        StringBuilder pre = new StringBuilder(Long.toHexString(str).toUpperCase(Locale.ENGLISH));
        while (pre.length() > 0 && pre.charAt(0) == '0')
        {
            pre.deleteCharAt(0);
        }
        
        while (pre.length() < minLength)
        {
            pre.insert(0, "0");
        }
        
        return pre.toString();
    }
    
    public static String toHex(Long str, int minLength, int maxLength)
    {
        StringBuilder pre = new StringBuilder(Long.toHexString(str).toUpperCase(Locale.ENGLISH));
        while (pre.length() > 0 && pre.charAt(0) == '0')
        {
            pre.deleteCharAt(0);
        }
        
        while (pre.length() < minLength)
        {
            pre.insert(0, "0");
        }
        
        while (pre.length() > maxLength)
        {
            pre.deleteCharAt(0);
        }
        
        return pre.toString();
    }
    
    private static final StreamingXXHash64 hash64 = xxHashFactory.newStreamingHash64(0);
    private static final StreamingXXHash32 hash32 = xxHashFactory.newStreamingHash32(0);
    
    public static long computeXXHash64AsLong(String text)
    {
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        return computeXXHash64AsLong(data);
    }
    
    public static long computeXXHash32AsLong(String text)
    {
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        return computeXXHash32AsLong(data);
    }
    
    public static synchronized long computeXXHash64AsLong(byte[] data)
    {
        hash64.reset();
        hash64.update(data, 0, data.length);
        return hash64.getValue();
    }
    
    public static synchronized long computeXXHash32AsLong(byte[] data)
    {
        hash32.reset();
        hash32.update(data, 0, data.length);
        return hash32.getValue();
    }
    
    public static String computeXXHash64(String text)
    {
        return toHex(computeXXHash64AsLong(text), 16);
    }
    
    public static String computeXXHash32(String text)
    {
        return toHex(computeXXHash32AsLong(text), 8);
    }
    
    public static long computeFNV(String input)
    {
        String toHash = input.toLowerCase(Locale.ENGLISH);
        int    hash   = Integer.parseUnsignedInt("2166136261");
        int    mask   = Integer.parseUnsignedInt("16777619");
        
        for (int i = 0; i < toHash.length(); i++)
        {
            hash = hash * mask;
            hash = hash ^ toHash.charAt(i);
        }
        
        return Integer.toUnsignedLong(hash);
    }
    
    // bin files
    public static long computeFNV1A(String input)
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
    
    // SDBM
    public static long computeSDBMHash(String input)
    {
        //String toHash = input.toLowerCase(Locale.ENGLISH);
        String toHash = input;
        
        int hash = 0;
        for (int i = 0; i < toHash.length(); i++)
        {
            int c = toHash.charAt(i);
            hash = c + (hash << 6) + (hash << 16) - hash;
        }
        
        return Integer.toUnsignedLong(hash);
    }
    
    public static void bruteForceHash(Function<String, Long> hashFunc, List<Long> hashes, List<String> words, String prefix, String postfix, String outputName, boolean reverse)
    {
        int[] offsets = new int[20];
        Arrays.fill(offsets, -1);
        
        StringBuilder sb       = new StringBuilder();
        int           index    = 0;
        int           maxIndex = 0;
        
        try
        {
            Path outputFile = UtilHandler.CDRAGON_FOLDER.resolve(outputName);
            if (!Files.exists(outputFile))
            {
                Files.createFile(outputFile);
            }
            
            //noinspection InfiniteLoopStatement
            while (true)
            {
                int value = ++offsets[index];
                
                while (value >= words.size())
                {
                    offsets[index] = 0;
                    offsets[index + 1]++;
                    index++;
                    if (index > maxIndex)
                    {
                        maxIndex = index;
                        System.out.println("Bruteforce currently at length: " + maxIndex);
                    }
                    
                    value = offsets[index];
                }
                index = 0;
                
                for (int j = offsets.length - 1; j >= 0; j--)
                {
                    int offset = offsets[j];
                    if (offset < 0)
                    {
                        continue;
                    }
                    
                    sb.append(words.get(offset));
                }
                
                String toHash = "";
                if (reverse)
                {
                    toHash = sb.reverse().toString().replace("\0", "");
                } else
                {
                    toHash = sb.toString().replace("\0", "");
                }
                sb.setLength(0);
                
                toHash = prefix + toHash + postfix;
                
                Long output = hashFunc.apply(toHash);
                if (hashes.contains(output))
                {
                    Files.write(outputFile, (toHash + "\n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
                    System.out.println(toHash);
                }
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    public static void bruteForceHash(Function<String, Long> hashFunc, List<Long> hashes)
    {
        bruteForceHash(hashFunc, hashes, Arrays.asList(UtilHandler.ALL), "", "", "bruteforced.txt", true);
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
    
    public static long computeCCITT32(byte[] buffer)
    {
        int crc = 0;
        
        ByteBuffer data = ByteBuffer.wrap(buffer);
        
        for (int i = 0; i < buffer.length; i++)
        {
            int value          = data.get(i) & 0xFF;
            int crcLookupIndex = ((crc >> 24) & 0xFF) ^ value;
            crc = ((crc << 8) & 0xFFFFFF00) ^ CRC_LOOKUP[crcLookupIndex];
        }
        
        return Integer.toUnsignedLong(crc);
    }
    
    public static long computeCCITT32(String text)
    {
        return computeCCITT32(text.getBytes(StandardCharsets.UTF_8));
    }
    
    public static long computeELFHash(String toHash)
    {
        toHash = toHash.toLowerCase(Locale.ENGLISH);
        long hash = 0;
        long temp;
        
        for (int i = 0; i < toHash.length(); i++)
        {
            hash = (hash << 4) + toHash.charAt(i);
            temp = hash & 0xF0000000;
            
            if (temp != 0)
            {
                hash = hash ^ (temp >> 24);
                hash = hash ^ temp;
            }
        }
        
        return hash;
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
    
    
    public static String getBINHash(int hash)
    {
        long   val = Integer.toUnsignedLong(hash);
        String hex = toHex(val, 8);
        return getBinHashes().getOrDefault(hex, hex);
    }
    
    public static String getBINHash(String hash)
    {
        long   hashed = computeFNV1A(hash);
        String hex    = toHex(hashed, 8);
        return getBinHashes().getOrDefault(hex, hex);
    }
    
    public static boolean hasBINHash(String hash)
    {
        return getBinHashes().containsValue(hash);
    }
    
    public static String getINIHash(int hash)
    {
        Long val = Integer.toUnsignedLong(hash);
        return getIniHashes().getOrDefault(val, String.valueOf(val));
    }
    
    
    public static Map<String, String> getBinHashes()
    {
        if (binHashNames != null)
        {
            return binHashNames;
        }
        
        try
        {
            InputStream is        = HashHandler.class.getClassLoader().getResourceAsStream("hashes/bin");
            Scanner     s         = new Scanner(is).useDelimiter("\\A");
            String[]    fileArray = s.next().split("\n");
            for (String file : fileArray)
            {
                System.out.println("Reading: " + file);
                String sb = UtilHandler.readInternalAsString("hashes/bin/" + file);
                binHashNames = UtilHandler.getGson().fromJson(sb, new TypeToken<Map<String, String>>() {}.getType());
                if (binHashNames == null)
                {
                    throw new IOException("Failed to load hashes");
                }
            }
            
        } catch (Exception e)
        {
            binHashNames = new HashMap<>();
            System.err.println("BIN Hash file not found: " + e.getMessage());
            System.err.println("Loading from GH...");
            
            String hashBINURL = "https://github.com/stelar7/lol-parser/raw/master/src/main/resources/hashes/bin/binhash.json";
            String hashesBIN  = String.join("", WebHandler.readWeb(hashBINURL));
            
            binHashNames = UtilHandler.getGson().fromJson(hashesBIN, new TypeToken<Map<String, String>>() {}.getType());
            if (binHashNames == null)
            {
                binHashNames = new HashMap<>();
            }
        }
        
        System.out.println("Loaded known bin hashes");
        return getBinHashes();
    }
    
    public static Map<Long, String> getIniHashes()
    {
        if (iniHashNames != null)
        {
            return iniHashNames;
        }
        
        try
        {
            String sb = Files.readString(INI_HASH_STORE);
            iniHashNames = UtilHandler.getGson().fromJson(sb, new TypeToken<Map<Long, String>>() {}.getType());
            System.out.println("Loaded known bin hashes");
        } catch (IOException e)
        {
            iniHashNames = new HashMap<>();
            System.err.println("INI Hash file not found: " + e.getMessage());
        }
        
        return getIniHashes();
    }
    
    private static final Map<String, String> reverseCache = new HashMap<>();
    
    public static String getBinKeyForHash(String hash)
    {
        if (reverseCache.containsKey(hash))
        {
            return reverseCache.get(hash);
        }
        
        String value = getBinHashes().entrySet()
                                     .stream()
                                     .filter(e -> e.getValue().equalsIgnoreCase(hash))
                                     .findAny()
                                     .map(Map.Entry::getKey)
                                     .orElse(hash);
        
        reverseCache.put(hash, value);
        return value;
    }
    
    public static String getWadHash(String hash)
    {
        return getWADHashes().getOrDefault(hash, hash);
    }
    
    public static Map<String, String> getWADHashes()
    {
        if (wadHashNames == null)
        {
            System.out.println("Reading hash files!");
            wadHashNames = new HashMap<>();
            try
            {
                InputStream  is        = HashHandler.class.getClassLoader().getResourceAsStream("hashes/wad");
                Scanner      s         = new Scanner(is).useDelimiter("\\A");
                List<String> files     = new ArrayList<>();
                String[]     fileArray = s.next().split("\n");
                for (String file : fileArray)
                {
                    System.out.println("Reading: " + file);
                    String              plugin = file.substring(0, file.lastIndexOf('.'));
                    String              sb     = UtilHandler.readInternalAsString("hashes/wad/" + plugin + ".json");
                    Map<String, String> val    = UtilHandler.getGson().fromJson(sb, new TypeToken<Map<String, String>>() {}.getType());
                    if (val == null)
                    {
                        val = new HashMap<>();
                    }
                    
                    wadHashNames.putAll(val);
                }
            } catch (NoSuchElementException e)
            {
                System.err.println("WAD Hash file not found: " + e.getMessage());
                System.err.println("Loading from GH...");
                String hashGameURL = "https://github.com/stelar7/lol-parser/raw/master/src/main/resources/hashes/wad/game.json";
                String hashLCUURL  = "https://github.com/stelar7/lol-parser/raw/master/src/main/resources/hashes/wad/lcu.json";
                
                String hashesGame = String.join("", WebHandler.readWeb(hashGameURL));
                String hashesLCU  = String.join("", WebHandler.readWeb(hashLCUURL));
                
                Map<String, String> val = UtilHandler.getGson().fromJson(hashesGame, new TypeToken<Map<String, String>>() {}.getType());
                if (val == null)
                {
                    val = new HashMap<>();
                }
                wadHashNames.putAll(val);
                
                val = UtilHandler.getGson().fromJson(hashesLCU, new TypeToken<Map<String, String>>() {}.getType());
                if (val == null)
                {
                    val = new HashMap<>();
                }
                wadHashNames.putAll(val);
            }
        }
        
        return wadHashNames;
    }
    
    public static void reloadWadHashes()
    {
        wadHashNames = null;
        getWADHashes();
    }
    
    public byte[] decryptAES(byte[] input, byte[] key)
    {
        try
        {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"));
            return cipher.doFinal(input);
        } catch (GeneralSecurityException e)
        {
            e.printStackTrace();
            return null;
        }
    }
}
