package no.stelar7.cdragon.viewer;

import com.google.gson.JsonObject;
import no.stelar7.cdragon.types.bin.BINParser;
import no.stelar7.cdragon.types.bin.data.BINFile;
import no.stelar7.cdragon.types.rman.RMANParser;
import no.stelar7.cdragon.types.rman.RMANParser.RMANFileType;
import no.stelar7.cdragon.types.rman.data.*;
import no.stelar7.cdragon.types.wad.WADParser;
import no.stelar7.cdragon.types.wad.data.WADFile;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.hashguessing.*;
import no.stelar7.cdragon.util.types.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.Collectors;

public class RMANTool
{
    public static void main(String[] args) throws IOException, InterruptedException
    {
        if (!(args.length == 3))
        {
            System.out.println("tool {cache} {intermediary} {final}");
            return;
        }
        
        Path cachePath   = Paths.get(args[0]);
        Path extractPath = Paths.get(args[1]);
        Path outputPath  = Paths.get(args[2]);
        
        new RMANTool(cachePath, extractPath, outputPath);
    }
    
    public RMANTool(Path cachePath, Path fileFolder, Path extractPath) throws IOException, InterruptedException
    {
        HashHandler.getBinHashes();
        HashHandler.getWADHashes();
        
        List<RMANFile> files = new ArrayList<>();
        JsonObject     obj   = RMANParser.getPBEManifest();
        files.add(RMANParser.loadFromPBE(obj, RMANFileType.GAME));
        files.add(RMANParser.loadFromPBE(obj, RMANFileType.LCU));
        
        System.out.println("Writing manifest content to file");
        files.forEach(RMANFile::printFileList);
        
        Path bundleFolder = cachePath.resolve("bundles");
        Files.createDirectories(bundleFolder);
        
        int     currentVersion = obj.get("version").getAsInt();
        Path    lastVersion    = cachePath.resolve("version");
        boolean shouldDownload = !Files.exists(lastVersion) || Integer.parseInt(Files.readAllLines(lastVersion).get(0)) < currentVersion;
        if (shouldDownload)
        {
            List<String> removedBundles = getRemovedBundleIds(files, bundleFolder);
            removeOldBundles(removedBundles, bundleFolder);
            downloadAllBundles(files, bundleFolder);
            Files.write(lastVersion, String.valueOf(currentVersion).getBytes(StandardCharsets.UTF_8));
        } else
        {
            System.out.println("This version is already downloaded, skipping download step");
        }
        
        boolean shouldExport = !Files.exists(fileFolder);
        if (shouldDownload || shouldExport)
        {
            System.out.println("Deleting old extract files");
            Runtime.getRuntime().exec(new String[]{"rm", "-rf", fileFolder.toFile().toString()}).waitFor();
            Files.createDirectories(fileFolder);
            
            // use one thread per core, and leave one free for the OS
            ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1);
            
            // sort content based on filesize
            Set<Pair<Integer, Function<Void, Void>>> extracts = new TreeSet<>(Comparator.comparingInt((ToIntFunction<Pair<Integer, Function<Void, Void>>>) Pair::getA).reversed());
            files.forEach(manifest -> manifest.getBody()
                                              .getFiles()
                                              .forEach(f -> {
                                                  extracts.add(new Pair<>(f.getFileSize(), (v) -> {
                                                      manifest.extractFile(f, bundleFolder, fileFolder);
                                                      return null;
                                                  }));
                                              }));
            
            extracts.forEach(e -> service.submit(() -> e.getB().apply(null)));
            service.shutdown();
            service.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } else
        {
            System.out.println("This version is already extracted, skipping merging step");
        }
        
        boolean shouldUnpack = !Files.exists(extractPath) || shouldDownload || shouldExport;
        if (shouldUnpack)
        {
            testCDragonWAD(fileFolder, extractPath, cachePath);
        } else
        {
            System.out.println("This version is already unpacked, skipping unpack step");
        }
        
