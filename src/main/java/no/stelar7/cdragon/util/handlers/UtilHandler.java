package no.stelar7.cdragon.util.handlers;

import com.google.gson.*;
import no.stelar7.cdragon.util.types.Vector2;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.prefs.Preferences;
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
    
    private static Gson        gson;
    private static JsonParser  parser;
    private static Preferences preferences;
    
    public static final Path DOWNLOADS_FOLDER = Paths.get(System.getProperty("user.home"), "Downloads");
    public static final Path TYPES_FOLDER     = Paths.get("src\\main\\java\\no\\stelar7\\cdragon\\types");
    
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
    
    public static int getMaxVersion(String url, String file, int min)
    {
        int i         = min - 1;
        int failCount = 0;
        int lastGood  = -1;
        
        while (failCount < 5)
        {
            String versionAsIP = getIPFromLong(++i);
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
        
        System.out.println("Returning version: " + lastGood + " (" + getIPFromLong(lastGood) + ")");
        return lastGood;
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
    
    public static JsonParser getJsonParser()
    {
        if (parser == null)
        {
            parser = new JsonParser();
        }
        
        return parser;
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
    
    private static final boolean debug = false;
    
    public static void logToFile(String file, String text)
    {
        if (debug)
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
    
    public static String mergeTopKeysToArray(String json)
    {
        Map<String, List<JsonElement>> data = new HashMap<>();
        
        
        for (int i = 2; i < json.length(); i++)
        {
            String key = json.substring(i);
            key = key.substring(0, key.indexOf('"'));
            
            i += (key.length() + 2);
            
            int           count = 0;
            StringBuilder sb    = new StringBuilder();
            
            while (true)
            {
                char at = json.charAt(i++);
                if (at == '{')
                {
                    count++;
                }
                
                if (at == '}')
                {
                    count--;
                }
                
                sb.append(at);
                
                if (count == 0)
                {
                    List<JsonElement> list = data.getOrDefault(key, new ArrayList<>());
                    list.add(getJsonParser().parse(sb.toString()));
                    data.put(key, list);
                    i++;
                    break;
                }
            }
        }
        
        return getGson().toJson(data);
    }
    
    
    private static final String      english    = readInternalAsString("dictionary/english.txt");
    private static final Set<String> dictionary = new HashSet<>(Arrays.asList(english.split("\n")));
    
    public static List<List<String>> searchDictionary(String input)
    {
        List<List<String>> results = new ArrayList<>();
        search(input, dictionary, new Stack<>(), results);
        return results;
    }
    
    
    private static void search(String input, Set<String> dictionary, Stack<String> words, List<List<String>> results)
    {
        for (int i = 0; i < input.length(); i++)
        {
            String substring = input.substring(0, i + 1);
            
            if (dictionary.contains(substring))
            {
                words.push(substring);
                
                if (i == input.length() - 1)
                {
                    results.add(new ArrayList<>(words));
                } else
                {
                    search(input.substring(i + 1), dictionary, words, results);
                }
                
                words.pop();
            }
        }
    }
    
    public static Preferences getPreferences()
    {
        if (preferences == null)
        {
            preferences = Preferences.userNodeForPackage(UtilHandler.class);
        }
        
        return preferences;
    }
}