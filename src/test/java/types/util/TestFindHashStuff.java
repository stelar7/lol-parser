package types.util;

import no.stelar7.cdragon.util.NaturalOrderComparator;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.types.*;
import no.stelar7.cdragon.util.types.math.Vector2;
import no.stelar7.cdragon.util.writers.JsonWriterWrapper;
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
    public void parseStringsFile() throws IOException
    {
        Path     stringPath = Paths.get("C:\\Riot Games\\exe.txt");
        String[] parts      = String.join("", Files.readAllLines(stringPath)).split(".exe: ");
        String jsonStrings = Arrays.stream(parts)
                                   .map(s -> s.split("\n")[0].split("./League")[0])
                                   .filter(s -> !s.isBlank())
                                   .collect(Collectors.joining("\n"));
        
        List<String> lines = Arrays.stream(jsonStrings.split("\n")).collect(Collectors.toList());
        Files.write(UtilHandler.DOWNLOADS_FOLDER.resolve("bins.txt"), jsonStrings.getBytes(StandardCharsets.UTF_8));
        System.out.println();
    }
    
    @Test
    public void checkNewFiles() throws IOException
    {
        Path binPath      = UtilHandler.DOWNLOADS_FOLDER.resolve("pbe");
        Path hashFile     = UtilHandler.DOWNLOADS_FOLDER.resolve("hashFile.txt");
        Path unknownFiles = UtilHandler.DOWNLOADS_FOLDER.resolve("unknownFiles.txt");
        Path newHashes    = UtilHandler.DOWNLOADS_FOLDER.resolve("newHashes.txt");
        Path fixedHashes  = UtilHandler.DOWNLOADS_FOLDER.resolve("fixedHashes.json");
        
        System.out.println("Checking bin files for strings");
        grepFiles(hashFile, binPath);
        System.out.println("Loading unknown files");
        fetchUnknownFiles(unknownFiles, binPath);
        System.out.println("Comparing hashes");
        fetchHashesForUnknownFiles(newHashes, unknownFiles, hashFile);
        System.out.println("Loading new hashes");
        fetchFilenameFromHash(fixedHashes, newHashes, hashFile);
    }
    
    public void fetchFilenameFromHash(Path output, Path newHashes, Path possible) throws IOException
    {
        Files.deleteIfExists(output);
        List<String> missing = Files.readAllLines(newHashes);
        List<String> dataList = Files.readAllLines(possible)
                                     .stream()
                                     .map(a -> a.toLowerCase(Locale.ENGLISH))
                                     .map(a -> new Pair<>(a, HashHandler.computeXXHash64(a)))
                                     .filter(a -> missing.contains(a.getB()))
                                     .filter(a -> !HashHandler.getWadHash(a.getB()).equals(a.getB()))
                                     .map(a -> String.format("\"%s\":\"%s\"", a.getB(), a.getA()))
                                     .collect(Collectors.toList());
        
        
        String data = "{\n" + String.join(",\n", dataList.stream().filter(s -> !s.isEmpty()).collect(Collectors.toSet())) + "\n}";
        Files.write(output, data.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
    }
    
    public void grepFiles(Path output, Path binPath) throws IOException
    {
        Files.deleteIfExists(output);
        Files.walkFileTree(binPath, new SimpleFileVisitor<>()
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
                    grepFile(file, output, "((?:ASSETS|DATA|LEVELS|Character)/[0-9a-zA-Z_. /-]+)");
                }
                
                /*
                if (file.toString().endsWith(".js"))
                {
                    grepFile(file, output, "((?:lol-)[0-9a-zA-Z_. /-]+)");
                }
                */
                
                return FileVisitResult.CONTINUE;
            }
        });
        
        String data = String.join("\n", Files.readAllLines(output, StandardCharsets.UTF_8).stream().filter(s -> !s.isEmpty()).collect(Collectors.toSet()));
        Files.write(output, data.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
    }
    
    public void fetchHashesForUnknownFiles(Path output, Path files, Path possible) throws IOException
    {
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
    
    public void fetchUnknownFiles(Path output, Path binPath) throws IOException
    {
        Files.deleteIfExists(output);
        Files.walkFileTree(binPath, new SimpleFileVisitor<>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
            {
                /*
                if (file.toString().contains("Champions") ||
                    file.toString().contains("FINAL") ||
                    file.toString().contains("Shipping") ||
                    file.toString().contains("Shaders") ||
                    file.toString().contains("Localized")
                )
                {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                */
                
                if (Files.isDirectory(file))
                {
                    return FileVisitResult.CONTINUE;
                }
                
                if (file.equals(output))
                {
                    return FileVisitResult.CONTINUE;
                }
                
                if (file.toString().endsWith("unknown.json"))
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
    
    public void grepFile(Path path, Path output, String pattern) throws IOException
    {
        String  data = String.join("", Files.readAllLines(path));
        Pattern p    = Pattern.compile(pattern);
        Matcher m    = p.matcher(data);
        
        if (m.results().count() > 0)
        {
            m.reset();
        }
        
        byte[] results = (m.results()
                           .map(MatchResult::group)
                           .filter(s -> s.contains("."))
                           .collect(Collectors.joining("\n"))
                          + "\n").getBytes(StandardCharsets.UTF_8);
        
        Files.write(output, results, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
    }
}
