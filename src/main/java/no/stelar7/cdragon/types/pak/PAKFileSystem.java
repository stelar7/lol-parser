package no.stelar7.cdragon.types.pak;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class PAKFileSystem
{
    Path                 root;
    Map<String, PAKFile> files = new HashMap<>();
    
    public void openDirectory(Path path)
    {
        try
        {
            Files.walkFileTree(path, new SimpleFileVisitor<>()
            {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                {
                    if (file.toString().endsWith(".pak"))
                    {
                        addFile(file);
                    }
                    
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    private void addFile(Path path)
    {
        PAKFile file = new PAKFile();
        
    }
}
