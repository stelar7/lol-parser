package types.filetypes;

import no.stelar7.cdragon.types.rman.*;
import no.stelar7.cdragon.types.wad.WADParser;
import no.stelar7.cdragon.types.wad.data.WADFile;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.readers.RandomAccessReader;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.*;
import java.util.*;

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
        
        
        RMANFileBodyFile testFile = data.getBody().getFiles().get(5);
        downloadFile(data, testFile);
        
        Path extractedPath = UtilHandler.DOWNLOADS_FOLDER.resolve("cdragon/bundles/files/");
        
        WADParser wp = new WADParser();
        WADFile   wf = wp.parse(extractedPath.resolve(testFile.getName()));
        wf.extractFiles("champions", testFile.getName(), extractedPath);
    }
    
    private void downloadFile(RMANFile manifest, RMANFileBodyFile file) throws IOException
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
