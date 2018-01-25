package no.stelar7.cdragon.types.raf;

import no.stelar7.cdragon.types.raf.data.*;
import no.stelar7.cdragon.util.readers.RandomAccessReader;

import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.*;

public class RAFParser
{
    
    public RAFFile parse(Path path)
    {
        RandomAccessReader raf     = new RandomAccessReader(path, ByteOrder.LITTLE_ENDIAN);
        RAFFile            rafFile = new RAFFile(path);
        
        rafFile.setHeader(parseHeader(raf));
        
        rafFile.setFileCount(raf.readInt());
        rafFile.setFiles(parseFiles(raf, rafFile.getFileCount()));
        
        rafFile.setPathsSize(raf.readInt());
        rafFile.setPathsCount(raf.readInt());
        rafFile.setPaths(parsePaths(raf, rafFile.getPathsCount()));
        
        rafFile.setStrings(parseStrings(raf, rafFile.getPathsCount()));
        
        return rafFile;
    }
    
    private List<String> parseStrings(RandomAccessReader raf, int count)
    {
        List<String> strings = new ArrayList<>();
        
        for (int i = 0; i < count; i++)
        {
            strings.add(raf.readString());
        }
        
        return strings;
    }
    
    private List<RAFContentPath> parsePaths(RandomAccessReader raf, int count)
    {
        List<RAFContentPath> paths = new ArrayList<>();
        
        for (int i = 0; i < count; i++)
        {
            paths.add(new RAFContentPath(raf.readInt(), raf.readInt()));
        }
        
        return paths;
    }
    
    private List<RAFContentFile> parseFiles(RandomAccessReader raf, int count)
    {
        List<RAFContentFile> files = new ArrayList<>();
        
        for (int i = 0; i < count; i++)
        {
            files.add(new RAFContentFile(raf.readInt(), raf.readInt(), raf.readInt(), raf.readInt()));
        }
        
        return files;
    }
    
    private RAFHeader parseHeader(RandomAccessReader raf)
    {
        RAFHeader header = new RAFHeader();
        
        header.setMagic(raf.readInt());
        header.setVersion(raf.readInt());
        header.setManagerIndex(raf.readInt());
        header.setFilesOffset(raf.readInt());
        header.setPathsOffset(raf.readInt());
        
        return header;
    }
}
