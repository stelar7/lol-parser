package types.filetypes;

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
        
        System.out.println("Downloading bundle manifest");
        String manifestUrl = UtilHandler.getJsonParser().parse(patchManifest).getAsJsonObject().get("game_patch_url").getAsString();
        
        System.out.println("Parsing Manifest");
        RMANFile data = parser.parse(WebHandler.readBytes(manifestUrl));
        
        /*
        RMANFileBodyFile file = data.getBody().getFiles().stream().filter(f -> f.getName().contains("Yorick.cs_CZ")).findAny().get();
        downloadFileBundles(data, file);
        extractFile(data, file);
        */
        
        int[] counter = {0};
        downloadAllBundles(data);
        
        // This is ran in a pool to limit the concurrent threads to 6 (parallelism + 1)
        ForkJoinPool forkJoinPool = new ForkJoinPool(5);
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
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            for (String chunkId : file.getChunkIds())
            {
                RMANFileBodyBundleChunkInfo info = manifest.getChunkMap().get(chunkId);
                RandomAccessReader          raf  = new RandomAccessReader(bundleFolder.resolve(info.getBundleId() + ".bundle"), ByteOrder.LITTLE_ENDIAN);
                raf.seek(info.getOffsetToChunk());
                byte[] compressedChunkData = raf.readBytes(info.getCompressedSize());
                byte[] uncompressedData    = CompressionHandler.uncompressZSTD(compressedChunkData);
                bos.write(uncompressedData);
            }
            
            Files.write(outputName, bos.toByteArray(), StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e)
        {
            e.printStackTrace();
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