        runHashChecks(cachePath, extractPath);
    }
    
    public void runHashChecks(Path cachePath, Path outputPath)
    {
        boolean found = false;
        found = found | checkBinsHashes(cachePath, outputPath);
        found = found | checkGameHashes(cachePath, outputPath);
        found = found | checkLCUHashes(cachePath, outputPath);
        
        if (found)
        {
            System.out.println("Found new hashes!");
            try
            {
                Runtime.getRuntime().exec(new String[]{});
                //Runtime.getRuntime().exec(new String[]{"git", "add",}).waitFor();
                // Runtime.getRuntime().exec(new String[]{"git", "commit", "-m", "update hashes"}).waitFor();
                //Runtime.getRuntime().exec(new String[]{"git", "push"}).waitFor();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
    
    public boolean checkBinsHashes(Path cachePath, Path outputPath)
    {
        BINHashGuesser guesser = new BINHashGuesser(HashGuesser.unknownFromExportBIN(cachePath.resolve("unknownBins.txt")), outputPath);
        //guesser.guessFromFile(UtilHandler.CDRAGON_FOLDER.resolve("bins.dump"), "#\\[(.+?)]");
        guesser.pullCDTB();
        guesser.guessNewCharacters();
        guesser.guessNewAnimations();
        guesser.guessFromFontFiles();
        guesser.saveAsJson(cachePath.resolve("bins.json"));
        return guesser.didFindNewHash();
    }
    
    public boolean checkGameHashes(Path cachePath, Path outputPath)
    {
        GameHashGuesser guesser = new GameHashGuesser(HashGuesser.unknownFromExportWAD(cachePath.resolve("unknownFiles.txt")));
        guesser.pullCDTB();
        guesser.guessShaderFiles(outputPath);
        guesser.guessAssetsBySearch(outputPath);
        guesser.guessBinByLinkedFiles(outputPath);
        guesser.saveAsJson(cachePath.resolve("game.json"));
        return guesser.didFindNewHash();
    }
    
    public boolean checkLCUHashes(Path cachePath, Path outputPath)
    {
        LCUHashGuesser guesser = new LCUHashGuesser(HashGuesser.unknownFromExportWAD(cachePath.resolve("unknownFiles.txt")));
        guesser.pullCDTB();
        guesser.addBasenameWord();
        guesser.substitutePlugins();
        guesser.substituteBasenames();
        guesser.substituteRegionLang();
        guesser.guessAssetsBySearch(outputPath);
        guesser.substituteBasenameWords(null, null, null, 1);
        guesser.saveAsJson(cachePath.resolve("lcu.json"));
        return guesser.didFindNewHash();
    }
    
    public void testCDragonWAD(Path intermediary, Path output, Path cachePath) throws IOException
    {
        generateUnknownFileList(intermediary, cachePath.resolve("unknownFiles.txt"));
        extractWads(intermediary, output);
        
        transformBIN(output);
        generateUnknownBinHashList(cachePath.resolve("unknownBins.txt"));
    }
    
    private void generateUnknownBinHashList(Path outputFile) throws IOException
    {
        System.out.println("Generating list of unknown bin hashes");
        outputFile.toFile().delete();
        
        Function<String, Boolean> canDecode = h -> {
            try
            {
                Long.decode("0x" + h);
                return true;
            } catch (Exception e)
            {
                return false;
            }
        };
        
        List<String> sortedHashes = new ArrayList<>(BINParser.hashes).stream()
                                                                     .filter(canDecode::apply)
                                                                     .filter(h -> HashHandler.getBinHashes().get(h) == null)
                                                                     .sorted()
                                                                     .collect(Collectors.toList());
        
        Files.write(outputFile, sortedHashes, StandardCharsets.UTF_8);
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
                 } catch (IOException e)
                 {
                     System.out.println(file);
                     e.printStackTrace();
                 }
             });
    }
    
    private void extractWads(Path from, Path to)
    {
        try
        {
            System.out.println("Deleting old output files");
            Runtime.getRuntime().exec(new String[]{"rm", "-rf", to.toFile().toString()}).waitFor();
            
            List<String> ends  = Arrays.asList(".wad", ".wad.client");
            List<String> endsc = Arrays.asList(".wad.compressed", ".wad.client.compressed");
            List<String> endsm = Collections.singletonList(".wad.mobile");
            
            ExecutorService                       service  = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1);
            Set<Pair<Long, Function<Void, Void>>> extracts = new TreeSet<>(Comparator.comparingLong((ToLongFunction<Pair<Long, Function<Void, Void>>>) Pair::getA).reversed());
            
            WADParser parser = new WADParser();
            Files.walk(from)
                 .filter(f -> !Files.isDirectory(f))
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
    
    
    private void generateUnknownFileList(Path from, Path saveFile) throws IOException
    {
        System.out.println("Generating unknown files list");
        
        WADParser parser = new WADParser();
        saveFile.toFile().delete();
        
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
                    parsed.printUnknownFiles(filename, saveFile);
                    return FileVisitResult.CONTINUE;
                }
                
                if (endscCount != 0)
                {
                    WADFile parsed   = parser.parseCompressed(file);
                    String  filename = String.format("%-60s", file.getParent().getFileName().toString() + "/" + file.getFileName().toString());
                    parsed.printUnknownFiles(filename, saveFile);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        
        List<String> content = new ArrayList<>();
        Files.readAllLines(saveFile).stream()
             .map(s -> new Triplet<>(s.substring(0, s.indexOf(":")).trim(), s.substring(s.indexOf(":") + 1, s.lastIndexOf(":")).trim(), s.substring(s.lastIndexOf(":") + 1).trim()))
             .collect(Collectors.groupingBy(Triplet::getA, Collectors.mapping(t -> new Pair<>(t.getC(), t.getB()), Collectors.toList())))
             .forEach((k, v) -> {
                 String line = String.format("%s : %-10s : %s", k, v.get(0).getB(), v.stream().map(Pair::getA).collect(Collectors.joining(",")));
                 content.add(line);
             });
        
        content.sort(
                Comparator.comparing((String a) -> a.substring(32).toLowerCase())
                          .thenComparing(Comparator.comparing((String a) -> a.substring(20, 30).toLowerCase()).reversed())
                          .thenComparing((String a) -> a.substring(0, 18).toLowerCase())
                    );
        Files.write(saveFile, content, StandardCharsets.UTF_8);
    }
    
    private List<String> getRemovedBundleIds(Collection<RMANFile> datas, Path bundleFolder) throws IOException
    {
        System.out.println("Calculating removed bundles");
        Set<String> keep = datas.stream().map(RMANFile::getBundleMap).flatMap(a -> a.keySet().stream()).collect(Collectors.toSet());
        List<String> has = Files.walk(bundleFolder)
                                .map(Path::getFileName)
                                .map(Path::toString)
                                .filter(s -> s.endsWith(".bundle"))
                                .map(s -> s.substring(0, 16))
                                .map(s -> s.toUpperCase(Locale.ENGLISH))
                                .collect(Collectors.toList());
        has.removeAll(keep);
        return has;
    }
    
    private List<String> getNewBundleIds(RMANFile data, Path bundleFolder) throws IOException
    {
        Set<String> keep = data.getBundleMap().keySet();
        List<String> has = Files.walk(bundleFolder)
                                .map(Path::getFileName)
                                .map(Path::toString)
                                .filter(s -> s.endsWith(".bundle"))
                                .map(s -> s.substring(0, 16))
                                .map(s -> s.toUpperCase(Locale.ENGLISH))
                                .collect(Collectors.toList());
        keep.removeAll(has);
        return new ArrayList<>(keep);
    }
    
    public void removeOldBundles(List<String> bundleIds, Path bundleFolder)
    {
        System.out.println("Found " + bundleIds.size() + " bundles that are not used in this version, deleting...");
        bundleIds.stream()
                 .map(s -> s.toUpperCase(Locale.ENGLISH))
                 .map(s -> s + ".bundle")
                 .forEach(s -> {
                     try
                     {
                         Files.deleteIfExists(bundleFolder.resolve(s));
                     } catch (IOException e)
                     {
                         e.printStackTrace();
                     }
                 });
    }
    
    private void downloadAllBundles(Collection<RMANFile> datas, Path bundleFolder) throws IOException
    {
        Files.createDirectories(bundleFolder);
        datas.forEach(f -> f.downloadBundles(f.getBody().getBundles(), bundleFolder));
    }
    
    private void downloadFileBundles(RMANFile manifest, RMANFileBodyFile file, Path bundleFolder)
    {
        Map<String, RMANFileBodyBundleChunkInfo> chunksById = manifest.getChunkMap();
        Set<RMANFileBodyBundle> bundles = file.getChunkIds()
                                              .stream()
                                              .map(c -> manifest.getBundleMap().get(chunksById.get(c).getBundleId()))
                                              .collect(Collectors.toSet());
        
        manifest.downloadBundles(bundles, bundleFolder);
    }
}
