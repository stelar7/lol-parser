package no.stelar7.cdragon.types.releasemanifest;

import no.stelar7.cdragon.interfaces.Parseable;
import no.stelar7.cdragon.types.releasemanifest.data.*;
import no.stelar7.cdragon.util.handlers.UtilHandler;
import no.stelar7.cdragon.util.readers.RandomAccessReader;

import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.*;

public class ReleasemanifestParser implements Parseable<ReleasemanifestDirectory>
{
    @Override
    public ReleasemanifestDirectory parse(Path path)
    {
        return parse(new RandomAccessReader(path, ByteOrder.LITTLE_ENDIAN));
    }
    
    @Override
    public ReleasemanifestDirectory parse(byte[] data)
    {
        return parse(new RandomAccessReader(data, ByteOrder.LITTLE_ENDIAN));
    }
    
    @Override
    public ReleasemanifestDirectory parse(RandomAccessReader raf)
    {
        ReleasemanifestDataFile file = new ReleasemanifestDataFile();
        
        file.setHeader(parseHeader(raf));
        file.setDirectories(parseDirectories(raf));
        file.setFiles(parseFiles(raf));
        file.setStrings(parseStrings(raf));
        
        return solveDirectory(file, file.getDirectories().get(0));
    }
    
    private ReleasemanifestDirectory solveDirectory(ReleasemanifestDataFile data, ReleasemanifestDataDirectory dir)
    {
        ReleasemanifestDirectory result = new ReleasemanifestDirectory();
        result.setName(data.getStrings().get(dir.getNameIndex()));
        
        for (int i = dir.getFileStartIndex(); i < dir.getFileStartIndex() + dir.getFileCount(); i++)
        {
            ReleasemanifestDataContentFile file = data.getFiles().get(i);
            
            ReleasemanifestContentFile contentFile = new ReleasemanifestContentFile();
            contentFile.setName(data.getStrings().get(file.getNameIndex()));
            contentFile.setSize(file.getSize());
            contentFile.setHash(file.getHash());
            
            result.getFiles().add(contentFile);
        }
        
        for (int i = dir.getSubdirectoryStartIndex(); i < dir.getSubdirectoryStartIndex() + dir.getSubdirectoryCount(); i++)
        {
            ReleasemanifestDataDirectory directory = data.getDirectories().get(i);
            result.getSubDirectories().add(solveDirectory(data, directory));
        }
        
        return result;
    }
    
    private ReleasemanifestHeader parseHeader(RandomAccessReader raf)
    {
        ReleasemanifestHeader header = new ReleasemanifestHeader();
        
        header.setMagic(raf.readString(4));
        header.setType(raf.readInt());
        header.setEntries(raf.readInt());
        header.setVersion(raf.readInt());
        
        return header;
    }
    
    
    private List<ReleasemanifestDataDirectory> parseDirectories(RandomAccessReader raf)
    {
        int count = raf.readInt();
        
        List<ReleasemanifestDataDirectory> dirs = new ArrayList<>();
        
        for (int i = 0; i < count; i++)
        {
            ReleasemanifestDataDirectory dir = new ReleasemanifestDataDirectory();
            
            dir.setNameIndex(raf.readInt());
            dir.setSubdirectoryStartIndex(raf.readInt());
            dir.setSubdirectoryCount(raf.readInt());
            dir.setFileStartIndex(raf.readInt());
            dir.setFileCount(raf.readInt());
            
            dirs.add(dir);
        }
        
        return dirs;
    }
    
    private List<ReleasemanifestDataContentFile> parseFiles(RandomAccessReader raf)
    {
        int count = raf.readInt();
        
        List<ReleasemanifestDataContentFile> files = new ArrayList<>();
        
        for (int i = 0; i < count; i++)
        {
            ReleasemanifestDataContentFile cFile = new ReleasemanifestDataContentFile();
            
            cFile.setNameIndex(raf.readInt());
            cFile.setVersion(raf.readInt());
            cFile.setHash(UtilHandler.toHex(raf.readBytes(16)));
            cFile.setFlags(raf.readInt());
            cFile.setSize(raf.readInt());
            cFile.setCompressedSize(raf.readInt());
            cFile.setUnknown(raf.readInt());
            cFile.setType(raf.readShort());
            cFile.setPadding(raf.readShort());
            
            files.add(cFile);
        }
        
        return files;
    }
    
    private List<String> parseStrings(RandomAccessReader raf)
    {
        int count = raf.readInt();
        // ignored stringSize property
        raf.readInt();
        
        List<String> files = new ArrayList<>();
        
        for (int i = 0; i < count; i++)
        {
            files.add(raf.readString());
        }
        
        return files;
    }
}
