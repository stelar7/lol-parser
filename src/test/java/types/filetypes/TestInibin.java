package types.filetypes;

import no.stelar7.cdragon.types.inibin.InibinParser;
import no.stelar7.cdragon.types.inibin.data.InibinFile;
import no.stelar7.cdragon.util.NaturalOrderComparator;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import org.junit.jupiter.api.Test;

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
        
        Path extractPath = UtilHandler.CDRAGON_FOLDER.resolve("inibin");
        Path rito        = UtilHandler.CDRAGON_FOLDER.resolve("raf");
        
        List<Path> paths = new ArrayList<>();
        
        Files.walkFileTree(rito, new SimpleFileVisitor<>()
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
            parsed.extract(extractPath);
        }
    }
    
    @Test
    public void testDownloadedInibin() throws IOException
    {
        InibinParser parser = new InibinParser();
        Path         file   = UtilHandler.CDRAGON_FOLDER.resolve("pman_inibin");
        Files.walkFileTree(file, new SimpleFileVisitor<>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
            {
                if (Files.isDirectory(file))
                {
                    return FileVisitResult.CONTINUE;
                }
                
                InibinFile parsed = parser.parseCompressed(file);
                parsed.extract(file.getParent());
                return FileVisitResult.CONTINUE;
            }
        });
    }
    
    @Test
    public void testKindredInbin()
    {
        InibinParser parser = new InibinParser();
        
        Path file = UtilHandler.CDRAGON_FOLDER.resolve("JannaUpgradeActive.inibin");
        
        System.out.println("Parsing: " + file.toString());
        InibinFile parsed = parser.parse(file);
        parsed.extract(file.getParent());
    }
}
