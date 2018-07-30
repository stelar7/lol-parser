package types.filetypes;

import no.stelar7.cdragon.util.NaturalOrderComparator;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.types.wad.WADParser;
import no.stelar7.cdragon.types.wad.data.WADFile;
import no.stelar7.cdragon.util.types.*;
import org.junit.Test;

import javax.print.attribute.HashAttributeSet;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

public class TestWAD
{
    WADParser parser = new WADParser();
    
    @Test
    public void testWeb()
    {
        String pluginName  = "rcp-be-lol-game-data";
        Path   extractPath = UtilHandler.DOWNLOADS_FOLDER;
        
        WADFile parsed = parser.parseLatest(pluginName, extractPath, true);
        
        if (parsed != null)
        {
            parsed.extractFiles(pluginName, null, extractPath);
        }
    }
    
    @Test
    public void testLocal()
    {
        WADFile parsed = parser.parse(UtilHandler.DOWNLOADS_FOLDER.resolve("cdragon/Ashe.wad.client"));
        parsed.extractFiles("Champions", "Ashe.wad.client.compressed", UtilHandler.DOWNLOADS_FOLDER.resolve("Ashe"));
    }
    
    @Test
    public void testClientWAD() throws Exception
    {
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
    
    @Test
    public void testPullCDTB()
    {
        String hashA = "https://raw.githubusercontent.com/CommunityDragon/CDTB/wad-client/cdragontoolbox/hashes.game.txt";
        String hashB = "https://raw.githubusercontent.com/CommunityDragon/CDTB/wad-client/cdragontoolbox/hashes.lcu.txt";
        
        Set<String> changedPlugins = new HashSet<>();
        
        Function<Vector2, String> findPlugin = s -> {
            String prePre = (String) s.getSecond();
            if (prePre.startsWith("plugins/"))
            {
                prePre = prePre.substring("plugins/".length());
            }
            String       plugin = prePre.substring(0, prePre.indexOf('/'));
            List<String> ch     = List.of("assets", "content", "data");
            if (ch.contains(plugin))
            {
                plugin = "champions";
            }
            return plugin;
        };
        
        List<String> data = WebHandler.readWeb(hashA);
        data.addAll(WebHandler.readWeb(hashB));
        Map<String, Set<Vector2>> hashes = data.stream()
                                               .map(line -> line.substring(line.indexOf(' ') + 1))
                                               .map(pre -> new Vector2(HashHandler.computeXXHash64(pre), pre))
                                               .collect(Collectors.groupingBy(findPlugin, Collectors.toSet()));
        
        hashes.forEach((plugin, set) -> {
            
            try
            {
                HashHandler.getWadHashes(plugin).forEach((k, v) -> set.add(new Vector2<>(k, v)));
                
                List<Vector2> foundHashes = new ArrayList<>(set);
                
                System.out.println("Sorting hashes");
                foundHashes.sort(Comparator.comparing(Vector2::getSecond, new NaturalOrderComparator()));
                
                System.out.println("Writing hashes");
                JsonWriterWrapper jsonWriter = new JsonWriterWrapper();
                jsonWriter.beginObject();
                for (Vector2<String, String> pair : foundHashes)
                {
                    jsonWriter.name(pair.getFirst()).value(pair.getSecond());
                }
                jsonWriter.endObject();
                
                Files.write(HashHandler.WAD_HASH_STORE.resolve(plugin + ".json"), jsonWriter.toString().getBytes(StandardCharsets.UTF_8));
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        });
    }
}