package types;

import no.stelar7.cdragon.types.bin.BINParser;
import no.stelar7.cdragon.types.bin.data.BINFile;
import no.stelar7.cdragon.util.handlers.*;
import org.junit.Test;

import java.io.IOException;
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
        Path file = Paths.get(System.getProperty("user.home"), "Downloads\\parser_test", "Aatrox.bin");
        System.out.println("Parsing: " + file.toString());
        
        BINFile data = parser.parse(file);
        System.out.println(data.toJson());
    }
    
    @Test
    public void testWEB() throws IOException
    {
        Path path = Paths.get(System.getProperty("user.home"), "Downloads\\decompressed\\Zoe");
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
        Path path = Paths.get(System.getProperty("user.home"), "Downloads\\grep.log");
        list = new HashSet<>(Files.readAllLines(path, StandardCharsets.UTF_8));
        buildStrings(pool, 50);
    }
    
    char[]      pool = new char[]{' ', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};
    Set<String> list = new HashSet<>();
    
    
    public void buildStrings(char[] root, int length) throws IOException
    {
        // allocate an array to hold our counts:
        int[]  pos   = new int[length];
        char[] combo = new char[length];
        for (int i = 0; i < length; i++)
        {
            combo[i] = root[0];
        }
        
        while (true)
        {
            // output the current combinations:
            String prefix = String.valueOf(combo).replace(" ", "");
            checkResult(prefix);
            
            // move on to the next combination:
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
        if (UtilHandler.hasBINHash(prefix))
        {
            return;
        }
        
        String hash = String.valueOf(UtilHandler.generateBINHash(prefix));
        if (list.contains(hash))
        {
            String out = String.format("\"%s\": \"%s\"%n", hash, prefix);
            Files.write(Paths.get(System.getProperty("user.home"), "Downloads\\grep.res"), out.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        }
    }
    
    
    @Test
    public void testClientBIN() throws IOException
    {
        Path       extractPath = Paths.get(System.getProperty("user.home"), "Downloads", "binfiles");
        Path       rito        = Paths.get(System.getProperty("user.home"), "Downloads", "temp");
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
