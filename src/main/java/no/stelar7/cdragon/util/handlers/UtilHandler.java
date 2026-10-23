package no.stelar7.cdragon.util.handlers;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.Security;
import java.util.*;
import java.util.function.Predicate;
import java.util.prefs.Preferences;
import java.util.regex.*;

public final class UtilHandler
{
    
    public static final String AES_KEY = "ab5678ed8ae01d46261da83fb22ad19c4475571c50721cfd4b6f5e10242894ee";
    public static final String AES_IV = "6a100bf533ef469e0d3165fc1c71aff8";
    
    
    static
    {
        System.setProperty("joml.format", "false");
        Security.addProvider(new BouncyCastleProvider());
    }
    
    
    private UtilHandler()
    {
        // Hide public constructor
    }
    
    private static Preferences preferences;
    
    /**
     * Where downloaded and extracted game data lives, can be changed with the system property cdragon.folder
     */
    public static Path CDRAGON_FOLDER = Paths.get(System.getProperty("cdragon.folder", "C:\\cdragon"));
    
    public static String pathToFilename(Path path)
    {
        if (!path.getFileName().toString().contains("."))
        {
            return path.getFileName().toString();
        }
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
    
    public static byte[] readInternalAsBytes(String filename)
    {
        try
        {
            InputStream file = UtilHandler.class.getClassLoader().getResourceAsStream(filename);
            if (file == null)
            {
                return new byte[0];
            }
            
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            file.transferTo(bos);
            return bos.toByteArray();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        
        return new byte[0];
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
            System.out.println(path.toString());
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
    
    /**
     * Removes everything starting at the last .
     */
    public static String removeEnding(String name)
    {
        return name.substring(0, name.lastIndexOf('.'));
    }
    
    public static float scale(final float valueIn, final float baseMin, final float baseMax, final float limitMin, final float limitMax)
    {
        return ((limitMax - limitMin) * (valueIn - baseMin) / (baseMax - baseMin)) + limitMin;
    }
    
    public static boolean debug = false;
    
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
    
    public static Predicate<Path> filetypePredicate(String type)
    {
        return test -> !Files.isDirectory(test) && test.toString().endsWith(type);
    }
    
    public static Predicate<Path> IS_JSON_PREDICATE = (file) -> filetypePredicate(".json").test(file);
    
    public static Predicate<Path> IS_BIN_PREDICATE = (file) -> filetypePredicate(".bin").test(file);
    
    public static Predicate<Path> WEB_FILE_PREDICATE = (file) -> filetypePredicate(".js").test(file) || filetypePredicate(".html").test(file);
    
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
    
    public static void leftPad(StringBuilder zString, String s, int i)
    {
        while (zString.length() < 16)
        {
            zString.insert(0, "0");
        }
    }
    
    public static byte[] getBBQStringData()
    {
        return UtilHandler.readInternalAsBytes("bbq/strings.dat");
    }
    
    public static Map<String, String> getBBQClassData()
    {
        String              data = UtilHandler.readInternalAsString("bbq/classes.json");
        Map<String, String> elem = JsonHandler.getGson().fromJson(data, Map.class);
        return elem;
    }
    
    public byte[] getPAKKey()
    {
        return UtilHandler.readInternalAsBytes("pak/aes.key");
    }
}