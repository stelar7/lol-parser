package types.filetypes;

import com.google.gson.reflect.TypeToken;
import no.stelar7.cdragon.types.bin.BINParser;
import no.stelar7.cdragon.types.bin.data.BINFile;
import no.stelar7.cdragon.types.dds.DDSParser;
import no.stelar7.cdragon.types.packagemanifest.PackagemanifestParser;
import no.stelar7.cdragon.types.packagemanifest.data.*;
import no.stelar7.cdragon.util.NaturalOrderComparator;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.types.wad.WADParser;
import no.stelar7.cdragon.types.wad.data.WADFile;
import no.stelar7.cdragon.util.types.*;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

public class TestWAD
{
    @Test
    public void testWeb()
    {
        WADParser parser      = new WADParser();
        String    pluginName  = "rcp-be-lol-game-data";
        Path      extractPath = UtilHandler.DOWNLOADS_FOLDER;
        
        WADFile parsed = parser.parseLatest(pluginName, extractPath, true);
        
        if (parsed != null)
        {
            parsed.extractFiles(pluginName, null, extractPath);
        }
    }
    
    @Test
    public void testPBE() throws IOException
    {
        final BINParser bp = new BINParser();
        final DDSParser dp = new DDSParser();
        
        Path from = UtilHandler.DOWNLOADS_FOLDER.resolve("pbe");
        Path to   = from.resolve("extracted");
        
        downloadPBEAssets("");
        String       jsn  = String.join("\n", WebHandler.readWeb("http://ddragon.leagueoflegends.com/cdn/languages.json"));
        List<String> data = UtilHandler.getGson().fromJson(jsn, new TypeToken<List<String>>() {}.getType());
        data.forEach(k -> {
            try
            {
                downloadPBEAssets("_" + k.toLowerCase());
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        });
        
        extractWads(from, to);
        System.out.println("Extraction finished!");
        
        /*
        if (Files.exists(from.resolve("client")))
        {
            Files.walk(from.resolve("client"))
                 .sorted(Comparator.reverseOrder())
                 .map(Path::toFile)
                 .forEach(File::delete);
        }
        
        if (Files.exists(from.resolve("game")))
        {
            Files.walk(from.resolve("game"))
                 .sorted(Comparator.reverseOrder())
                 .map(Path::toFile)
                 .forEach(File::delete);
        }
        */
        
        System.out.println("Transforming bin files to json");
        Files.walk(from)
             .filter(a -> a.getFileName().toString().endsWith(".bin"))
             .forEach(file -> {
                 try
                 {
                     BINFile parsed = bp.parse(file);
                     Path    output = file.resolveSibling(UtilHandler.pathToFilename(file) + ".json");
                     Files.write(output, parsed.toJson().getBytes(StandardCharsets.UTF_8));
                     file.toFile().deleteOnExit();
                 } catch (IOException e)
                 {
                     e.printStackTrace();
                 }
             });
        
        System.out.println("Transforming dds files to png");
        Files.walk(from)
             .filter(a -> a.getFileName().toString().endsWith(".dds"))
             .forEach(file -> {
                 try
                 {
                     BufferedImage img    = dp.parse(file);
                     Path          output = file.resolveSibling(UtilHandler.pathToFilename(file) + ".png");
                     ImageIO.write(img, "png", output.toFile());
                     file.toFile().deleteOnExit();
                 } catch (IOException e)
                 {
                     e.printStackTrace();
                 }
             });
    }
    
    private void extractDDS(Path from) throws IOException
    {
        DDSParser parser = new DDSParser();
        Files.walkFileTree(from, new SimpleFileVisitor<>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
            {
                if (file.getFileName().toString().endsWith(".dds"))
                {
                    BufferedImage img    = parser.parse(file);
                    Path          output = file.resolveSibling(UtilHandler.pathToFilename(file) + ".png");
                    ImageIO.write(img, "png", output.toFile());
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }
    
    private void extractBins(Path from) throws IOException
    {
        BINParser parser = new BINParser();
        Files.walkFileTree(from, new SimpleFileVisitor<>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
            {
                if (file.getFileName().toString().endsWith(".bin"))
                {
                    BINFile parsed = parser.parse(file);
                    Path    output = file.resolveSibling(UtilHandler.pathToFilename(file) + ".json");
                    Files.write(output, parsed.toJson().getBytes(StandardCharsets.UTF_8));
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }
    
    public void downloadPBEAssets(String lang)
    {
        PackagemanifestParser pparser = new PackagemanifestParser();
        String                prefix  = "http://l3cdn.riotgames.com/releases/pbe";
        
        
        List<String>        gversions = WebHandler.readWeb("http://l3cdn.riotgames.com/releases/pbe/projects/lol_game_client" + lang + "/releases/releaselisting_PBE");
        String              gversion  = gversions.get(1);
        ByteArray           gdata     = WebHandler.readBytes(String.format("http://l3cdn.riotgames.com/releases/pbe/projects/lol_game_client" + lang + "/releases/%s/packages/files/packagemanifest", gversion));
        PackagemanifestFile gfile     = pparser.parse(gdata);
        
        System.out.println("Downloading game files");
        Path gameOutput = UtilHandler.DOWNLOADS_FOLDER.resolve("pbe").resolve("game").resolve(gversions.get(0));
        gfile.getFiles().parallelStream().forEach(s -> {
            String output   = s.getFilePath().substring(s.getFilePath().indexOf("files") + "files".length() + 1);
            String filename = UtilHandler.getFilename(s.getFilePath());
            System.out.println(filename);
            WebHandler.downloadFile(gameOutput.resolve(output), prefix + s.getFilePath());
        });
        
        List<String>        cversions = WebHandler.readWeb("http://l3cdn.riotgames.com/releases/pbe/projects/league_client" + lang + "/releases/releaselisting_PBE");
        String              cversion  = cversions.get(1);
        ByteArray           cdata     = WebHandler.readBytes(String.format("http://l3cdn.riotgames.com/releases/pbe/projects/league_client" + lang + "/releases/%s/packages/files/packagemanifest", cversion));
        PackagemanifestFile cfile     = pparser.parse(cdata);
        
        System.out.println("Downloading client files");
        Path clientOutput = UtilHandler.DOWNLOADS_FOLDER.resolve("pbe").resolve("client").resolve(cversions.get(0));
        cfile.getFiles().parallelStream().forEach(s -> {
            String output   = s.getFilePath().substring(s.getFilePath().indexOf("files") + "files".length() + 1);
            String filename = UtilHandler.getFilename(s.getFilePath());
            System.out.println(filename);
            WebHandler.downloadFile(clientOutput.resolve(output), prefix + s.getFilePath());
        });
    }
    
    @Test
    public void testLocal()
    {
        WADParser parser = new WADParser();
        WADFile   parsed = parser.parse(UtilHandler.DOWNLOADS_FOLDER.resolve("cdragon/Map11.de_DE.wad.client"));
        parsed.extractFiles("levels", "Map11.de_DE.wad.client", UtilHandler.DOWNLOADS_FOLDER.resolve("Ashe"));
    }
    
    @Test
    public void testClientWAD() throws Exception
    {
        Path extractPath = UtilHandler.DOWNLOADS_FOLDER.resolve("temp");
        Path rito        = Paths.get("C:\\Riot Games\\League of Legends");
        
        extractWads(rito, extractPath);
    }
    
    private void extractWads(Path from, Path to) throws IOException
    {
        WADParser parser = new WADParser();
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
        String hashC = "https://github.com/Morilli/CDTB/raw/new-hashes/cdragontoolbox/hashes.game.txt";
        String hashD = "https://github.com/Morilli/CDTB/raw/new-hashes/cdragontoolbox/hashes.lcu.txt";
        
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
        data.addAll(WebHandler.readWeb(hashC));
        data.addAll(WebHandler.readWeb(hashD));
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