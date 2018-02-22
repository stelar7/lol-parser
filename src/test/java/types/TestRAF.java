package types;

import no.stelar7.cdragon.types.raf.RAFParser;
import no.stelar7.cdragon.types.raf.data.RAFFile;
import no.stelar7.cdragon.util.NaturalOrderComparator;
import org.junit.Test;

import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class TestRAF
{
    @Test
    public void testClientRAF() throws Exception
    {
        RAFParser parser = new RAFParser();
        
        Path extractPath = Paths.get(System.getProperty("user.home"), "Downloads", "raf2");
        Path rito        = Paths.get("C:\\Riot Games");
        
        List<Path> paths = new ArrayList<>();
        
        Files.walkFileTree(rito, new SimpleFileVisitor<>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
            {
                if (file.getFileName().toString().endsWith(".raf"))
                {
                    paths.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        
        paths.sort(new NaturalOrderComparator());
        
        for (Path file : paths)
        {
            System.out.println("Parsing: " + file.toString());
            RAFFile parsed = parser.parse(file);
            parsed.extractFiles(extractPath);
        }
    }
}
