package types.filetypes;

import com.google.gson.JsonObject;
import no.stelar7.cdragon.types.rman.*;
import no.stelar7.cdragon.types.rman.data.*;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.readers.RandomAccessReader;
import org.junit.Test;

import java.io.*;
import java.nio.ByteOrder;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class TestRMAN
{
    
    @Test
    public void testRMAN() throws Exception
    {
        RMANParser parser = new RMANParser();
        
        System.out.println("Downloading patcher manifest");
        String patcherUrl    = "https://lol.dyn.riotcdn.net/channels/public/pbe-pbe-win.json";
        String patchManifest = String.join("\n", WebHandler.readWeb(patcherUrl));
        
        JsonObject obj     = UtilHandler.getJsonParser().parse(patchManifest).getAsJsonObject();
        int        version = obj.get("version").getAsInt();
        System.out.println("Found patch version " + version);
        
        System.out.println("Downloading bundle manifest");
        String manifestUrl = obj.get("game_patch_url").getAsString();
        
        System.out.println("Parsing Manifest");
        RMANFile data = parser.parse(WebHandler.readBytes(manifestUrl));
        
        int[] counter = {0};
        removeOldBundles(data);
        downloadAllBundles(data);
        
        // does not include .exe and .dll files
        Map<String, List<RMANFileBodyFile>> filesPerLang = data.getBody()
                                                               .getFiles()
                                                               .stream()
                                                               .filter(f -> f.getName().substring(f.getName().indexOf('.') + 1).contains("."))
                                                               .collect(Collectors.groupingBy(
                                                                       f -> f.getName().substring(f.getName().indexOf('.') + 1, f.getName().indexOf('.', f.getName().indexOf('.') + 1))));
        
        
        long allocatedMemory      = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long presumableFreeMemory = Runtime.getRuntime().maxMemory() - allocatedMemory;
        int  suggestedThreadCount = (int) (Math.floorDiv(presumableFreeMemory, 500_000_000) / 4);
        
        // This is ran in a pool to limit the memory usage (sets the concurrent threads to parallelism + 1, instead of core count (12 in my case))
        ForkJoinPool forkJoinPool = new ForkJoinPool(Math.max(suggestedThreadCount, 1));
        forkJoinPool.submit(() -> data.getBody()
                                      .getFiles()
                                      .parallelStream()
                                      .forEach(f ->
                                               {
                                                   counter[0]++;
                                                   extractFile(data, f);
                                                   System.out.format("Extracting file %s of %s%n", counter[0], data.getBody().getFiles().size());
                                               })).get();
        forkJoinPool.shutdown();
        
        TestWAD tw = new TestWAD();
        tw.testCDragonWAD();
    }
    
    public void removeOldBundles(RMANFile data) throws IOException
    {
        Path        bundleFolder = UtilHandler.DOWNLOADS_FOLDER.resolve("cdragon\\bundles");
        Set<String> keep         = data.getBundleMap().keySet();
        List<String> has = Files.walk(bundleFolder)
                                .map(Path::getFileName)
                                .map(Path::toString)
                                .filter(s -> s.endsWith(".bundle"))
                                .map(s -> s.substring(0, 16))
                                .map(s -> s.toUpperCase(Locale.ENGLISH))
                                .collect(Collectors.toList());
        
        System.out.println("Found " + has.size() + " bundle files");
        has.removeAll(keep);
        
        System.out.println(has.size() + " are not used in the current version, so we delete them");
        has.stream()
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
    
    private void downloadAllBundles(RMANFile manifest) throws IOException
    {
        downloadBundles(manifest.getBody().getBundles());
    }
    
    private void downloadFileBundles(RMANFile manifest, RMANFileBodyFile file) throws IOException
    {
        Map<String, RMANFileBodyBundleChunkInfo> chunksById = manifest.getChunkMap();
        List<RMANFileBodyBundle> bundles = file.getChunkIds()
                                               .stream()
                                               .map(c -> manifest.getBundleMap().get(chunksById.get(c).getBundleId()))
                                               .collect(Collectors.toList());
        
        downloadBundles(bundles);
    }
    
    private void extractFile(RMANFile manifest, RMANFileBodyFile file)
    {
        try
        {
            Path bundleFolder = UtilHandler.DOWNLOADS_FOLDER.resolve("cdragon\\bundles");
            Path fileFolder   = UtilHandler.DOWNLOADS_FOLDER.resolve("extractedFiles");
            Path outputName   = fileFolder.resolve(file.getFullFilepath(manifest));
            Files.createDirectories(outputName.getParent());
            
            System.out.println("Loading bundles needed for " + file.getName());
            
            List<String>                chunkIds = file.getChunkIds();
            ByteArrayOutputStream       bos      = new ByteArrayOutputStream();
            RMANFileBodyBundleChunkInfo current  = manifest.getChunkMap().get(chunkIds.get(0));
            RandomAccessReader          raf      = new RandomAccessReader(bundleFolder.resolve(current.getBundleId() + ".bundle"), ByteOrder.LITTLE_ENDIAN);
            for (int i = 0, chunkIdsSize = chunkIds.size(); i < chunkIdsSize; i++)
            {
                raf.seek(current.getOffsetToChunk());
                byte[] compressedChunkData = raf.readBytes(current.getCompressedSize());
                byte[] uncompressedData    = CompressionHandler.uncompressZSTD(compressedChunkData);
                bos.write(uncompressedData);
                
                if (i + 1 >= chunkIdsSize)
                {
                    break;
                }
                
                RMANFileBodyBundleChunkInfo next = manifest.getChunkMap().get(chunkIds.get(i + 1));
                if (!current.getBundleId().equals(next.getBundleId()))
                {
                    raf = new RandomAccessReader(bundleFolder.resolve(next.getBundleId() + ".bundle"), ByteOrder.LITTLE_ENDIAN);
                }
                
                current = next;
            }
            Files.write(outputName, bos.toByteArray(), StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            
        } catch (IOException e1)
        {
            e1.printStackTrace();
        }
    }
    
    private void downloadBundles(List<RMANFileBodyBundle> bundles) throws IOException
    {
        Path bundleFolder = UtilHandler.DOWNLOADS_FOLDER.resolve("cdragon\\bundles");
        Files.createDirectories(bundleFolder);
        
        AtomicInteger count = new AtomicInteger();
        System.out.println("Downloading bundles");
        
        bundles.parallelStream().forEach(bundle -> {
            
            String bundleId   = bundle.getBundleId();
            Path   bundlePath = bundleFolder.resolve(bundleId + ".bundle");
            long   bundleSize = bundle.getChunks().stream().mapToLong(RMANFileBodyBundleChunk::getCompressedSize).sum();
            
            if (!WebHandler.shouldDownloadBundle(bundleId, bundlePath, bundleSize))
            {
                System.out.println("Skipping bundle: " + bundleId + " (" + count.incrementAndGet() + "/" + bundles.size() + ")");
                return;
            }
            
            if (Files.exists(bundlePath))
            {
                bundlePath.toFile().delete();
            }
            
            System.out.println("Downloading bundle: " + bundleId + " (" + count.incrementAndGet() + "/" + bundles.size() + ")");
            WebHandler.downloadBundle(bundleId, bundlePath);
        });
    }
}
