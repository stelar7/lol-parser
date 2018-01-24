import no.stelar7.cdragon.types.bin.BINParser;
import no.stelar7.cdragon.types.bin.data.BINFile;
import no.stelar7.cdragon.util.*;
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
        System.out.println(data.toJSON());
    }
    
    @Test
    public void testWEB() throws IOException
    {
        Path path = Paths.get(System.getProperty("user.home"), "Downloads\\decompressed\\Zoe");
        Files.walkFileTree(path, new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
            {
                if (file.toString().endsWith(".bin"))
                {
                    BINFile bf = parser.parse(file);
                    System.out.println(bf.toJSON());
                    System.out.println();
                }
                
                return FileVisitResult.CONTINUE;
            }
        });
    }
    
    @Test
    public void testClientBIN() throws IOException
    {
        Path       extractPath = Paths.get(System.getProperty("user.home"), "Downloads", "bintemp");
        Path       rito        = Paths.get(System.getProperty("user.home"), "Downloads\\bins");
        List<Path> paths       = new ArrayList<>();
        
        Files.walkFileTree(rito, new SimpleFileVisitor<Path>()
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
                Files.createDirectories(extractPath);
                //Files.write(extractPath.resolve(UtilHandler.pathToFilename(path) + ".json.bak"), parsed.toJSON().getBytes(StandardCharsets.UTF_8));
                
                byte[]  data   = FileTypeHandler.makePrettyJson(parsed.toJSON().getBytes(StandardCharsets.UTF_8));
                Files.write(extractPath.resolve(UtilHandler.pathToFilename(path) + ".json"), data);
            } catch (RuntimeException ex)
            {
                // ignore it
                System.out.println(ex.getMessage());
            }
        }
    }
}
