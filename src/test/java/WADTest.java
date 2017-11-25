import no.stelar7.cdragon.wad.WADParser;
import no.stelar7.cdragon.wad.data.WADFile;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class WADTest
{
    @Test
    public void testWAD() throws Exception
    {
        WADParser parser = new WADParser();
        
        String pluginName  = "rcp-be-lol-game-data";
        Path   extractPath = Paths.get(System.getProperty("user.home"), "Downloads");
        
        WADFile parsed = parser.parseLatest(pluginName, extractPath);
        parsed.extractFiles(pluginName, null, extractPath);
    }
    
    @Test
    public void testClientWAD() throws Exception
    {
        WADParser parser = new WADParser();
        
        Path extractPath = Paths.get(System.getProperty("user.home"), "Downloads", "temp");
        Path rito        = Paths.get("C:\\Riot Games");
        
        Files.walkFileTree(rito, new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
            {
                if (file.getFileName().toString().contains(".wad"))
                {
                    WADFile parsed = parser.parse(file);
                    parsed.extractFiles(file.getParent().getFileName().toString(), file.getFileName().toString(), extractPath.resolve(file.getParent().getFileName()));
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }
}