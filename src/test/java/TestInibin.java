import no.stelar7.cdragon.inibin.InibinParser;
import no.stelar7.cdragon.inibin.data.InibinFile;
import no.stelar7.cdragon.util.NaturalOrderComparator;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class TestInibin
{
    
    @Test
    public void testClientInibin() throws IOException
    {
        InibinParser parser = new InibinParser();
        
        Path extractPath = Paths.get(System.getProperty("user.home"), "Downloads", "inibin");
        Path rito        = Paths.get(System.getProperty("user.home"), "Downloads", "raf");
        
        List<Path> paths = new ArrayList<>();
        
        Files.walkFileTree(rito, new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
            {
                if (file.getFileName().toString().endsWith(".inibin"))
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
            InibinFile parsed = parser.parse(file);
            parsed.extractFile(extractPath);
        }
    }
    
    @Test
    public void testKindredInbin()
    {
        InibinParser parser = new InibinParser();
        
        Path file = Paths.get(System.getProperty("user.home"), "Downloads", "JannaUpgradeActive.inibin");
        
        System.out.println("Parsing: " + file.toString());
        InibinFile parsed = parser.parse(file);
        parsed.extractFile(file.getParent());
    }
}
