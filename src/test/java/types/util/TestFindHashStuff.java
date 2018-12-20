package types.util;

import no.stelar7.cdragon.util.NaturalOrderComparator;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.types.*;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

public class TestFindHashStuff
{
    @Test
    public void checkNewFiles() throws IOException
    {
        System.out.println("Checking bin files for strings");
        grepFiles();
        System.out.println("Loading unknown files");
        fetchUnknownFiles();
        System.out.println("Comparing hashes");
        fetchHashesForUnknownFiles();
        System.out.println("Loading new hashes");
        fetchFilenameFromHash();
        saveFoundHashes();
    }
    
    public void saveFoundHashes() throws IOException
    {
        Path loadPath = UtilHandler.DOWNLOADS_FOLDER.resolve("fixedHashes");
        List<String> lines = Files.readAllLines(loadPath)
                                  .stream()
                                  .filter(x -> !x.equalsIgnoreCase("{"))
                                  .filter(x -> !x.equalsIgnoreCase("}"))
                                  .filter(x -> !x.equalsIgnoreCase("{}"))
                                  .collect(Collectors.toList());
        
        Set<String> changedPlugins = new HashSet<>();
        for (String u : lines)
        {
            String[] parts  = u.split("\":\"");
            String   first  = parts[0].replaceAll("[\"]", "").trim();
            String   second = parts[1].replaceAll("[\",]", "").trim();
            
            String[] plugin = {second.substring(0, second.indexOf('/'))};
            
            if (second.startsWith("assets") || second.startsWith("data"))
            {
                plugin[0] = "champions";
            }
            
            Map<String, String> hashes = HashHandler.getWadHashes(plugin[0]);
            hashes.computeIfAbsent(first, (key) -> {
                changedPlugins.add(plugin[0]);
                return second;
            });
        }
        
        
        for (String plugin : changedPlugins)
        {
            System.out.println("Found new hashes for: " + plugin);
            Set<Vector2<String, String>> foundHashes = new HashSet<>();
            
            System.out.println("Loading currently known hashes");
            HashHandler.getWadHashes(plugin).forEach((k, v) -> foundHashes.add(new Vector2<>(k, v)));
            
            List<Vector2<String, String>> allHashes = new ArrayList<>(foundHashes);
            System.out.println("Sorting hashes");
            allHashes.sort(Comparator.comparing(Vector2::getSecond, new NaturalOrderComparator()));
            
            System.out.println("Writing hashes");
            JsonWriterWrapper jsonWriter = new JsonWriterWrapper();
            jsonWriter.beginObject();
            for (Vector2<String, String> pair : allHashes)
            {
                jsonWriter.name(pair.getFirst()).value(pair.getSecond());
            }
            jsonWriter.endObject();
            Files.write(HashHandler.WAD_HASH_STORE.resolve(plugin + ".json"), jsonWriter.toString().getBytes(StandardCharsets.UTF_8));
        }
    }
    
