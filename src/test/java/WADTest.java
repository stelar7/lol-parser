import no.stelar7.cdragon.util.UtilHandler;
import no.stelar7.cdragon.wad.WADParser;
import no.stelar7.cdragon.wad.data.WADFile;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.*;

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
    public void testWADAll()
    {
        WADParser parser = new WADParser();
        
        String pluginName  = "rcp-be-lol-game-data";
        Path   extractPath = Paths.get(System.getProperty("user.home"), "Downloads");
        
        ExecutorService executor = Executors.newFixedThreadPool(1);//Runtime.getRuntime().availableProcessors() / 2);
        for (int i = (int) UtilHandler.getLongFromIP("0.0.1.38"); i > 0; i--)
        {
            int ver = i;
            executor.submit(() -> {
                try
                {
                    WADFile parsed   = parser.parseVersion(pluginName, ver, extractPath);
                    String  realName = pluginName + "_" + UtilHandler.getIPFromLong(ver);
                    
                    if (parsed != null)
                    {
                        parsed.extractFiles(realName, null, extractPath);
                    } else
                    {
                        System.out.println("File not found; " + realName);
                    }
                } catch (Exception e)
                {
                    e.printStackTrace();
                }
            });
        }
        
        try
        {
            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }
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