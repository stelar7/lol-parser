package no.stelar7.cdragon.types.rman.data;

import com.github.luben.zstd.ZstdInputStream;
import com.google.gson.JsonElement;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.readers.RandomAccessReader;

import java.io.*;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
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
    
    public void printFileList()
    {
        StandardOpenOption[] options = {StandardOpenOption.WRITE, StandardOpenOption.APPEND, StandardOpenOption.CREATE};
        body.getFiles().forEach(f -> {
            try
            {
                String path = f.getFullFilepath(this) + "\n";
                Files.write(UtilHandler.DOWNLOADS_FOLDER.resolve("FILELIST.txt"), path.getBytes(StandardCharsets.UTF_8), options);
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        });
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
            System.out.println("Loading bundles needed for " + file.getName());
            Path outputName = outputFolder.resolve(file.getFullFilepath(this));
            Files.createDirectories(outputName.getParent());
            
            
            List<String>                chunkIds = file.getChunkIds();
            RMANFileBodyBundleChunkInfo current  = getChunkMap().get(chunkIds.get(0));
            
            FileOutputStream   fos = new FileOutputStream(outputName.toFile());
            RandomAccessReader raf = new RandomAccessReader(bundleFolder.resolve(current.getBundleId() + ".bundle"), ByteOrder.LITTLE_ENDIAN);
            for (int i = 0, chunkIdsSize = chunkIds.size(); i < chunkIdsSize; i++)
            {
                raf.seek(current.getOffsetToChunk());
                byte[]          compressedChunkData = raf.readBytes(current.getCompressedSize());
                ZstdInputStream is                  = new ZstdInputStream(new ByteArrayInputStream(compressedChunkData));
                is.transferTo(fos);
                
                if (i + 1 >= chunkIdsSize)
                {
                    break;
                }
                
                RMANFileBodyBundleChunkInfo next = getChunkMap().get(chunkIds.get(i + 1));
                if (!current.getBundleId().equals(next.getBundleId()))
                {
                    raf.close();
                    
                    Path nextBundle = bundleFolder.resolve(next.getBundleId() + ".bundle");
                    raf = new RandomAccessReader(nextBundle, ByteOrder.LITTLE_ENDIAN);
                }
                
                current = next;
            }
            raf.close();
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
            
            bundlePath.toFile().delete();
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
    
    public List<RMANFileBodyFile> getUnchangedFiles(RMANFile oldM)
    {
        List<RMANFileBodyFile> ignored = new ArrayList<>();
        oldM.getBody().getFiles().forEach(oldFile -> {
            
            Optional<RMANFileBodyFile> optf = getBody().getFiles().stream().filter(n -> n.getName().equalsIgnoreCase(oldFile.getName())).findAny();
            optf.ifPresent(newFile -> {
                if (oldFile.getChunkIds().equals(newFile.getChunkIds()))
                {
                    ignored.add(oldFile);
                }
            });
        });
        
        return ignored;
    }
    
    
    public List<RMANFileBodyFile> getChangedFiles(RMANFile oldM)
    {
        List<RMANFileBodyFile> ignored = new ArrayList<>();
        oldM.getBody().getFiles().forEach(oldFile -> {
            
            Optional<RMANFileBodyFile> optf = getBody().getFiles().stream().filter(n -> n.getName().equalsIgnoreCase(oldFile.getName())).findAny();
            optf.ifPresentOrElse(newFile -> {
                if (!oldFile.getChunkIds().equals(newFile.getChunkIds()))
                {
                    ignored.add(oldFile);
                }
            }, () -> ignored.add(oldFile));
        });
        
        return ignored;
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
