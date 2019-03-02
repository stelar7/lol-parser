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
    
    public void extractFiles(Path path, String wadName)
    {
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
            
            saveFile(fileHeader, path, wadName);
        }
        
        fileReader.close();
    }
    
    public void saveFile(WADContentHeaderV1 header, Path savePath, String wadName)
    {
        String unhashed = HashHandler.getWadHash(header.getPathHash());
        String filename = unhashed.equals(header.getPathHash()) ?  header.getPathHash() : unhashed;
        Path   self     = savePath.resolve(filename);
        
        if (self.toString().length() > 255)
        {
            self = self.resolveSibling("too_long_filename_" + header.getPathHash());
        }
        
        self.getParent().toFile().mkdirs();
        String parentName = self.getParent().getFileName().toString();
        byte[] data       = readContentFromHeaderData(header);
        
        if (filename.endsWith("json"))
        {
            data = FileTypeHandler.makePrettyJson(data);
        }
        
        if (filename.endsWith("js"))
        {
            data = UtilHandler.beautifyJS(new String(data, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8);
        }
        
        try
        {
            if (Files.exists(self))
            {
                if (Files.size(self) > data.length)
                {
                    return;
                }
            }
            
            if (header.getPathHash().equals(filename))
            {
                findFileTypeAndRename(data, filename, savePath, wadName);
            } else
            {
                Files.write(self, data);
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    
    public synchronized byte[] readContentFromHeaderData(WADContentHeaderV1 header)
    {
        fileReader.seek(header.getOffset());
        
        switch (header.getCompressionType())
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
    
    
    private void findFileTypeAndRename(byte[] data, String filename, Path parent, String wadName) throws IOException
    {
        ByteArray magic    = new ByteArray(Arrays.copyOf(data, data.length));
        String    fileType = FileTypeHandler.findFileType(magic);
        
        if ("unknown".equals(fileType))
        {
            System.err.format("Unknown filetype: %s\\%s %s%n", parent, filename, magic.copyOfRange(0, 4));
        }
        
        StringBuilder sb    = new StringBuilder(filename).append(".").append(fileType);
        Path          other = parent.resolve("unknown").resolve(wadName).resolve(sb.toString());
        
        if ("json".equals(fileType))
        {
            data = FileTypeHandler.makePrettyJson(data);
        }
        
        if ("js".equals(fileType))
        {
            data = UtilHandler.beautifyJS(new String(data, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8);
        }
        
        Files.createDirectories(other.getParent());
        Files.write(other, data);
    }
    
    public void printUnknownFiles(String wadfilename)
    {
        try
        {
            StandardOpenOption[] flags = {StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND};
            for (WADContentHeaderV1 header : this.getContentHeaders())
            {
                String filename = HashHandler.getWadHash(header.getPathHash());
                if (!filename.equals(header.getPathHash()))
                {
                    continue;
                }
                
                byte[] data     = readContentFromHeaderData(header);
                String filetype = FileTypeHandler.findFileType(new ByteArray(data));
                String output   = String.format("%s : %-5s : %s%n", filename, filetype, wadfilename);
                Files.write(UtilHandler.DOWNLOADS_FOLDER.resolve("unknownsSorted.txt"), output.getBytes(StandardCharsets.UTF_8), flags);
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
