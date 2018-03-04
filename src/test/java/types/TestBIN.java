package types;

import com.google.gson.reflect.TypeToken;
import no.stelar7.cdragon.types.bin.BINParser;
import no.stelar7.cdragon.types.bin.data.BINFile;
import no.stelar7.cdragon.util.handlers.*;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class TestBIN
{
    BINParser parser = new BINParser();
    
    @Test
    public void testBIN()
    {
        Path file = UtilHandler.DOWNLOADS_FOLDER.resolve("parser_test\\611d601b17222a88.bin");
        System.out.println("Parsing: " + file.toString());
        
        BINFile data = parser.parse(file);
        System.out.println(data.toJson());
    }
    
    @Test
    public void testWEB() throws IOException
    {
        Path path = UtilHandler.DOWNLOADS_FOLDER.resolve("decompressed\\Zoe");
        Files.walkFileTree(path, new SimpleFileVisitor<>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
            {
                if (file.toString().endsWith(".bin"))
                {
                    BINFile bf = parser.parse(file);
                    System.out.println(bf.toJson());
                    System.out.println();
                }
                
                return FileVisitResult.CONTINUE;
            }
        });
    }
    
    @Test
    public void testGenerateBINHash() throws IOException
    {
        Path path = UtilHandler.DOWNLOADS_FOLDER.resolve("grep.log");
        list = new HashSet<>(Files.readAllLines(path, StandardCharsets.UTF_8));
        buildStrings(pool, 50);
    }
    
    char[]      pool = new char[]{' ', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};
    Set<String> list = new HashSet<>();
    
    
    public void buildStrings(char[] root, int length) throws IOException
    {
        int[]  pos   = new int[length];
        char[] combo = new char[length];
        for (int i = 0; i < length; i++)
        {
            combo[i] = root[0];
        }
        
        while (true)
        {
            String prefix = String.valueOf(combo).replace(" ", "");
            if (prefix.length() > 7)
            {
                if (prefix.contains("a") || prefix.contains("e") || prefix.contains("i") || prefix.contains("o") || prefix.contains("u"))
                {
                    checkResult(prefix);
                }
            }
            
            int place = length - 1;
            while (place >= 0)
            {
                if (++pos[place] == root.length)
                {
                    pos[place] = 0;
                    combo[place] = root[0];
                    place--;
                } else
                {
                    combo[place] = root[pos[place]];
                    break;
                }
            }
            if (place < 0)
            {
                break;
            }
        }
    }
    
    
    public void checkResult(String prefix) throws IOException
    {
        if (HashHandler.hasBINHash(prefix))
        {
            return;
        }
        
        String hash = String.valueOf(HashHandler.generateBINHash(prefix));
        if (list.contains(hash))
        {
            String out = String.format("\"%s\": \"%s\"%n", hash, prefix);
            Files.write(UtilHandler.DOWNLOADS_FOLDER.resolve("grep.res"), out.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        }
    }
    
    private void splitResultToFile() throws IOException
    {
        
        Path          newHashStore = UtilHandler.DOWNLOADS_FOLDER.resolve("grep - kopi.res");
        List<String>  lines        = Files.readAllLines(newHashStore);
        StringBuilder sb           = new StringBuilder("{\n");
        
        for (int i = 1; i < lines.size() - 1; i++)
        {
            String line = lines.get(i);
            String key  = line.substring(line.indexOf('"') + 1);
            key = key.substring(0, key.indexOf('"'));
            
            String value = line.substring(line.indexOf('"') + key.length() + 4);
            value = value.substring(value.indexOf('"') + 1);
            value = value.substring(0, value.indexOf('"'));
            
            sb.append('"').append(value).append('"').append(": ").append('"').append(key).append('"').append(',').append("\n");
        }
        sb.reverse().deleteCharAt(1).reverse();
        sb.append("}");
        
        Files.write(UtilHandler.DOWNLOADS_FOLDER.resolve("words_fixed.log"), sb.toString().getBytes(StandardCharsets.UTF_8));
    }
    
    @Test
    public void testForWords() throws IOException
    {
        splitResultToFile();
        
        String text = UtilHandler.readAsString(UtilHandler.DOWNLOADS_FOLDER.resolve("words_fixed.log"));
        Type   type = new TypeToken<Map<String, String>>() {}.getType();
        
        Map<String, String> hashes = UtilHandler.getGson().fromJson(text, type);
        
        StandardOpenOption[] opens = new StandardOpenOption[]{StandardOpenOption.APPEND, StandardOpenOption.CREATE, StandardOpenOption.WRITE};
        
        hashes.forEach((k, v) -> {
            List<List<String>> result = UtilHandler.searchDictionary(k);
            if (!result.isEmpty())
            {
                try
                {
                    Files.write(UtilHandler.DOWNLOADS_FOLDER.resolve("words.log"), (k + ": " + result + "\n").getBytes(StandardCharsets.UTF_8), opens);
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        });
        
    }
    
    
    @Test
    public void testClientBIN() throws IOException
    {
        Path       extractPath = UtilHandler.DOWNLOADS_FOLDER.resolve("binfiles");
        Path       rito        = UtilHandler.DOWNLOADS_FOLDER.resolve("temp");
        List<Path> paths       = new ArrayList<>();
        
        Files.walkFileTree(rito, new SimpleFileVisitor<>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
            {
                paths.add(file);
                return FileVisitResult.CONTINUE;
            }
        });
        
        Comparator<Path> c = (cA, cB) -> {
            try
            {
                return Files.size(cA) < Files.size(cB) ? 1 : -1;
            } catch (IOException e)
            {
                e.printStackTrace();
                return 0;
            }
        };
        
        paths.sort(c);
        
        for (Path path : paths)
        {
            try
            {
                System.out.println("Parsing file: " + path);
                BINFile parsed = parser.parse(path);
                if (parsed == null)
                {
                    continue;
                }
                
                Files.createDirectories(extractPath);
                byte[] data = FileTypeHandler.makePrettyJson(parsed.toJson().getBytes(StandardCharsets.UTF_8));
                Files.write(extractPath.resolve(UtilHandler.pathToFilename(path) + ".json"), data);
            } catch (RuntimeException ex)
            {
                // ignore it
                System.out.println(ex.getMessage());
            }
        }
    }
}
