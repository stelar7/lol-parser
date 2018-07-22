package no.stelar7.cdragon.types.wad.data;

import no.stelar7.cdragon.types.wad.data.content.*;
import no.stelar7.cdragon.types.wad.data.header.WADHeaderBase;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.readers.RandomAccessReader;
import no.stelar7.cdragon.util.types.ByteArray;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class WADFile
{
    private String unknownHashContainer = "unknown.json";
    
    private final RandomAccessReader fileReader;
    private       WADHeaderBase      header;
    
    private List<WADContentHeaderV1> contentHeaders = new ArrayList<>();
    
    public WADFile(RandomAccessReader raf)
    {
        this.fileReader = raf;
    }
    
    public RandomAccessReader getFileReader()
    {
        return fileReader;
    }
    
    public WADHeaderBase getHeader()
    {
        return header;
    }
    
    public void setHeader(WADHeaderBase header)
    {
        this.header = header;
    }
    
    public List<WADContentHeaderV1> getContentHeaders()
    {
        return contentHeaders;
    }
    
    public void setContentHeaders(List<WADContentHeaderV1> contentHeaders)
    {
        this.contentHeaders = contentHeaders;
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
            
            if (contentHeaders.size() > 1500)
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
        String filename = HashHandler.getWadHashes(pluginName).getOrDefault(hash, "unknown\\" + hash);
        Path   self     = savePath.resolve(filename);
        
        self.getParent().toFile().mkdirs();
        String parentName = self.getParent().getFileName().toString();
        byte[] data       = readContentFromHeaderData(header);
        
        if (filename.endsWith("json"))
        {
            data = FileTypeHandler.makePrettyJson(data);
        }
        
        if (Files.exists(self))
        {
            if (Files.size(self) > data.length)
            {
                return;
            }
        }
        
        if ("unknown".equals(parentName))
        {
            Files.write(savePath.resolve(unknownHashContainer), (hash + "\n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
            findFileTypeAndRename(data, filename, savePath);
        } else
        {
            Files.write(self, data);
        }
    }
    
    
    public synchronized byte[] readContentFromHeaderData(WADContentHeaderV1 header)
    {
        fileReader.seek(header.getOffset());
        WADCompressionType type = WADCompressionType.valueOf(header.getCompressed());
        
        switch (type)
        {
            case NONE:
                return fileReader.readBytes(header.getFileSize());
            case GZIP:
                return CompressionHandler.uncompressGZIP(fileReader.readBytes(header.getCompressedFileSize()));
            case REFERENCE:
                return fileReader.readString(fileReader.readInt()).getBytes(StandardCharsets.UTF_8);
            case ZSTD:
                return CompressionHandler.uncompressZSTD(fileReader.readBytes(header.getCompressedFileSize()), header.getFileSize());
            default:
                return null;
        }
    }
    
    
    private void findFileTypeAndRename(byte[] data, String filename, Path parent) throws IOException
    {
        ByteArray magic    = new ByteArray(Arrays.copyOf(data, data.length));
        String    fileType = FileTypeHandler.findFileType(magic);
        
        if ("unknown".equals(fileType))
        {
            System.err.format("Unknown filetype: %s\\%s %s%n", parent, filename, magic.copyOfRange(0, 4));
        }
        
        StringBuilder sb    = new StringBuilder(filename).append(".").append(fileType);
        Path          other = parent.resolve(sb.toString());
        
        if ("json".equals(fileType))
        {
            data = FileTypeHandler.makePrettyJson(data);
        }
        
        Files.write(other, data);
    }
}
