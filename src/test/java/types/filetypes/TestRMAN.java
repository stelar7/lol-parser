package types.filetypes;

import no.stelar7.cdragon.types.rman.*;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.readers.RandomAccessReader;
import no.stelar7.cdragon.util.types.*;
import org.junit.Test;

import java.io.*;
import java.nio.ByteOrder;
import java.nio.file.*;
import java.util.*;

public class TestRMAN
{
    
    @Test
    public void testRMAN() throws IOException
    {
        RMANParser parser = new RMANParser();
        
        System.out.println("Downloading patcher manifest");
        String patcherUrl    = "https://lol.dyn.riotcdn.net/channels/public/pbe-pbe-win.json";
        String patchManifest = String.join("\n", WebHandler.readWeb(patcherUrl));
        
        System.out.println("Downloading bundle manifest");
        String manifestUrl = UtilHandler.getJsonParser().parse(patchManifest).getAsJsonObject().get("patch_url").getAsString();
        
        System.out.println("Parsing Manifest");
        RMANFile data = parser.parse(WebHandler.readBytes(manifestUrl));
        
        downloadAllBundles(data);
        
        //List<RMANFileBodyFile> files = data.getBody().getFiles().stream().filter(f -> f.getName().contains("Map11.de")).collect(Collectors.toList());
        //RMANFileBodyFile testFile = files.get(0);
        //downloadFileFullBundles(data, testFile);
        //downloadFileRanged(data, testFile);
        
        //Path extractedPath = UtilHandler.DOWNLOADS_FOLDER.resolve("cdragon/bundles/files/");
        
        //WADParser wp = new WADParser();
        //WADFile   wf = wp.parse(extractedPath.resolve(testFile.getName()));
        //wf.extractFiles("levels", testFile.getName(), extractedPath);
    }
    
    private void downloadAllBundles(RMANFile manifest) throws IOException
    {
        Path bundleFolder = UtilHandler.DOWNLOADS_FOLDER.resolve("cdragon\\bundles");
        Files.createDirectories(bundleFolder);
        
        int count = 0;
        System.out.println("Downloading bundles");
        
        /*
        manifest.getBody()
                .getBundles()
                .parallelStream()
                .filter(b -> WebHandler.shouldDownloadBundle(b.getBundleId(), bundleFolder.resolve(b.getBundleId() + ".bundle"), b.getChunks().stream().mapToLong(RMANFileBodyBundleChunk::getCompressedSize).sum()))
                .forEach(b -> WebHandler.downloadBundle(b.getBundleId(), bundleFolder.resolve(b.getBundleId() + ".bundle")));
        */
        
        for (RMANFileBodyBundle bundle : manifest.getBody().getBundles())
        {
            String bundleId   = bundle.getBundleId();
            Path   bundlePath = bundleFolder.resolve(bundleId + ".bundle");
            long   bundleSize = bundle.getChunks().stream().mapToLong(RMANFileBodyBundleChunk::getCompressedSize).sum();
            
            if (!WebHandler.shouldDownloadBundle(bundleId, bundlePath, bundleSize))
            {
                continue;
            }
            
            System.out.println("Downloading bundle: " + bundleId + " (" + ++count + "/" + manifest.getBody().getBundles().size() + ")");
            WebHandler.downloadBundle(bundleId, bundlePath);
        }
    }
    
