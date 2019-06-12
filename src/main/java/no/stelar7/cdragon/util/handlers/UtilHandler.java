package no.stelar7.cdragon.util.handlers;

import com.google.common.collect.Sets;
import com.google.gson.*;
import no.stelar7.cdragon.util.types.*;
import no.stelar7.cdragon.util.types.math.Vector2;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Predicate;
import java.util.prefs.Preferences;
import java.util.regex.*;
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
    private static JSPrettier  jsPretty;
    
    public static Path CDRAGON_FOLDER = new File("D:\\").toPath();
    
    public static String pathToFilename(Path path)
    {
        return path.getFileName().toString().substring(0, path.getFileName().toString().lastIndexOf('.'));
    }
    
    public static String getFilename(String path)
    {
        return path.substring(path.lastIndexOf('/') + 1);
    }
    
    public static String pathToFolderName(Path path)
    {
        return path.getFileName().toString();
    }
    
    public static Set<String> product(Set<String> words, int repeat)
    {
        List<Set<String>> preProduct = new ArrayList<>();
        for (int i = 0; i < repeat; i++)
        {
            preProduct.add(words);
        }
        
        Set<String> product = new HashSet<>();
        for (List<String> p : Sets.cartesianProduct(preProduct))
        {
            product.add(String.join("", p));
        }
        
        return product;
    }
    
    @SafeVarargs
    public static Set<String> product(Set<String>... words)
    {
        Set<String> product = new HashSet<>();
        for (List<String> p : Sets.cartesianProduct(words))
        {
            product.add(String.join("", p));
        }
        
        return product;
    }
    
    @SafeVarargs
    public static Set<String> product(String join, Set<String>... words)
    {
        Set<String> product = new HashSet<>();
        for (List<String> p : Sets.cartesianProduct(words))
        {
            product.add(String.join(join, p));
        }
        
        return product;
    }
    
    public static void reverse(byte[] data)
    {
        for (int i = 0; i < data.length / 2; i++)
        {
            byte temp = data[i];
            data[i] = data[data.length - i - 1];
            data[data.length - i - 1] = temp;
        }
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
            sb.append("\t\"").append(pair.getFirst()).append("\": \"").append(pair.getSecond()).append("\",\n");
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
    
    public static String readInternalAsString(String filename)
    {
        InputStream file = UtilHandler.class.getClassLoader().getResourceAsStream(filename);
        if (file == null)
        {
            return "";
        }
        
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
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(UtilHandler.CDRAGON_FOLDER.resolve(file).toFile(), true)))
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
        json = json.replace("\n", "");
        
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
    
    public static String getEnding(Path file)
    {
        return getEnding(file.toString());
    }
    
    public static String getEnding(String name)
    {
        if (name.contains("."))
        {
            return name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ENGLISH);
        }
        
        return name;
    }
    
    public static String beautifyJS(String input)
    {
        try
        {
            if (jsPretty == null)
            {
                jsPretty = new JSPrettier();
            }
            
            //return jsPretty.beautify(input);
            return input;
        } catch (Exception e)
        {
            e.printStackTrace();
            return input;
        }
    }
    
    public static Predicate<Path> IS_JSON_PREDICATE = (file) -> {
        if (Files.isDirectory(file))
        {
            return false;
        }
        
        return file.toString().endsWith(".json");
    };
    
    public static List<Path> getFilesMatchingPredicate(Path start, Predicate<Path> check)
    {
        List<Path> readMe = new ArrayList<>();
        try
        {
            Files.walkFileTree(start, new SimpleFileVisitor<>()
            {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                {
                    if (check.test(file))
                    {
                        readMe.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        return readMe;
    }
    
    public static String readPathAsString(Path path)
    {
        try
        {
            return Files.readString(path);
        } catch (IOException e)
        {
            e.printStackTrace();
            return "";
        }
    }
    
    public static Map<String, String> extractRegex(String input, String regex, String... vars)
    {
        Map<String, String> matches = new HashMap<>();
        
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(input);
        if (!m.find())
        {
            return Collections.emptyMap();
        }
        
        Arrays.stream(vars).forEach(v -> matches.put(v, m.group(v)));
        
        return matches;
    }
    
    public static String extractRegex(String input, String regex, String var)
    {
        return extractRegex(input, regex, new String[]{var}).getOrDefault(var, null);
    }
    
    public static Pair<String, String> getLCUConnectionData()
    {
        boolean      isWindows      = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("win");
        List<String> windowsCommand = Arrays.asList("WMIC", "process", "where", "name='LeagueClientUx.exe'", "get", "commandLine");
        List<String> macCommand     = Arrays.asList("ps", "x", "|", "grep", "'LeagueClientUx.exe'");
        
        String passwordRegex = "--remoting-auth-token=(?<password>[^ \\\"]+)";
        String portRegex     = "--app-port=(?<port>\\d+)";
        
        try
        {
            ProcessBuilder builder = new ProcessBuilder(isWindows ? windowsCommand : macCommand);
            Process        process = builder.start();
            BufferedReader br      = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String         line    = br.lines().collect(Collectors.joining());
            
            String password = extractRegex(line, passwordRegex, "password");
            String port     = extractRegex(line, portRegex, "port");
            
            if (password == null || port == null)
            {
                System.err.println("The league client does not appear to be running!");
                return null;
            }
            
            return new Pair<>(password, port);
            
        } catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }
    
    public static Map<String, String> getLCUAuthorizationHeader()
    {
        Pair<String, String> info    = getLCUConnectionData();
        String               val     = "riot:" + info.getA();
        String               encoded = new String(Base64.getEncoder().encode(val.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
        
        return Map.of("Authorization", "Basic " + encoded);
    }
    
    /**
     * returns true if the Nth bit is set, with 1 being the first bit.
     * (passing in 0,0 as the argument also returns true)
     *
     * @param value value to check
     * @param bit   bit to check
     * @return true if bit is set
     */
    public static boolean isBitflagSet(int value, int bit)
    {
        if (value == bit)
        {
            return true;
        }
        
        if (bit == 0)
        {
            return false;
        }
        
        return (value & (1L << bit)) != 0;
    }
}