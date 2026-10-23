package no.stelar7.cdragon.types.raf;

import no.stelar7.cdragon.types.raf.data.*;
import no.stelar7.cdragon.util.readers.*;

import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.*;

/**
 * Parser for the .raf index of RAF archives (version 1).
 * The file list and path list are read from the offsets in the header, path strings from the offsets in the path list.
 */
public class RAFParser
{
    public static final int MAGIC = 0x18BE0EF0;
    
    public RAFFile parse(Path path)
    {
        RandomAccessReader raf = new RandomAccessReader(path, ByteOrder.LITTLE_ENDIAN);
        
        try
        {
            RAFHeader header = parseHeader(raf);
        
            raf.seek(checkOffset(raf, header.getFilesOffset()));
            int                  fileCount = readCount(raf, 4 * Integer.BYTES);
            List<RAFContentFile> files     = parseFiles(raf, fileCount);
        
            long pathsOffset = checkOffset(raf, header.getPathsOffset());
            raf.seek(pathsOffset);
            int                  pathsSize  = raf.readInt();
            int                  pathsCount = readCount(raf, 2 * Integer.BYTES);
            List<RAFContentPath> paths      = parsePaths(raf, pathsCount);
        
            List<String> strings = parseStrings(raf, pathsOffset, paths);
            return new RAFFile(path, header, files, paths, strings, pathsSize, pathsCount, fileCount);
        } catch (IndexOutOfBoundsException e)
        {
            throw new ParseException("Unexpected end of RAF data", raf.pos(), e);
        }
    }
    
    private static long checkOffset(RandomAccessReader raf, int offset)
    {
        long value = Integer.toUnsignedLong(offset);
        if (value >= raf.size())
        {
            throw new ParseException("RAF offset " + value + " is outside the file", raf.pos());
        }
        return value;
    }
    
    private static int readCount(RandomAccessReader raf, int itemSize)
    {
        long count = raf.readU32();
        if (count * itemSize > raf.remaining())
        {
            throw new ParseException("Invalid RAF count " + count, raf.pos() - 4);
        }
        return (int) count;
    }
    
    /**
     * Path strings are stored at an offset relative to the start of the path list, with a length that includes the 0x00
     */
    private List<String> parseStrings(RandomAccessReader raf, long pathsOffset, List<RAFContentPath> paths)
    {
        List<String> strings = new ArrayList<>(paths.size());
        
        for (RAFContentPath path : paths)
        {
            long offset = pathsOffset + Integer.toUnsignedLong(path.getOffset());
            long length = Integer.toUnsignedLong(path.getLength());
            if (offset + length > raf.size())
            {
                throw new ParseException("RAF path string is outside the file", offset);
            }
            
            raf.seek(offset);
            strings.add(raf.readFixedString(length).trim());
        }
        
        return strings;
    }
    
    private List<RAFContentPath> parsePaths(RandomAccessReader raf, int count)
    {
        List<RAFContentPath> paths = new ArrayList<>(count);
        
        for (int i = 0; i < count; i++)
        {
            paths.add(new RAFContentPath(raf.readInt(), raf.readInt()));
        }
        
        return paths;
    }
    
    private List<RAFContentFile> parseFiles(RandomAccessReader raf, int count)
    {
        List<RAFContentFile> files = new ArrayList<>(count);
        
        for (int i = 0; i < count; i++)
        {
            files.add(new RAFContentFile(raf.readInt(), raf.readInt(), raf.readInt(), raf.readInt()));
        }
        
        return files;
    }
    
    private RAFHeader parseHeader(RandomAccessReader raf)
    {
        int magic = raf.readInt();
        if (magic != MAGIC)
        {
            throw new ParseException(String.format("Invalid RAF magic %08X", magic), 0);
        }
        
        int version = raf.readInt();
        if (version != 1)
        {
            throw new ParseException("Unsupported RAF version " + version, 4);
        }
        
        int managerIndex = raf.readInt();
        int filesOffset  = raf.readInt();
        int pathsOffset  = raf.readInt();
        
        return new RAFHeader(magic, version, managerIndex, filesOffset, pathsOffset);
        
    }
}
