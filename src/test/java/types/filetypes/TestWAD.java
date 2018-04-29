package types.filetypes;

import no.stelar7.cdragon.util.handlers.UtilHandler;
import no.stelar7.cdragon.types.wad.WADParser;
import no.stelar7.cdragon.types.wad.data.WADFile;
import org.junit.Test;

import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class TestWAD
{
    @Test
    public void testWAD()
    {
        WADParser parser = new WADParser();
        
        String pluginName  = "rcp-be-lol-game-data";
        Path   extractPath = UtilHandler.DOWNLOADS_FOLDER;
        
        WADFile parsed = parser.parseLatest(pluginName, extractPath, true);
        
        if (parsed != null)
        {
            parsed.extractFiles(pluginName, null, extractPath);
        }
    }
    
    @Test
    public void testWEB()
    {
        WADParser parser = new WADParser();
        
        WADFile parsed = parser.parse(UtilHandler.DOWNLOADS_FOLDER.resolve("decompressed\\Zoe.wad.client"));
        parsed.extractFiles("Champions", "Zoe.client", UtilHandler.DOWNLOADS_FOLDER.resolve("decompressed\\Zoe"));
    }
    
    @Test
    public void testClientWAD() throws Exception
    {
        WADParser parser = new WADParser();
        
        Path extractPath = UtilHandler.DOWNLOADS_FOLDER.resolve("temp");
        Path rito        = Paths.get("C:\\Riot Games\\League of Legends");
        
        Files.walkFileTree(rito, new SimpleFileVisitor<>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
            {
                if (file.getFileName().toString().endsWith(".wad"))
                {
                    WADFile parsed = parser.parse(file);
                    parsed.extractFiles(file.getParent().getFileName().toString(), file.getFileName().toString(), extractPath);
                }
                
                if (file.getFileName().toString().endsWith(".wad.client"))
                {
                    WADFile parsed = parser.parse(file);
                    parsed.extractFiles(file.getParent().getFileName().toString(), file.getFileName().toString(), extractPath);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }
}