    private void downloadFileRanged(RMANFile manifest, RMANFileBodyFile file) throws IOException
    {
        System.out.println("Loading bundles needed for " + file.getName());
        
        Map<String, RMANFileBodyBundleChunkInfo> chunksById = manifest.getChunkMap();
        
        // extract the needed chunks
        Map<String, List<LongRange>> downloadRanges = new HashMap<>();
        for (String chunkId : file.getChunkIds())
        {
            RMANFileBodyBundleChunkInfo data   = chunksById.get(chunkId);
            List<LongRange>             ranges = downloadRanges.computeIfAbsent(data.getBundleId(), (k) -> new ArrayList<>());
            ranges.add(new LongRange(data.getOffsetToChunk(), data.getOffsetToChunk() + data.getCompressedSize()));
        }
        
        // reduce the map to continuus ranges
        System.out.println("Reducing to HTTP-bytes request");
        for (String key : downloadRanges.keySet())
        {
            List<LongRange> ranges = downloadRanges.get(key);
            ranges.sort(Comparator.comparing(LongRange::getFrom));
            
            for (int i = 0; i < ranges.size() - 1; i++)
            {
                LongRange start = ranges.get(i);
                LongRange end   = ranges.get(i + 1);
                if (start.getTo() == end.getFrom())
                {
                    LongRange joined = new LongRange(start.getFrom(), end.getTo());
                    
                    ranges.remove(start);
                    ranges.remove(end);
                    ranges.add(0, joined);
                    i--;
                    
                    ranges.sort(Comparator.comparing(LongRange::getFrom));
                }
            }
        }
        
        
        // download the ranges
        System.out.println("Downloading files...");
        Path bundleFolder = UtilHandler.DOWNLOADS_FOLDER.resolve("cdragon\\bundles");
        Files.createDirectories(bundleFolder);
        downloadRanges.entrySet().parallelStream().forEach((e) -> WebHandler.downloadBundleBytes(e.getKey(), e.getValue(), bundleFolder));
        
        // TODO split the range into chunks, and merge the chunks back into the file
        System.out.println();
        
    }
    
    private void downloadFileFullBundles(RMANFile manifest, RMANFileBodyFile file) throws IOException
    {
        System.out.println("Loading bundles needed for " + file.getName());
        Set<RMANFileBodyBundle> bundlesNeeded = new HashSet<>();
        outer:
        for (String chunkId : file.getChunkIds())
        {
            for (RMANFileBodyBundle bundle : manifest.getBody().getBundles())
            {
                Optional<RMANFileBodyBundleChunk> chunk = bundle.getChunks().stream().filter(c -> c.getChunkId().equals(chunkId)).findAny();
                if (chunk.isPresent())
                {
                    bundlesNeeded.add(bundle);
                    continue outer;
                }
            }
        }
        
        
        Path bundleFolder = UtilHandler.DOWNLOADS_FOLDER.resolve("cdragon\\bundles");
        Files.createDirectories(bundleFolder);
        
        int count = 0;
        System.out.println("Downloading bundles");
        for (RMANFileBodyBundle bundle : bundlesNeeded)
        {
            String bundleId   = bundle.getBundleId();
            Path   bundlePath = bundleFolder.resolve(bundleId + ".bundle");
            long   bundleSize = bundle.getChunks().stream().mapToLong(RMANFileBodyBundleChunk::getCompressedSize).sum();
            
            System.out.println("Downloading bundle: " + bundleId + " (" + ++count + "/" + bundlesNeeded.size() + ")");
            if (!WebHandler.shouldDownloadBundle(bundleId, bundlePath, bundleSize))
            {
                WebHandler.downloadBundle(bundleId, bundlePath);
            }
            
            extractAllChunks(bundle, bundleId, bundlePath);
        }
        
        Path chunkFolder = bundleFolder.resolve("chunks");
        Path fileFolder  = bundleFolder.resolve("files");
        Files.createDirectories(chunkFolder);
        Files.createDirectories(fileFolder);
        
        System.out.println("Creating file from chunks");
        for (String chunkId : file.getChunkIds())
        {
            byte[] input = Files.readAllBytes(chunkFolder.resolve(chunkId + ".chunk"));
            Files.write(fileFolder.resolve(file.getName()), input, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
    }
    
    private void extractAllChunks(RMANFileBodyBundle bundle, String bundleId, Path bundlePath)
    {
        try
        {
            
            Path chunkFolder = bundlePath.resolveSibling("chunks");
            Files.createDirectories(chunkFolder);
            RandomAccessReader raf = new RandomAccessReader(bundlePath, ByteOrder.LITTLE_ENDIAN);
            
            for (RMANFileBodyBundleChunk chunk : bundle.getChunks())
            {
                String chunkId = chunk.getChunkId();
                
                byte[] compressedChunkData = raf.readBytes(chunk.getCompressedSize());
                byte[] uncompressedData    = CompressionHandler.uncompressZSTD(compressedChunkData);
                Files.write(chunkFolder.resolve(chunkId + ".chunk"), uncompressedData);
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
