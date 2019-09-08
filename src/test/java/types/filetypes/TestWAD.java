package types.filetypes;

import no.stelar7.cdragon.types.bin.BINParser;
import no.stelar7.cdragon.types.bin.data.BINFile;
import no.stelar7.cdragon.types.dds.DDSParser;
import no.stelar7.cdragon.types.filemanifest.ManifestContentParser;
import no.stelar7.cdragon.types.packagemanifest.PackagemanifestParser;
import no.stelar7.cdragon.types.packagemanifest.data.PackagemanifestFile;
import no.stelar7.cdragon.types.skn.SKNParser;
import no.stelar7.cdragon.types.skn.data.SKNFile;
import no.stelar7.cdragon.types.wad.WADParser;
import no.stelar7.cdragon.types.wad.data.WADFile;
import no.stelar7.cdragon.types.wad.data.content.WADContentHeaderV1;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.types.*;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.*;
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
        Path      path   = UtilHandler.CDRAGON_FOLDER.resolve("extractedFiles\\DATA\\FINAL\\Champions\\Kaisa.cs_CZ.wad.client");
        WADParser parser = new WADParser();
        WADFile   parsed = parser.parse(path);
        parsed.extractFiles(path.resolveSibling("Kaisa"), "Kaisa");
    }
    
    @Test
    public void listAllFilesInAllWads() throws IOException
    {
        List<String> ends  = Arrays.asList(".wad", ".wad.client");
        List<String> endsc = Arrays.asList(".wad.compressed", ".wad.client.compressed");
        Path         from  = UtilHandler.CDRAGON_FOLDER.resolve("extractedFiles");
        
        String search = "fontconfig";
        
        WADParser parser = new WADParser();
        Files.walk(from)
             .parallel()
             .forEach(file -> {
                 if (Files.isDirectory(file))
                 {
                     return;
                 }
            
                 String parent   = file.getParent().getFileName().toString();
                 String child    = file.getFileName().toString();
                 String filename = parent + "/" + child;
            
                 WADFile parsed = null;
                 if (ends.stream().anyMatch(child::endsWith))
                 {
                     parsed = parser.parseReadOnly(file);
                 }
            
                 if (endsc.stream().anyMatch(child::endsWith))
                 {
                     parsed = parser.parseCompressed(file);
                 }
            
                 if (parsed == null)
                 {
                     return;
                 }
            
                 boolean containsSearch = parsed.getContentHeaders()
                                                .stream()
                                                .map(WADContentHeaderV1::getPathHash)
                                                .map(HashHandler::getWadHash)
                                                .anyMatch(s -> s.contains(search));
            
                 if (containsSearch)
                 {
                     System.out.println(filename + " contains a filename containing " + search);
                 }
             });
    }
    
    @Test
    public void testClientWAD() throws Exception
    {
        Path extractPath = UtilHandler.CDRAGON_FOLDER.resolve("temp");
        Path rito        = Paths.get("C:\\Riot Games\\League of Legends");
        
        Path single = Paths.get("D:\\extractedFiles\\DATA\\FINAL\\Champions\\Zoe.wad.client");
        extractWad(single, extractPath);
        Files.walk(extractPath).filter(a -> a.getFileName().toString().endsWith(".skn")).forEach(f -> {
            System.out.println(f);
            SKNParser p   = new SKNParser();
            SKNFile   skn = p.parse(f);
            System.out.println(skn.toOBJ(skn.getMaterials().get(0)));
        });
        
        //extractWads(rito, extractPath);
    }
    
    @Test
    public void testCDragonWAD() throws Exception
    {
        Path extractPath = UtilHandler.CDRAGON_FOLDER.resolve("pbe");
        Path rito        = UtilHandler.CDRAGON_FOLDER.resolve("extractedFiles");
        
        generateUnknownFileList(rito);
        extractWads(rito, extractPath);
        
        transformBIN(extractPath);
        generateUnknownBinHashList();
        
        transformManifest(extractPath);
        
        // windows10 handles dds files just fine
        //transformDDS(extractPath);
        
        // this isnt needed, as i dont mess with models, and when i do, i use SKN files directly
        //transformSKN(extractPath);
    }
    
    private void generateUnknownBinHashList() throws IOException
    {
        System.out.println("Generating list of unknown bin hashes");
        Path binhash = UtilHandler.CDRAGON_FOLDER.resolve("binHashUnknown.txt");
        Files.write(binhash, "".getBytes(StandardCharsets.UTF_8));
        List<String> sortedHashes = new ArrayList<>(BINParser.hashes).stream()
                                                                     .filter(h -> {
                                                                         try
                                                                         {
                                                                             Long.decode("0x" + h);
                                                                             return true;
                                                                         } catch (Exception e)
                                                                         {
                                                                             return false;
                                                                         }
                                                                     })
                                                                     .sorted()
                                                                     .filter(h -> HashHandler.getBinHashes().get(h) == null)
                                                                     .collect(Collectors.toList());
        
        Files.write(binhash, sortedHashes, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
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
    
    private void transformSKN(Path extractPath) throws IOException
    {
        System.out.println("Transforming skl files to obj");
        
        final SKNParser dp = new SKNParser();
        Files.walk(extractPath)
             .parallel()
             .filter(a -> a.getFileName().toString().endsWith(".skn"))
             .forEach(file -> {
                 try
                 {
                     SKNFile skn          = dp.parse(file);
                     Path    outputFolder = file.resolveSibling("models");
                     Files.createDirectories(outputFolder);
                     skn.getMaterials().forEach(m -> {
                         try
                         {
                             Path   output = outputFolder.resolve(m.getName().replace(":", "-") + ".obj");
                             String obj    = skn.toOBJ(m);
                             Files.write(output, obj.getBytes(StandardCharsets.UTF_8));
                         } catch (IOException e)
                         {
                             System.out.println(file);
                             e.printStackTrace();
                         }
                     });
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
             .filter(a -> a.getFileName().toString().endsWith(".dds"))
             .forEach(file -> {
                 try
                 {
                     BufferedImage img    = dp.parse(file);
                     Path          output = file.resolveSibling(UtilHandler.pathToFilename(file) + ".png");
                     ImageIO.write(img, "png", output.toFile());
                     //file.toFile().deleteOnExit();
                 } catch (IOException e)
                 {
                     System.out.println(file);
                     e.printStackTrace();
                 }
             });
    }
    
    private void transformBIN(Path extractPath) throws IOException
    {
        System.out.println("Transforming bin files to json");
        
        final BINParser bp = new BINParser();
        Files.walk(extractPath)
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
                     System.out.println(file);
                     e.printStackTrace();
                 }
             });
    }
    
    private void extractWad(Path file, Path to)
    {
        if (Files.isDirectory(file))
        {
            return;
        }
        
        String parent   = file.getParent().getFileName().toString();
        String child    = file.getFileName().toString();
        String filename = parent + "/" + child;
        
        System.out.println("Extracting from " + filename);
        WADFile parsed = new WADParser().parse(file);
        parsed.extractFiles(to, parent);
    }
    
    
    private void extractWads(Path from, Path to)
    {
        try
        {
            List<String> ends  = Arrays.asList(".wad", ".wad.client");
            List<String> endsc = Arrays.asList(".wad.compressed", ".wad.client.compressed");
            List<String> endsm = Collections.singletonList(".wad.mobile");
            
            ExecutorService                       service  = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1);
            Set<Pair<Long, Function<Void, Void>>> extracts = new TreeSet<>(Comparator.comparingLong((ToLongFunction<Pair<Long, Function<Void, Void>>>) Pair::getA).reversed());
            
            WADParser parser = new WADParser();
            Files.walk(from)
                 .filter(f -> !Files.isDirectory(f))
                 // .filter(f -> f.toString().contains("rcp-be-lol-game-data"))
                 .forEach(file -> {
                     try
                     {
                         String parent   = file.getParent().getFileName().toString();
                         String child    = file.getFileName().toString();
                         String filename = parent + "/" + child;
                         long   size     = Files.size(file);
                    
                         if (ends.stream().anyMatch(child::endsWith))
                         {
                             Function<Void, Void> export = a -> {
                                 System.out.println("Extracting from " + filename);
                                 WADFile parsed = parser.parseReadOnly(file);
                                 parsed.extractFiles(to, parent);
                                 return null;
                             };
                        
                             extracts.add(new Pair(size, export));
                         }
                    
                         if (endsc.stream().anyMatch(child::endsWith))
                         {
                             Function<Void, Void> export = a -> {
                                 System.out.println("Extracting from " + filename);
                                 WADFile parsed = parser.parseCompressed(file);
                                 parsed.extractFiles(to, parent);
                                 return null;
                             };
                        
                             extracts.add(new Pair(size, export));
                         }
                    
                         if (endsm.stream().anyMatch(child::endsWith))
                         {
                             Function<Void, Void> export = a -> {
                                 System.out.println("Extracting from " + filename);
                                 WADFile parsed = parser.parseReadOnly(file);
                                 parsed.extractFiles(to.resolveSibling("mobile"), parent);
                                 return null;
                             };
                        
                             extracts.add(new Pair(size, export));
                         }
                    
                     } catch (IOException e)
                     {
                         e.printStackTrace();
                     }
                 });
            
            
            extracts.forEach(e -> service.submit(() -> e.getB().apply(null)));
            service.shutdown();
            service.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
            
        } catch (IOException | InterruptedException e)
        {
            e.printStackTrace();
        }
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
                try
                {
                    long endsCount  = ends.stream().filter(a -> file.getFileName().toString().endsWith(a)).count();
                    long endscCount = endsc.stream().filter(a -> file.getFileName().toString().endsWith(a)).count();
                    if (endsCount != 0)
                    {
                        WADFile              parsed = parser.parse(file);
                        StandardOpenOption[] flags  = {StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND};
                        List<String>         hashes = parsed.getContentHeaders().stream().map(WADContentHeaderV1::getPathHash).collect(Collectors.toList());
                        Files.write(UtilHandler.CDRAGON_FOLDER.resolve("hashes.txt"), hashes, StandardCharsets.UTF_8, flags);
                    }
                    
                    if (endscCount != 0)
                    {
                        WADFile              parsed = parser.parseCompressed(file);
                        StandardOpenOption[] flags  = {StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND};
                        List<String>         hashes = parsed.getContentHeaders().stream().map(WADContentHeaderV1::getPathHash).collect(Collectors.toList());
                        Files.write(UtilHandler.CDRAGON_FOLDER.resolve("hashes.txt"), hashes, StandardCharsets.UTF_8, flags);
                    }
                    
                } catch (IOException e)
                {
                    e.printStackTrace();
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
}