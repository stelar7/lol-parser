package types.filetypes;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;
import no.stelar7.cdragon.types.bin.BINParser;
import no.stelar7.cdragon.types.bin.data.BINFile;
import no.stelar7.cdragon.util.handlers.*;
import org.javers.core.*;
import org.junit.*;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.Map.Entry;

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
    public void testWriteBin()
    {
        Path file  = UtilHandler.DOWNLOADS_FOLDER.resolve("parser_test\\611d601b17222a88.bin");
        Path file2 = UtilHandler.DOWNLOADS_FOLDER.resolve("parser_test\\bintest.bin");
        
        BINFile data = parser.parse(file);
        data.write(file2);
        
        BINFile data2 = parser.parse(file2);
        
        Javers jav = JaversBuilder.javers().build();
        Assert.assertTrue("Input and output .bin file is different...", jav.compare(data, data2).getChanges().isEmpty());
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
        
        String hash = String.valueOf(HashHandler.computeBINHash(prefix));
        if (list.contains(hash))
        {
            String out = String.format("\"%s\": \"%s\"%n", hash, prefix);
            Files.write(UtilHandler.DOWNLOADS_FOLDER.resolve("grep.res"), out.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        }
    }
    
    @Test
    public void testAddFromFile() throws IOException
    {
        List<String> names = Files.readAllLines(UtilHandler.DOWNLOADS_FOLDER.resolve("binhashes.txt"));
        names.forEach(n -> {
            long hash = HashHandler.computeBINHash(n);
            HashHandler.getBinHashes().putIfAbsent(hash, n);
        });
        
        sortHashes();
    }
    
    @Test
    public void sortHashes() throws IOException
    {
        List<Entry<Long, String>> hashes = new ArrayList<>(HashHandler.getBinHashes().entrySet());
        hashes.sort(Entry.comparingByKey());
        
        try (JsonWriter writer = new JsonWriter(new BufferedWriter(new FileWriter(HashHandler.BIN_HASH_STORE.toFile()))))
        {
            writer.setIndent("    ");
            writer.beginObject();
            hashes.forEach(e -> {
                try
                {
                    writer.name(e.getKey().toString()).value(e.getValue().replace("\\", "\\\\"));
                } catch (IOException e1)
                {
                    e1.printStackTrace();
                }
            });
            writer.endObject();
        }
    }
    
    @Test
    public void splitResultToFile() throws IOException
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
        String text = UtilHandler.readAsString(UtilHandler.DOWNLOADS_FOLDER.resolve("words_fixed.log"));
        Type   type = new TypeToken<LinkedHashMap<String, String>>() {}.getType();
        
        Map<String, String>          hashes = UtilHandler.getGson().fromJson(text, type);
        Queue<Entry<String, String>> toRead = new ArrayDeque<>(hashes.entrySet());
        
        BufferedWriter bw = new BufferedWriter(new FileWriter(UtilHandler.DOWNLOADS_FOLDER.resolve("output.log").toFile(), true));
        
        while (true)
        {
            Entry<String, String> value = toRead.poll();
            
            if (value == null)
            {
                break;
            }
            
            String k = value.getKey();
            String v = value.getValue();
            
            List<List<String>> result = UtilHandler.searchDictionary(k);
            if (!result.isEmpty())
            {
                bw.write(String.format("%s: %s: %s%n", v, k, result));
            }
        }
        
        bw.flush();
    }
    
    
    @Test
    public void testClientBIN() throws IOException
    {
        Path extractPathBin  = UtilHandler.DOWNLOADS_FOLDER.resolve("bin");
        Path extractPathJson = UtilHandler.DOWNLOADS_FOLDER.resolve("json");
        Path rito            = UtilHandler.DOWNLOADS_FOLDER.resolve("temp");
        
        List<Path> paths = new ArrayList<>();
        Files.walkFileTree(rito, new SimpleFileVisitor<>()
        {
            
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
            {
                if (file.toString().endsWith(".bin"))
                {
                    paths.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        
        Comparator<Path> c = (cA, cB) -> {
            try
            {
                return Files.size(cA) > Files.size(cB) ? -1 : 1;
            } catch (IOException e)
            {
                e.printStackTrace();
                return 0;
            }
        };
        
        paths.sort(c);
        
        Files.createDirectories(extractPathBin);
        Files.createDirectories(extractPathJson);
        for (Path path : paths)
        {
            try
            {
                if (Files.exists(extractPathJson.resolve(UtilHandler.pathToFilename(path) + ".json")))
                {
                    continue;
                }
                
                BINFile parsed = parser.parse(path);
                if (parsed == null)
                {
                    throw new RuntimeException("invalid bin file???");
                }
                
                byte[] json = parsed.toJson().getBytes(StandardCharsets.UTF_8);
                byte[] data = FileTypeHandler.makePrettyJson(json);
                
                Files.write(extractPathBin.resolve(UtilHandler.pathToFilename(path) + ".bin"), Files.readAllBytes(path));
                Files.write(extractPathJson.resolve(UtilHandler.pathToFilename(path) + ".json"), data);
                
            } catch (RuntimeException ex)
            {
                System.out.println("Failed to parse file: " + path);
                System.out.println(ex.getMessage());
            }
        }
    }
    
    @Test
    public void testBinForARType() throws IOException
    {
        Path folder = UtilHandler.DOWNLOADS_FOLDER.resolve("binfiles");
        
        Map<String, List<String>> typeMap = new HashMap<>();
        
        Files.walkFileTree(folder, new SimpleFileVisitor<>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
            {
                JsonObject elem = UtilHandler.getJsonParser().parse(UtilHandler.readAsString(file)).getAsJsonObject();
                
                if (!elem.has("CharacterRecord"))
                {
                    return FileVisitResult.CONTINUE;
                }
                
                JsonArray recFull = elem.getAsJsonArray("CharacterRecord");
                for (JsonElement reco : recFull)
                {
                    JsonObject rec  = reco.getAsJsonObject();
                    JsonObject unki = rec.getAsJsonObject(rec.keySet().toArray(new String[0])[0]);
                    
                    if (!unki.has("PrimaryAbilityResource"))
                    {
                        return FileVisitResult.CONTINUE;
                    }
                    
                    JsonObject  par          = unki.getAsJsonObject("PrimaryAbilityResource");
                    JsonObject  unk          = par.getAsJsonObject(par.keySet().toArray(new String[0])[0]);
                    JsonElement keyContainer = unk.get("arType");
                    
                    String key;
                    
                    if (keyContainer == null)
                    {
                        key = "null";
                    } else
                    {
                        key = keyContainer.getAsString();
                    }
                    
                    
                    String val = unki.get("mCharacterName").getAsString();
                    
                    List<String> values = typeMap.getOrDefault(key, new ArrayList<>());
                    values.add(val);
                    typeMap.put(key, values);
                    
                }
                
                return FileVisitResult.CONTINUE;
            }
        });
        
        System.out.println(UtilHandler.getGson().toJson(typeMap));
    }
    
}
