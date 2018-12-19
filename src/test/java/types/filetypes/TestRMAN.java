package types.filetypes;

import no.stelar7.cdragon.types.rman.*;
import no.stelar7.cdragon.types.wad.WADParser;
import no.stelar7.cdragon.types.wad.data.WADFile;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.readers.RandomAccessReader;
import no.stelar7.cdragon.util.types.*;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class TestRMAN
{
    // https://lol.dyn.riotcdn.net/channels/public/pbe-pbe-win.json
    
    @Test
    public void testRMAN() throws IOException
    {
        RMANParser parser = new RMANParser();
        
        Path file = UtilHandler.DOWNLOADS_FOLDER.resolve("cdragon\\DC9F6F78A04934D6.manifest");
        System.out.println("Parsing: " + file.toString());
        RMANFile data = parser.parse(file);
        
        List<RMANFileBodyFile> files = data.getBody().getFiles().stream().filter(f -> f.getName().contains("Map11.de")).collect(Collectors.toList());
        
        
        RMANFileBodyFile testFile = files.get(0);
        //downloadFileFullBundles(data, testFile);
        downloadFileRanged(data, testFile);
        
        Path extractedPath = UtilHandler.DOWNLOADS_FOLDER.resolve("cdragon/bundles/files/");
        
        WADParser wp = new WADParser();
        WADFile   wf = wp.parse(extractedPath.resolve(testFile.getName()));
        wf.extractFiles("levels", testFile.getName(), extractedPath);
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
        Path bundleFolder = UtilHandler.DOWNLOADS_FOLDER.resolve("cdragon\\bundles");
        Files.createDirectories(bundleFolder);
        downloadRanges.forEach((k, v) -> WebHandler.downloadBundleBytes(k, v, bundleFolder));
        
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
            System.out.println("Downloading bundle: " + bundleId + " (" + ++count + "/" + bundlesNeeded.size() + ")");
            WebHandler.downloadBundle(bundleId, bundlePath);
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
    
    private void extractAllChunks(RMANFileBodyBundle bundle, String bundleId, Path bundlePath) throws IOException
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
    }
}
