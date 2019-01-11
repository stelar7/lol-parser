package types.filetypes;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;
import no.stelar7.cdragon.types.bin.BINParser;
import no.stelar7.cdragon.types.bin.data.*;
import no.stelar7.cdragon.util.NaturalOrderComparator;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.types.*;
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
        Path file = UtilHandler.DOWNLOADS_FOLDER.resolve("cdragon/skin14.bin");
        /*
        Files.walkFileTree(UtilHandler.DOWNLOADS_FOLDER.resolve("bin"), new SimpleFileVisitor<>()
        
        {
            
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
            {
                System.out.println("Parsing: " + file.toString());
                BINFile data = parser.parse(file);
                System.out.println(data.toJson());
                return FileVisitResult.CONTINUE;
            }
        });
         */
        BINFile data = parser.parse(file);
        System.out.println(data.toJson());
    }
    
    @Test
    public void testBINCompare()
    {
        Path file  = UtilHandler.DOWNLOADS_FOLDER.resolve("anivia.bin");
        Path file2 = UtilHandler.DOWNLOADS_FOLDER.resolve("ashe.bin");
        Path file3 = UtilHandler.DOWNLOADS_FOLDER.resolve("aatrox.bin");
        
        BINFile data  = parser.parse(file);
        BINFile data2 = parser.parse(file2);
        BINFile data3 = parser.parse(file3);
        
        JsonElement ani = UtilHandler.getJsonParser().parse(data.toJson()).getAsJsonObject().get("CharacterRecord");
        JsonElement ash = UtilHandler.getJsonParser().parse(data2.toJson()).getAsJsonObject().get("CharacterRecord");
        JsonElement aat = UtilHandler.getJsonParser().parse(data3.toJson()).getAsJsonObject().get("CharacterRecord");
        
        System.out.println();
        System.out.println(ani);
        System.out.println();
        System.out.println(ash);
        System.out.println();
        System.out.println(aat);
    }
    
    @Test
    public void testBINLinkedFileHash() throws IOException
    {
        Path                         file       = UtilHandler.DOWNLOADS_FOLDER.resolve("temp");
        Set<Vector2<String, String>> dupRemover = new HashSet<>();
        Files.walkFileTree(file, new SimpleFileVisitor<>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
            {
                if (!file.toString().endsWith(".bin"))
                {
                    return FileVisitResult.CONTINUE;
                }
                
                BINFile data    = parser.parse(file);
                String  ignored = "DATA/";
                for (String link : data.getLinkedFiles())
                {
                    if (link.startsWith("DATA/Characters") || link.substring(ignored.length()).indexOf('_') == -1)
                    {
                        String linkVal = link.toLowerCase(Locale.ENGLISH);
                        dupRemover.add(new Vector2<>(HashHandler.computeXXHash64(linkVal), linkVal));
                    } else
                    {
                        String character = link.substring(ignored.length(), link.indexOf('_'));
                        String ext       = link.substring(link.lastIndexOf("."));
                        
                        String afterChar = link.substring(ignored.length() + character.length() + 1);
                        
                        String skinString = afterChar.substring(0, afterChar.length() - ext.length());
                        while (!skinString.isEmpty())
                        {
                            if (skinString.indexOf('_') == -1)
                            {
                                break;
                            }
                            String folder = skinString.substring(0, skinString.indexOf('_'));
                            skinString = skinString.substring(folder.length() + 1);
                            
                            String skin;
                            
                            int underIndex = skinString.indexOf('_');
                            if (underIndex != -1)
                            {
                                skin = skinString.substring(0, underIndex);
                                skinString = skinString.substring(skin.length() + 1);
                            } else
                            {
                                skin = skinString;
                                skinString = "";
                            }
                            
                            String result = String.format("data/characters/%s/%s/%s%s", character, folder, skin, ext).toLowerCase(Locale.ENGLISH);
                            dupRemover.add(new Vector2<>(HashHandler.computeXXHash64(result), result));
                        }
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
        JsonWriterWrapper jw = new JsonWriterWrapper();
        jw.beginObject();
        for (Vector2<String, String> dataPair : dupRemover)
        {
            jw.name(dataPair.getFirst()).value(dataPair.getSecond());
        }
        jw.endObject();
        Files.write(Paths.get("combined.json"), jw.toString().getBytes(StandardCharsets.UTF_8));
        testUnsplit();
    }
    
    public void testUnsplit() throws IOException
    {
        Path loadPath = Paths.get("combined.json");
        
        if (!Files.exists(loadPath))
        {
            return;
        }
        
        List<Vector2<String, String>> foundHashes = new ArrayList<>();
        
        String              txt = new String(Files.readAllBytes(loadPath), StandardCharsets.UTF_8);
        Map<String, String> val = UtilHandler.getGson().fromJson(txt, new TypeToken<Map<String, String>>() {}.getType());
        val.entrySet()
           .stream()
           .map(Vector2::new)
           .forEach(foundHashes::add);
        
        HashHandler.getWadHashes("champions")
                   .entrySet()
                   .stream()
                   .map(Vector2::new)
                   .forEach(data -> {
                       if (!foundHashes.contains(data))
                       {
                           foundHashes.add(data);
                       }
                   });
        
        foundHashes.sort(Comparator.comparing(Vector2::getSecond, new NaturalOrderComparator()));
        
        StringBuilder sb = new StringBuilder("{\n");
        for (Vector2<String, String> pair : foundHashes)
        {
            sb.append("\t\"").append(pair.getFirst()).append("\": \"").append(pair.getSecond()).append("\",\n");
        }
        sb.reverse().delete(0, 2).reverse().append("\n}");
        Files.write(HashHandler.WAD_HASH_STORE.resolve("champions" + ".json"), sb.toString().getBytes(StandardCharsets.UTF_8));
        
        Files.deleteIfExists(loadPath);
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
        List<String> names = Files.readAllLines(UtilHandler.DOWNLOADS_FOLDER.resolve("BinHashes.txt"));
        System.out.println(HashHandler.getBinHashes().size());
        names.forEach(n -> {
            long hash = HashHandler.computeBINHash(n);
            HashHandler.getBinHashes().put(hash, n);
        });
        
        sortHashes();
        System.out.println(HashHandler.getBinHashes().size());
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
        Path rito = UtilHandler.DOWNLOADS_FOLDER.resolve("temp");
        
        List<Path> paths = new ArrayList<>();
        Files.walkFileTree(rito, new SimpleFileVisitor<>()
        {
            
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
            {
                if (file.toString().endsWith(".bin"))
                {
                    BINFile parsed = parser.parse(file);
                    if (parsed == null)
                    {
                        throw new RuntimeException("invalid bin file??? (" + file.toAbsolutePath().toString() + ")");
                    }
                    
                    byte[] json = parsed.toJson().getBytes(StandardCharsets.UTF_8);
                    byte[] data = FileTypeHandler.makePrettyJson(json);
                    
                    Files.write(file.resolveSibling(UtilHandler.pathToFilename(file) + ".json"), data);
                    
                }
                return FileVisitResult.CONTINUE;
            }
        });
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
    
    @Test
    public void generateUnknownBinhashList() throws IOException
    {
        final BINParser bp = new BINParser();
        Files.walk(UtilHandler.DOWNLOADS_FOLDER.resolve("pbe"))
             .filter(a -> a.getFileName().toString().endsWith(".bin"))
             .forEach(bp::parse);
        
        String output = String.join("\n", BINParser.hashes);
        Files.write(UtilHandler.DOWNLOADS_FOLDER.resolve("binHashes.txt"), output.getBytes(StandardCharsets.UTF_8));
    }
    
}