    public void fetchFilenameFromHash() throws IOException
    {
        Path output = UtilHandler.DOWNLOADS_FOLDER.resolve("fixedHashes");
        Files.deleteIfExists(output);
        
        Path newHashes = UtilHandler.DOWNLOADS_FOLDER.resolve("newHashes");
        Path possible  = UtilHandler.DOWNLOADS_FOLDER.resolve("hashFile");
        
        List<String> missing = Files.readAllLines(newHashes);
        
        List<String> dataList = Files.readAllLines(possible)
                                     .stream()
                                     .map(a -> a.toLowerCase(Locale.ENGLISH))
                                     .map(a -> new Pair<>(a, HashHandler.computeXXHash64(a)))
                                     .filter(a -> missing.contains(a.getB()))
                                     .map(a -> String.format("\"%s\":\"%s\"", a.getB(), a.getA()))
                                     .collect(Collectors.toList());
        
        
        String data = "{\n" + String.join(",\n", dataList.stream().filter(s -> !s.isEmpty()).collect(Collectors.toSet())) + "\n}";
        Files.write(output, data.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
    }
    
    public void grepFiles() throws IOException
    {
        Path output = UtilHandler.DOWNLOADS_FOLDER.resolve("hashFile");
        Files.deleteIfExists(output);
        Files.walkFileTree(UtilHandler.DOWNLOADS_FOLDER.resolve("pbe/extracted"), new SimpleFileVisitor<>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
            {
                if (file.equals(output))
                {
                    return FileVisitResult.CONTINUE;
                }
                
                if (file.toString().endsWith(".json"))
                {
                    grepFile(file, output);
                }
                
                return FileVisitResult.CONTINUE;
            }
        });
        
        String data = String.join("\n", Files.readAllLines(output, StandardCharsets.UTF_8).stream().filter(s -> !s.isEmpty()).collect(Collectors.toSet()));
        Files.write(output, data.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
    }
    
    public void fetchHashesForUnknownFiles() throws IOException
    {
        Path output   = UtilHandler.DOWNLOADS_FOLDER.resolve("newHashes");
        Path files    = UtilHandler.DOWNLOADS_FOLDER.resolve("unknownFiles");
        Path possible = UtilHandler.DOWNLOADS_FOLDER.resolve("hashFile");
        Files.deleteIfExists(output);
        
        List<String> possibleHashes = Files.readAllLines(possible).stream()
                                           .map(s -> s.toLowerCase(Locale.ENGLISH))
                                           .map(HashHandler::computeXXHash64)
                                           .collect(Collectors.toList());
        
        List<String> outputData = Files.readAllLines(files);
        outputData.retainAll(possibleHashes);
        
        String data = String.join("\n", outputData.stream().filter(s -> !s.isEmpty()).collect(Collectors.toSet()));
        Files.write(output, data.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
    }
    
    public void fetchUnknownFiles() throws IOException
    {
        Path output = UtilHandler.DOWNLOADS_FOLDER.resolve("unknownFiles");
        Files.deleteIfExists(output);
        
        Files.walkFileTree(UtilHandler.DOWNLOADS_FOLDER.resolve("pbe/extracted"), new SimpleFileVisitor<>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
            {
                if (file.equals(output))
                {
                    return FileVisitResult.CONTINUE;
                }
                
                if (file.toString().contains("unknown"))
                {
                    String name = file.toString();
                    name = name.substring(name.lastIndexOf('\\') + 1);
                    name = name.substring(0, name.lastIndexOf('.'));
                    name = name + "\n";
                    Files.write(output, name.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
                }
                
                return FileVisitResult.CONTINUE;
            }
        });
        
        String data = String.join("\n", Files.readAllLines(output, StandardCharsets.UTF_8).stream().filter(s -> !s.isEmpty()).collect(Collectors.toSet()));
        Files.write(output, data.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
    }
    
    @Test
    public void listParts()
    {
        getParts();
        System.out.println();
    }
    
    Set<String> exts = null;
    
    private Set<String> getExts()
    {
        if (exts == null)
        {
            exts = HashHandler.loadAllWadHashes()
                              .values()
                              .stream()
                              .map(s -> s.substring(s.lastIndexOf('.') + 1))
                              .collect(Collectors.toSet());
        }
        
        return exts;
    }
    
    Set<String> parts = null;
    
    private Set<String> getParts()
    {
        if (parts == null)
        {
            parts = HashHandler.loadAllWadHashes()
                               .values()
                               .stream()
                               .flatMap(a -> Arrays.stream(a.split("[_/\\-.]")))
                               .collect(Collectors.toSet());
        }
        
        return parts;
    }
    
    public void grepFile(Path path, Path output) throws IOException
    {
        String  data = String.join("", Files.readAllLines(path));
        Pattern p    = Pattern.compile("((?:ASSETS|DATA|Character)/[0-9a-zA-Z_. /-]+)");
        Matcher m    = p.matcher(data);
        
        byte[] results = (m.results().map(MatchResult::group).collect(Collectors.joining("\n")) + "\n").getBytes(StandardCharsets.UTF_8);
        Files.write(output, results, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
    }
}
