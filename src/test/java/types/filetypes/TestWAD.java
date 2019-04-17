package types.filetypes;

import no.stelar7.cdragon.types.bin.BINParser;
import no.stelar7.cdragon.types.bin.data.BINFile;
import no.stelar7.cdragon.types.dds.DDSParser;
import no.stelar7.cdragon.types.filemanifest.ManifestContentParser;
import no.stelar7.cdragon.types.packagemanifest.PackagemanifestParser;
import no.stelar7.cdragon.types.packagemanifest.data.*;
import no.stelar7.cdragon.types.wad.data.content.WADContentHeaderV1;
import no.stelar7.cdragon.util.NaturalOrderComparator;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.types.wad.WADParser;
import no.stelar7.cdragon.types.wad.data.WADFile;
import no.stelar7.cdragon.util.hashguessing.HashGuesser;
import no.stelar7.cdragon.util.types.*;
import no.stelar7.cdragon.util.types.math.Vector2;
import no.stelar7.cdragon.util.writers.JsonWriterWrapper;
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
        Path      extractPath = UtilHandler.CDRAGON_FOLDER;
        
        WADFile parsed = parser.parseLatest(pluginName, extractPath, true);
        
        if (parsed != null)
        {
            parsed.extractFiles(extractPath, pluginName);
        }
    }
    
    @Test
    public void testPBE() throws IOException
    {
        final BINParser bp = new BINParser();
        final DDSParser dp = new DDSParser();
        
        Path from = UtilHandler.CDRAGON_FOLDER.resolve("pbe");
        Path to   = from.resolve("extracted");
        
        /*
        
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
        
        */
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
        /*
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
             */
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
        Path gameOutput = UtilHandler.CDRAGON_FOLDER.resolve("pbe").resolve("game").resolve(gversions.get(0));
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
        Path clientOutput = UtilHandler.CDRAGON_FOLDER.resolve("pbe").resolve("client").resolve(cversions.get(0));
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
        Path      path   = UtilHandler.CDRAGON_FOLDER.resolve("extractedFiles2\\DATA\\FINAL\\Champions\\Corki.wad.client");
        WADParser parser = new WADParser();
        WADFile   parsed = parser.parse(path);
        parsed.extractFiles(path.resolveSibling("blitz"), "FiddleSticks");
    }
    
    @Test
    public void testClientWAD() throws Exception
    {
        Path extractPath = UtilHandler.CDRAGON_FOLDER.resolve("temp");
        Path rito        = Paths.get("C:\\Riot Games\\League of Legends");
        
        extractWads(rito, extractPath);
    }
    
    @Test
    public void testCDragonWAD() throws Exception
    {
        Path extractPath = UtilHandler.CDRAGON_FOLDER.resolve("pbe");
        Path rito        = UtilHandler.CDRAGON_FOLDER.resolve("extractedFiles");
        
        generateUnknownFileList(rito);
        extractWads(rito, extractPath);
        transformManifest(extractPath);
        transformBIN(extractPath);
        transformDDS(extractPath);
    }
    
    private void transformManifest(Path extractPath) throws IOException
    {
        System.out.println("Transforming manifests to readable format");
        
        final ManifestContentParser dp = new ManifestContentParser();
        Files.walk(extractPath)
             .parallel()
             .filter(a -> a.getFileName().toString().endsWith(".manifestv1"))
             .forEach(file -> {
                 try
                 {
                     Path               output  = file.resolveSibling(UtilHandler.pathToFilename(file) + ".json");
                     Collection<String> content = dp.parseV1(file).getItems();
                     Files.write(output, UtilHandler.getGson().toJson(content).getBytes(StandardCharsets.UTF_8));
                     //file.toFile().deleteOnExit();
                 } catch (IOException e)
                 {
                     System.out.println(file);
                     e.printStackTrace();
                 }
             });
        
        Files.walk(extractPath)
             .parallel()
             .filter(a -> a.getFileName().toString().endsWith(".manifestv2"))
             .forEach(file -> {
                 try
                 {
                     Path                      output  = file.resolveSibling(UtilHandler.pathToFilename(file) + ".json");
                     Map<String, List<String>> content = dp.parseV2(file).getItems();
                     Files.write(output, UtilHandler.getGson().toJson(content).getBytes(StandardCharsets.UTF_8));
                     //file.toFile().deleteOnExit();
                 } catch (IOException e)
                 {
                     System.out.println(file);
                     e.printStackTrace();
                 }
             });
    }
    
    private void transformDDS(Path extractPath) throws IOException
    {
        System.out.println("Transforming dds files to png");
        
        final DDSParser dp = new DDSParser();
        Files.walk(extractPath)
             .parallel()
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
    
    private void transformBIN(Path extractPath) throws IOException
    {
        System.out.println("Transforming bin files to json");
        
        final BINParser bp = new BINParser();
        Files.walk(extractPath)
             .parallel()
             .filter(a -> a.getFileName().toString().endsWith(".bin"))
             .forEach(file -> {
                 try
                 {
                     BINFile parsed = bp.parse(file);
                     Path    output = file.resolveSibling(UtilHandler.pathToFilename(file) + ".json");
                     Files.write(output, parsed.toJson().getBytes(StandardCharsets.UTF_8));
                     //file.toFile().deleteOnExit();
                 } catch (IOException e)
                 {
                     e.printStackTrace();
                 }
             });
    }
    
    private void extractWads(Path from, Path to) throws IOException
    {
        List<String> ends  = Arrays.asList(".wad", ".wad.client");
        List<String> endsc = Arrays.asList(".wad.compressed", ".wad.client.compressed");
        
        WADParser parser = new WADParser();
        Files.walk(from)
             .parallel()
             // .filter(f -> f.toString().contains("rcp-be-lol-game-data"))
             .forEach(file -> {
                 if (Files.isDirectory(file))
                 {
                     return;
                 }
            
                 String parent   = file.getParent().getFileName().toString();
                 String child    = file.getFileName().toString();
                 String filename = parent + "/" + child;
            
                 if (ends.stream().anyMatch(child::endsWith))
                 {
                     System.out.println("Extracting from " + filename);
                     WADFile parsed = parser.parseReadOnly(file);
                     parsed.extractFiles(to, parent);
                 }
            
                 if (endsc.stream().anyMatch(child::endsWith))
                 {
                     System.out.println("Extracting from " + filename);
                     WADFile parsed = parser.parseCompressed(file);
                     parsed.extractFiles(to, parent);
                 }
            
                 file.toFile().delete();
             });
    }
    
    @Test
    public void generateHashfileList() throws IOException
    {
        System.out.println("Generating hashfile list");
        
        WADParser parser = new WADParser();
        UtilHandler.CDRAGON_FOLDER.resolve("hashes.txt").toFile().delete();
        Files.walkFileTree(UtilHandler.CDRAGON_FOLDER.resolve("extractedFiles"), new SimpleFileVisitor<>()
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
                    try
                    {
                        StandardOpenOption[] flags = {StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND};
                        for (WADContentHeaderV1 header : parsed.getContentHeaders())
                        {
                            Files.write(UtilHandler.CDRAGON_FOLDER.resolve("hashes.txt"), (header.getPathHash() + "\n").getBytes(StandardCharsets.UTF_8), flags);
                        }
                    } catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
                
                if (endscCount != 0)
                {
                    WADFile parsed = parser.parseCompressed(file);
                    try
                    {
                        StandardOpenOption[] flags = {StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND};
                        for (WADContentHeaderV1 header : parsed.getContentHeaders())
                        {
                            Files.write(UtilHandler.CDRAGON_FOLDER.resolve("hashes.txt"), (header.getPathHash() + "\n").getBytes(StandardCharsets.UTF_8), flags);
                        }
                    } catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
                
                return FileVisitResult.CONTINUE;
            }
        });
    }
    
    private void generateUnknownFileList(Path from) throws IOException
    {
        System.out.println("Generating unknown files list");
        
        WADParser parser = new WADParser();
        UtilHandler.CDRAGON_FOLDER.resolve("unknownsSorted.txt").toFile().delete();
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
                    WADFile parsed   = parser.parseReadOnly(file);
                    String  filename = String.format("%-60s", file.getParent().getFileName().toString() + "/" + file.getFileName().toString());
                    parsed.printUnknownFiles(filename);
                    return FileVisitResult.CONTINUE;
                }
                
                if (endscCount != 0)
                {
                    WADFile parsed   = parser.parseCompressed(file);
                    String  filename = String.format("%-60s", file.getParent().getFileName().toString() + "/" + file.getFileName().toString());
                    parsed.printUnknownFiles(filename);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }
    
    private final Function<Vector2, String> findPlugin = s -> {
        String prePre = (String) s.getSecond();
        if (prePre.startsWith("plugins/"))
        {
            return "lcu";
        }
        
        return "game";
    };
    
    
    @Test
    public void testPullCDTB()
    {
        System.out.println("Feching hashlists from CDTB");
        String hashA = "https://github.com/CommunityDragon/CDTB/raw/master/cdragontoolbox/hashes.game.txt";
        String hashB = "https://github.com/CommunityDragon/CDTB/raw/master/cdragontoolbox/hashes.lcu.txt";
        String hashC = "https://github.com/Morilli/CDTB/raw/new-hashes/cdragontoolbox/hashes.game.txt";
        String hashD = "https://github.com/Morilli/CDTB/raw/new-hashes/cdragontoolbox/hashes.lcu.txt";
        
        Set<String>  changedPlugins = new HashSet<>();
        List<String> data           = WebHandler.readWeb(hashA);
        data.addAll(WebHandler.readWeb(hashB));
        data.addAll(WebHandler.readWeb(hashC));
        data.addAll(WebHandler.readWeb(hashD));
        Map<String, Set<Vector2>> hashes = data.stream()
                                               .map(line -> line.substring(line.indexOf(' ') + 1))
                                               .map(pre -> new Vector2(HashHandler.computeXXHash64(pre), pre))
                                               .collect(Collectors.groupingBy(findPlugin, Collectors.toSet()));
        
        updateLocalHashList(hashes);
        System.out.println("Hash loading finished");
    }
    
    @Test
    public void mergeHashHandlerFiles()
    {
        Map<String, Set<Vector2>> lcuHash = HashGuesser.hashFileLCU.load(true)
                                                                   .values()
                                                                   .stream()
                                                                   .map(pre -> new Vector2(HashHandler.computeXXHash64(pre), pre))
                                                                   .collect(Collectors.groupingBy(findPlugin, Collectors.toSet()));
        
        Map<String, Set<Vector2>> gameHash = HashGuesser.hashFileGAME.load(true)
                                                                     .values()
                                                                     .stream()
                                                                     .map(pre -> new Vector2(HashHandler.computeXXHash64(pre), pre))
                                                                     .collect(Collectors.groupingBy(findPlugin, Collectors.toSet()));
        
        updateLocalHashList(lcuHash);
        updateLocalHashList(gameHash);
    }
    
    public void updateLocalHashList(Map<String, Set<Vector2>> hashes)
    {
        Map<String, Set<Vector2>> selfHashes = HashHandler.getWADHashes()
                                                          .values()
                                                          .stream()
                                                          .map(pre -> new Vector2(HashHandler.computeXXHash64(pre), pre))
                                                          .collect(Collectors.groupingBy(findPlugin, Collectors.toSet()));
        
        System.out.println("Updating local hashlists");
        hashes.forEach((k, v) -> {
            try
            {
                
                Set<Vector2> allHashes = new HashSet<>(v);
                allHashes.addAll(selfHashes.get(k));
                
                List<Vector2> foundHashes = new ArrayList<>(allHashes);
                foundHashes.sort(Comparator.comparing(Vector2::getSecond, new NaturalOrderComparator()));
                
                JsonWriterWrapper jsonWriter = new JsonWriterWrapper();
                jsonWriter.beginObject();
                for (Vector2<String, String> pair : foundHashes)
                {
                    jsonWriter.name(pair.getFirst()).value(pair.getSecond());
                }
                jsonWriter.endObject();
                
                Files.write(HashHandler.WAD_HASH_STORE.resolve(k + ".json"), jsonWriter.toString().getBytes(StandardCharsets.UTF_8));
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        });
        
        HashHandler.reloadWadHashes();
    }
}