package no.stelar7.cdragon.wad.data;

import lombok.*;
import no.stelar7.cdragon.util.*;
import no.stelar7.cdragon.wad.data.content.*;
import no.stelar7.cdragon.wad.data.header.WADHeaderBase;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

@Data
public class WADFile
{
    private String unknownHashContainer = "unknown.json";
    
    @Setter(value = AccessLevel.NONE)
    @Getter(value = AccessLevel.NONE)
    private final RandomAccessReader fileReader;
    private       WADHeaderBase      header;
    
    private List<WADContentHeaderV1> contentHeaders = new ArrayList<>();
    
    public WADFile(RandomAccessReader raf)
    {
        this.fileReader = raf;
    }
    
    public void extractFiles(String pluginName, String wadName, Path path)
    {
        if (wadName == null)
        {
            wadName = "assets.wad";
        }
        System.out.println("Extracting files from " + pluginName + "/" + wadName);
        final Path outputPath = path.resolve(pluginName);
        
        Path ukp = outputPath.resolve(unknownHashContainer);
        try
        {
            if (!Files.exists(ukp))
            {
                Files.createDirectories(ukp.getParent());
                Files.createFile(ukp);
            }
            Files.write(ukp, new byte[]{});
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        
        String legitName = pluginName;
        if (pluginName.contains("_"))
        {
            legitName = pluginName.substring(0, pluginName.indexOf('_'));
        }
        String realName = legitName;
        
        final int interval = (int) Math.floor(getContentHeaders().size() / 10f);
        for (int index = 0; index < getContentHeaders().size(); index++)
        {
            WADContentHeaderV1 fileHeader = getContentHeaders().get(index);
            
            if (contentHeaders.size() > 500)
            {
                if (index % interval == 0)
                {
                    System.out.println(index + "/" + getContentHeaders().size());
                }
            }
            
            if (getHeader().getMajor() > 1 && ((WADContentHeaderV2) fileHeader).isDuplicate())
            {
                //System.out.println("Duplicate file, skipping");
                continue;
            }
            
            try
            {
                saveFile(fileHeader, outputPath, realName);
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        
        fileReader.close();
    }
    
    private void saveFile(WADContentHeaderV1 header, Path savePath, String pluginName) throws IOException
    {
        String hash     = String.format("%016X", header.getPathHash()).toLowerCase(Locale.ENGLISH);
        String filename = UtilHandler.getKnownFileHashes(pluginName).getOrDefault(hash, "unknown\\" + hash);
        Path   self     = savePath.resolve(filename);
        
        self.getParent().toFile().mkdirs();
        String parentName = self.getParent().getFileName().toString();
        byte[] data       = readContentFromHeaderData(header);
        
        
        if (filename.endsWith("json"))
        {
            data = FileTypeHandler.makePrettyJson(data);
        }
        
        if ("unknown".equals(parentName))
        {
            Files.write(savePath.resolve(unknownHashContainer), (hash + "\n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
            findFileTypeAndRename(self, data, filename, savePath);
        } else
        {
            Files.write(self, data);
        }
    }
    
    private synchronized byte[] readContentFromHeaderData(WADContentHeaderV1 header) throws IOException
    {
        fileReader.seek(header.getOffset());
        if (header.isCompressed())
        {
            byte[] fileBytes = fileReader.readBytes(header.getCompressedFileSize());
            if (header.getCompressed() == 1)
            {
                return CompressionHandler.uncompressGZIP(fileBytes);
            }
            
            if (header.getCompressed() == 2)
            {
                System.out.println(new String(fileBytes, StandardCharsets.UTF_8));
                return fileBytes;
            }
            
            if (header.getCompressed() == 3)
            {
                return CompressionHandler.uncompressZSTD(fileBytes, header.getFileSize());
            }
            
            Files.write(Paths.get("unknown.file"), fileBytes);
            System.out.println("Found file with unknown compression!");
            return fileBytes;
        } else
        {
            return fileReader.readBytes(header.getFileSize());
        }
    }
    
    
    private void findFileTypeAndRename(Path self, byte[] data, String filename, Path parent) throws IOException
    {
        String        fileType = FileTypeHandler.findFileType(data, self);
        StringBuilder sb       = new StringBuilder(filename).append(".").append(fileType);
        Path          other    = parent.resolve(sb.toString());
        
        if (filename.endsWith("json"))
        {
            data = FileTypeHandler.makePrettyJson(data);
        }
        
        Files.write(other, data);
    }
}
