package no.stelar7.cdragon.types.raf.data;

import no.stelar7.cdragon.interfaces.Extractable;
import no.stelar7.cdragon.util.handlers.*;
import no.stelar7.cdragon.util.readers.*;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.*;
import java.util.List;

public final class RAFFile implements Extractable
{
    // the .dat file is only opened when content is read
    private final transient Path               datPath;
    private transient       RandomAccessReader datReader;
    
    private final RAFHeader            header;
    private final List<RAFContentFile> files;
    private final List<RAFContentPath> paths;
    private final List<String>         strings;
    private final int                  pathsSize;
    private final int                  pathsCount;
    private final int                  fileCount;
    
    /**
     * @param pathToRaf the .raf file, the content is read from the .dat file next to it
     */
    public RAFFile(Path pathToRaf, RAFHeader header, List<RAFContentFile> files, List<RAFContentPath> paths, List<String> strings, int pathsSize, int pathsCount, int fileCount)
    {
        this.datPath = pathToRaf.resolveSibling(pathToRaf.getFileName() + ".dat");
        this.header = header;
        this.files = files;
        this.paths = paths;
        this.strings = strings;
        this.pathsSize = pathsSize;
        this.pathsCount = pathsCount;
        this.fileCount = fileCount;
    }
    
    public synchronized RandomAccessReader getDatReader()
    {
        if (datReader == null)
        {
            datReader = new RandomAccessReader(datPath, ByteOrder.BIG_ENDIAN);
        }
        return datReader;
    }
    
    public RAFHeader getHeader()
    {
        return header;
    }
    
    public List<RAFContentFile> getFiles()
    {
        return files;
    }
    
    public List<RAFContentPath> getPaths()
    {
        return paths;
    }
    
    public List<String> getStrings()
    {
        return strings;
    }
    
    public int getPathsSize()
    {
        return pathsSize;
    }
    
    public int getPathsCount()
    {
        return pathsCount;
    }
    
    public int getFileCount()
    {
        return fileCount;
    }
    
    public void extract(Path path)
    {
        try
        {
            for (int index = 0; index < fileCount; index++)
            {
                RAFContentFile file = files.get(index);
                String         name = strings.get(file.getPathIndex());
                byte[]         data = readContentFromData(file);
                
                Path outputPath = path.resolve(name);
                Files.createDirectories(outputPath.getParent());
                
                Files.write(outputPath, data);
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    public synchronized byte[] readContentFromData(RAFContentFile file)
    {
        boolean            deflated  = false;
        RandomAccessReader datReader = getDatReader();
        long               offset    = Integer.toUnsignedLong(file.getOffset());
        long               size      = Integer.toUnsignedLong(file.getSize());
        if (offset + size > datReader.size())
        {
            throw new ParseException("RAF content is outside the .dat file", offset);
        }
        
        datReader.seek(offset);
        byte[] fileHeader = datReader.readBytes(2);
        
        if (FileTypeHandler.isProbableDEFLATE(fileHeader))
        {
            deflated = true;
        }
        
        datReader.seek(offset);
        byte[] data = datReader.readBytes(size);
        
        if (deflated)
        {
            return CompressionHandler.uncompressDEFLATE(data);
        }
        
        return data;
    }
}
