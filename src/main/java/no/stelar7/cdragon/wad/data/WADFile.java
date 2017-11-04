package no.stelar7.cdragon.wad.data;

import lombok.*;
import net.sf.jmimemagic.*;
import no.stelar7.cdragon.util.*;
import no.stelar7.cdragon.wad.data.content.*;
import no.stelar7.cdragon.wad.data.header.WADHeaderBase;

import java.io.*;
import java.nio.ByteOrder;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

@Data
public class WADFile
{
    @Setter(value = AccessLevel.NONE)
    @Getter(value = AccessLevel.NONE)
    private final RAFReader fileReader;
    
    private WADHeaderBase header;
    private List<WADContentHeaderV1> contentHeaders = new ArrayList<>();
    
    public WADFile(RAFReader raf)
    {
        this.fileReader = raf;
    }
    
    public void extractFiles(Path outputPath)
    {
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        
        final int interval = (int) Math.ceil(getContentHeaders().size() / 20f);
        
        for (int index = 0; index < getContentHeaders().size(); index++)
        {
            final int selfIndex = index;
            executor.submit(() ->
                            {
                                WADContentHeaderV1 fileHeader = getContentHeaders().get(selfIndex);
                
                                if (getHeader().getMajor() > 1 && ((WADContentHeaderV2) fileHeader).isDuplicate())
                                {
                                    return;
                                }
                
                                saveFile(fileHeader, outputPath);
                
                                if (selfIndex % interval == 0)
                                {
                                    System.out.println(selfIndex + "/" + getContentHeaders().size());
                                }
                
                            });
        }
        
        try
        {
            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
            fileReader.close();
        } catch (InterruptedException | IOException e)
        {
            e.printStackTrace();
        }
    }
    
    private void saveFile(WADContentHeaderV1 header, Path savePath)
    {
        try
        {
            String hash             = Long.toUnsignedString(header.getPathHash(), 16);
            String filename         = UtilHandler.getKnownFileHashes().getOrDefault(hash, "unknown/" + hash);
            String adjustedFileName = filename.startsWith("/") ? ("." + filename) : filename;
            Path   self             = savePath.resolve(adjustedFileName).normalize();
            
            self.getParent().toFile().mkdirs();
            
            byte[] data = findHeaderData(header);
            Files.write(self, data);
            
            if ("unknown".equals(self.getParent().getFileName().toString()))
            {
                findFileTypeAndRename(self, data, adjustedFileName, savePath);
            }
            
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    private synchronized byte[] findHeaderData(WADContentHeaderV1 header) throws IOException
    {
        fileReader.seek(header.getOffset());
        if (header.isCompressed())
        {
            return CompressionHandler.uncompressGZIP(fileReader.readBytes(header.getCompressedFileSize()));
        } else
        {
            return fileReader.readBytes(header.getFileSize());
        }
    }
    
    private void findFileTypeAndRename(Path self, byte[] data, String filename, Path parent)
    {
        try
        {
            StringBuilder sb = new StringBuilder(filename);
            
            MagicMatch match = Magic.getMagicMatch(data);
            if (match != null)
            {
                sb.append(".");
                
                if (!match.getExtension().isEmpty())
                {
                    sb.append(match.getExtension());
                } else
                {
                    // JMimeMagic can find _most_ types, but not newer ones, so we check the magic number for those
                    try (RAFReader raf2 = new RAFReader(new RandomAccessFile(self.toFile(), "r"), ByteOrder.LITTLE_ENDIAN))
                    {
                        ByteArrayWrapper magic     = new ByteArrayWrapper(raf2.readBytes(4));
                        String           extention = UtilHandler.getMagicNumbers().getOrDefault(magic, "txt");
                        sb.append(extention);
                    }
                }
                
                Path other = parent.resolve(sb.toString()).normalize();
                Files.move(self, other, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (MagicParseException | MagicMatchNotFoundException | MagicException | IOException e)
        {
            System.err.println("Magic didnt find extension for hash: " + filename + ", you should try with FILE");
            e.printStackTrace();
        }
    }
    
}
