package types.filetypes;

import no.stelar7.cdragon.types.packagemanifest.PackagemanifestParser;
import no.stelar7.cdragon.types.packagemanifest.data.*;
import no.stelar7.cdragon.types.releasemanifest.ReleasemanifestParser;
import no.stelar7.cdragon.types.releasemanifest.data.ReleasemanifestDirectory;
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
    public void testPBE() throws IOException
    {
        PackagemanifestParser pparser = new PackagemanifestParser();
        String                prefix  = "http://l3cdn.riotgames.com/releases/pbe";
        
        
        List<String>        gversions = WebHandler.readWeb("http://l3cdn.riotgames.com/releases/pbe/projects/lol_game_client/releases/releaselisting_PBE");
        ByteArray           gdata     = WebHandler.readBytes(String.format("http://l3cdn.riotgames.com/releases/pbe/projects/lol_game_client/releases/%s/packages/files/packagemanifest", gversions.get(0)));
        PackagemanifestFile gfile     = pparser.parse(gdata);
        
        System.out.println("Downloading game files");
        Path gameOutput = UtilHandler.DOWNLOADS_FOLDER.resolve("pbe").resolve("game").resolve(gversions.get(0));
        gfile.getFiles().parallelStream().forEach(s -> {
            String output   = s.getFilePath().substring(s.getFilePath().indexOf("files") + "files".length() + 1);
            String filename = UtilHandler.getFilename(s.getFilePath());
            System.out.println(filename);
            WebHandler.downloadFile(gameOutput.resolve(output), prefix + s.getFilePath());
        });
        
        List<String>        cversions = WebHandler.readWeb("http://l3cdn.riotgames.com/releases/pbe/projects/league_client/releases/releaselisting_PBE");
        ByteArray           cdata     = WebHandler.readBytes(String.format("http://l3cdn.riotgames.com/releases/pbe/projects/league_client/releases/%s/packages/files/packagemanifest", cversions.get(0)));
        PackagemanifestFile cfile     = pparser.parse(cdata);
        
        System.out.println("Downloading client files");
        Path clientOutput = UtilHandler.DOWNLOADS_FOLDER.resolve("pbe").resolve("client").resolve(cversions.get(0));
        cfile.getFiles().parallelStream().forEach(s -> {
            String output   = s.getFilePath().substring(s.getFilePath().indexOf("files") + "files".length() + 1);
            String filename = UtilHandler.getFilename(s.getFilePath());
            System.out.println(filename);
            WebHandler.downloadFile(clientOutput.resolve(output), prefix + s.getFilePath());
        });
        
        
        extractAllWads(UtilHandler.DOWNLOADS_FOLDER.resolve("pbe"), UtilHandler.DOWNLOADS_FOLDER.resolve("pbe").resolve("extracted"));
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
        
        extractAllWads(rito, extractPath);
    }
    
    private void extractAllWads(Path from, Path to) throws IOException
    {
        Files.walkFileTree(from, new SimpleFileVisitor<>()
        {
            List<String> ends = Arrays.asList(".wad", ".wad.client");
            List<String> endsc = Arrays.asList(".wad.compressed", ".wad.client.compressed");
            
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
            {
                long endsCount  = ends.stream().filter(a -> file.getFileName().toString().endsWith(a)).count();
                long endscCount = endsc.stream().filter(a -> file.getFileName().toString().endsWith(a)).count();
                if (endsCount != 0)
                {
                    WADFile parsed = parser.parse(file);
                    parsed.extractFiles(file.getParent().getFileName().toString(), file.getFileName().toString(), to);
                    return FileVisitResult.CONTINUE;
                }
                
                if (endscCount != 0)
                {
                    WADFile parsed = parser.parseCompressed(file);
                    parsed.extractFiles(file.getParent().getFileName().toString(), file.getFileName().toString(), to);
                }
                return FileVisitResult.CONTINUE;
            }
            
        });
    }
    
    @Test
    public void testPullCDTB()
    {
        String hashA = "https://github.com/CommunityDragon/CDTB/raw/master/cdragontoolbox/hashes.game.txt";
        String hashB = "https://github.com/CommunityDragon/CDTB/raw/master/cdragontoolbox/hashes.lcu.txt";
        
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
                foundHashes.sort(Comparator.comparing(Vector2::getSecond, new NaturalOrderComparator()));
                
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