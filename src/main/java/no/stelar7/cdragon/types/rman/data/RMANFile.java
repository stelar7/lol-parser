package no.stelar7.cdragon.types.rman.data;

import com.github.luben.zstd.Zstd;
import com.google.gson.*;
import no.stelar7.cdragon.util.ProgressBar;
import no.stelar7.cdragon.util.handlers.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class RMANFile
{
    private final RMANFileHeader header;
    private final RMANFileBody   body;
    private final byte[]         compressedBody;
    private final byte[]         signature;
    
    // lookup maps, built by buildChunkMap and buildBundleMap
    private Map<Long, RMANFileBodyBundleChunkInfo> chunksById  = null;
    private Map<String, RMANFileBodyBundle>        bundlesById = null;
    
    // lazily built by getDirectory
    private transient Map<Long, RMANFileBodyDirectory> directoriesById = null;
    
    /**
     * @param signature the signature after the body, or null if the file is not signed
     */
    public RMANFile(RMANFileHeader header, RMANFileBody body, byte[] compressedBody, byte[] signature)
    {
        this.header = header;
        this.body = body;
        this.compressedBody = compressedBody;
        this.signature = signature;
    }
    
    public void buildChunkMap()
    {
        int chunkCount = getBody().getBundles().stream().mapToInt(b -> b.getChunks().size()).sum();
        chunksById = new HashMap<>((int) (chunkCount / 0.75f) + 1);
        for (RMANFileBodyBundle bundle : getBody().getBundles())
        {
            long currentIndex = 0;
            for (RMANFileBodyBundleChunk chunk : bundle.getChunks())
            {
                RMANFileBodyBundleChunkInfo chunkInfo = new RMANFileBodyBundleChunkInfo(bundle.getBundleId(), chunk.getChunkId(), currentIndex, chunk.getCompressedSize(), chunk.getUncompressedSize());
                chunksById.put(chunk.getChunkId(), chunkInfo);
                
                currentIndex += chunk.getCompressedSize();
            }
        }
    }
    
    /**
     * Returns the directory with this id, or null if there is none
     */
    public synchronized RMANFileBodyDirectory getDirectory(long directoryId)
    {
        if (directoriesById == null)
        {
            directoriesById = new HashMap<>();
            for (RMANFileBodyDirectory directory : getBody().getDirectories())
            {
                directoriesById.putIfAbsent(directory.getDirectoryId(), directory);
            }
        }
        return directoriesById.get(directoryId);
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
        return Arrays.stream(file.getChunkIdArray())
                   .mapToObj(getChunkMap()::get)
                   .map(RMANFileBodyBundleChunkInfo::getBundleId)
                   .map(getBundleMap()::get)
                   .collect(Collectors.toSet());
    }
    
    public void printFileList()
    {
        try
        {
            StandardOpenOption[] options = {StandardOpenOption.WRITE, StandardOpenOption.APPEND, StandardOpenOption.CREATE};
            List<String>         lines   = body.getFiles().stream().map(f -> f.getFullFilepath(this)).collect(Collectors.toList());
            Files.write(UtilHandler.CDRAGON_FOLDER.resolve("FILELIST.txt"), lines, StandardCharsets.UTF_8, options);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    public RMANFileHeader getHeader()
    {
        return header;
    }
    
    public byte[] getCompressedBody()
    {
        return compressedBody;
    }
    
    public byte[] getSignature()
    {
        return signature;
    }
    
    public RMANFileBody getBody()
    {
        return body;
    }
    
    public Map<Long, RMANFileBodyBundleChunkInfo> getChunkMap()
    {
        return chunksById;
    }
    
    public Map<String, RMANFileBodyBundle> getBundleMap()
    {
        return bundlesById;
    }
    
    public void extractFile(RMANFileBodyFile file, Path bundleFolder, Path outputFolder)
    {
        Path outputName = outputFolder.resolve(file.getFullFilepath(this));

        // bundles are opened once each, and only the needed chunks are read from them
        Map<String, FileChannel> bundles = new HashMap<>();
        try
        {
            Files.createDirectories(outputName.getParent());
        } catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }

        try (OutputStream fos = new BufferedOutputStream(Files.newOutputStream(outputName)))
        {
            for (int i = 0; i < file.getChunkCount(); i++)
            {
                long                        chunkId = file.getChunkId(i);
                RMANFileBodyBundleChunkInfo chunk   = getChunkMap().get(chunkId);
                if (chunk == null)
                {
                    throw new IllegalStateException("Chunk " + HashHandler.toHex(chunkId, 16) + " of " + file.getName() + " is not in any bundle");
                }

                FileChannel channel = bundles.get(chunk.getBundleId());
                if (channel == null)
                {
                    channel = FileChannel.open(bundleFolder.resolve(chunk.getBundleId() + ".bundle"), StandardOpenOption.READ);
                    bundles.put(chunk.getBundleId(), channel);
                }

                ByteBuffer compressed = ByteBuffer.allocate(Math.toIntExact(chunk.getCompressedSize()));
                long       position   = chunk.getOffsetToChunk();
                while (compressed.hasRemaining())
                {
                    int read = channel.read(compressed, position + compressed.position());
                    if (read < 0)
                    {
                        throw new EOFException("Bundle " + chunk.getBundleId() + " ended before chunk " + HashHandler.toHex(chunkId, 16));
                    }
                }

                fos.write(Zstd.decompress(compressed.array(), Math.toIntExact(chunk.getUncompressedSize())));
            }
        } catch (IOException e1)
        {
            e1.printStackTrace();
            throw new UncheckedIOException(e1);
        } finally
        {
            for (FileChannel channel : bundles.values())
            {
                try
                {
                    channel.close();
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }
    
    public void downloadBundles(Collection<RMANFileBodyBundle> bundles, Path bundleFolder)
    {
        AtomicInteger downloaded = new AtomicInteger();
        ProgressBar   progress   = new ProgressBar("Downloading bundles", bundles.size());
        
        bundles.parallelStream().forEach(bundle -> {
            
            String bundleId   = bundle.getBundleId();
            Path   bundlePath = bundleFolder.resolve(bundleId + ".bundle");
            long   bundleSize = bundle.getChunks().stream().mapToLong(RMANFileBodyBundleChunk::getCompressedSize).sum();
            
            if (WebHandler.shouldDownloadBundle(bundleId, bundlePath, bundleSize))
            {
                bundlePath.toFile().delete();
                WebHandler.downloadBundle(bundleId, bundlePath);
                downloaded.incrementAndGet();
            }
            
            progress.step();
        });
        
        progress.finish();
        System.out.println("Downloaded " + downloaded.get() + " bundles, " + (bundles.size() - downloaded.get()) + " were already up to date");
    }
    
    
    public Map<String, List<RMANFileBodyFile>> getChampionFilesByLanguage()
    {
        List<String> champKeys = new ArrayList<>();
        
        String champSum = String.join("", WebHandler.readWeb("https://raw.communitydragon.org/latest/plugins/rcp-be-lol-game-data/global/default/v1/champion-summary.json"));
        for (JsonElement element : JsonParser.parseString(champSum).getAsJsonArray())
        {
            champKeys.add(element.getAsJsonObject().get("alias").getAsString());
        }
        
        List<RMANFileBodyFile> files = getBody().getFiles()
                                                .stream()
                                                .filter(a -> champKeys.stream().anyMatch(k -> a.getName().startsWith(k)))
                                                .toList();
        
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
    
    /**
     * Files of this manifest by lowercase name (the first one, if several share a name)
     */
    private Map<String, RMANFileBodyFile> getFilesByLowercaseName()
    {
        Map<String, RMANFileBodyFile> byName = new HashMap<>();
        for (RMANFileBodyFile file : getBody().getFiles())
        {
            byName.putIfAbsent(file.getName().toLowerCase(Locale.ROOT), file);
        }
        return byName;
    }

    public List<RMANFileBodyFile> getUnchangedFiles(RMANFile oldM)
    {
        Map<String, RMANFileBodyFile> newFiles = getFilesByLowercaseName();
        List<RMANFileBodyFile>        ignored  = new ArrayList<>();
        for (RMANFileBodyFile oldFile : oldM.getBody().getFiles())
        {
            RMANFileBodyFile newFile = newFiles.get(oldFile.getName().toLowerCase(Locale.ROOT));
            if (newFile != null && oldFile.hasSameChunks(newFile))
            {
                ignored.add(oldFile);
            }
        }

        return ignored;
    }


    public List<RMANFileBodyFile> getChangedFiles(RMANFile oldM)
    {
        Map<String, RMANFileBodyFile> newFiles = getFilesByLowercaseName();
        List<RMANFileBodyFile>        ignored  = new ArrayList<>();
        for (RMANFileBodyFile oldFile : oldM.getBody().getFiles())
        {
            RMANFileBodyFile newFile = newFiles.get(oldFile.getName().toLowerCase(Locale.ROOT));
            if (newFile == null || !oldFile.hasSameChunks(newFile))
            {
                ignored.add(oldFile);
            }
        }

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
