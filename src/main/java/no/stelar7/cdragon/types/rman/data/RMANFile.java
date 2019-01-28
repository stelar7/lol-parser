package no.stelar7.cdragon.types.rman.data;

import com.google.gson.JsonElement;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.readers.RandomAccessReader;

import java.io.*;
import java.nio.ByteOrder;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RMANFile
{
    private RMANFileHeader header;
    private RMANFileBody   body;
    private byte[]         compressedBody;
    private byte[]         signature;
    
    private Map<String, RMANFileBodyBundleChunkInfo> chunksById  = null;
    private Map<String, RMANFileBodyBundle>          bundlesById = null;
    
    public void buildChunkMap()
    {
        chunksById = new HashMap<>();
        for (RMANFileBodyBundle bundle : getBody().getBundles())
        {
            int currentIndex = 0;
            for (RMANFileBodyBundleChunk chunk : bundle.getChunks())
            {
                RMANFileBodyBundleChunkInfo chunkInfo = new RMANFileBodyBundleChunkInfo(bundle.getBundleId(), chunk.getChunkId(), currentIndex, chunk.getCompressedSize());
                chunksById.put(chunk.getChunkId(), chunkInfo);
                
                currentIndex += chunk.getCompressedSize();
            }
        }
    }
    
    public void buildBundleMap()
    {
        bundlesById = new HashMap<>();
        for (RMANFileBodyBundle bundle : getBody().getBundles())
        {
            bundlesById.put(bundle.getBundleId(), bundle);
        }
    }
    
    public Set<RMANFileBodyBundle> getBundlesForFile(RMANFileBodyFile file)
    {
        return file.getChunkIds()
                   .stream()
                   .map(getChunkMap()::get).collect(Collectors.toList())
                   .stream()
                   .map(RMANFileBodyBundleChunkInfo::getBundleId)
                   .map(getBundleMap()::get)
                   .collect(Collectors.toSet());
    }
    
    public RMANFileHeader getHeader()
    {
        return header;
    }
    
    public void setHeader(RMANFileHeader header)
    {
        this.header = header;
    }
    
    public byte[] getCompressedBody()
    {
        return compressedBody;
    }
    
    public void setCompressedBody(byte[] compressedBody)
    {
        this.compressedBody = compressedBody;
    }
    
    public byte[] getSignature()
    {
        return signature;
    }
    
    public void setSignature(byte[] signature)
    {
        this.signature = signature;
    }
    
    public RMANFileBody getBody()
    {
        return body;
    }
    
    public void setBody(RMANFileBody body)
    {
        this.body = body;
    }
    
    public Map<String, RMANFileBodyBundleChunkInfo> getChunkMap()
    {
        return chunksById;
    }
    
    public Map<String, RMANFileBodyBundle> getBundleMap()
    {
        return bundlesById;
    }
    
    public void extractFile(RMANFileBodyFile file, Path bundleFolder, Path outputFolder)
    {
        try
        {
            Path outputName = outputFolder.resolve(file.getFullFilepath(this));
            Files.createDirectories(outputName.getParent());
            
            System.out.println("Loading bundles needed for " + file.getName());
            
            List<String>                chunkIds = file.getChunkIds();
            ByteArrayOutputStream       bos      = new ByteArrayOutputStream();
            RMANFileBodyBundleChunkInfo current  = getChunkMap().get(chunkIds.get(0));
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
                
                RMANFileBodyBundleChunkInfo next = getChunkMap().get(chunkIds.get(i + 1));
                if (!current.getBundleId().equals(next.getBundleId()))
                {
                    raf = new RandomAccessReader(bundleFolder.resolve(next.getBundleId() + ".bundle"), ByteOrder.LITTLE_ENDIAN);
                }
                
                current = next;
            }
            Files.write(outputName, bos.toByteArray());
            
        } catch (IOException e1)
        {
            e1.printStackTrace();
        }
    }
    
    public void downloadBundles(Collection<RMANFileBodyBundle> bundles, Path bundleFolder)
    {
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
    
    
    public Map<String, List<RMANFileBodyFile>> getChampionFilesByLanguage()
    {
        List<String> champKeys = new ArrayList<>();
        
        String chamsum = String.join("", WebHandler.readWeb("https://raw.communitydragon.org/latest/plugins/rcp-be-lol-game-data/global/default/v1/champion-summary.json"));
        for (JsonElement element : UtilHandler.getJsonParser().parse(chamsum).getAsJsonArray())
        {
            champKeys.add(element.getAsJsonObject().get("alias").getAsString());
        }
        
        List<RMANFileBodyFile> files = getBody().getFiles()
                                                .stream()
                                                .filter(a -> champKeys.stream().anyMatch(k -> a.getName().startsWith(k)))
                                                .collect(Collectors.toList());
        
        Function<String, String> fixName = (input) -> {
            
            String value = input.substring(input.indexOf('.') + 1);
            if (value.equalsIgnoreCase("wad.client"))
            {
                return "";
            }
            
            return value.replace(".wad.client", "").toLowerCase(Locale.ENGLISH);
        };
        
        return files.stream().collect(Collectors.groupingBy(f -> fixName.apply(f.getName()), Collectors.mapping(f -> f, Collectors.toList())));
    }
    
    
    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        RMANFile rmanFile = (RMANFile) o;
        return Objects.equals(header, rmanFile.header) &&
               Arrays.equals(compressedBody, rmanFile.compressedBody) &&
               Arrays.equals(signature, rmanFile.signature) &&
               Objects.equals(body, rmanFile.body);
    }
    
    @Override
    public int hashCode()
    {
        int result = Objects.hash(header, body);
        result = 31 * result + Arrays.hashCode(compressedBody);
        result = 31 * result + Arrays.hashCode(signature);
        return result;
    }
    
    @Override
    @SuppressWarnings("ImplicitArrayToString")
    public String toString()
    {
        return "RMANFile{" +
               "header=" + header +
               ", compressedBody=" + compressedBody +
               ", signature=" + signature +
               ", body=" + body +
               '}';
    }
}
