package no.stelar7.cdragon.types.wpk.data;

import no.stelar7.cdragon.interfaces.Extractable;
import no.stelar7.cdragon.types.wem.data.WEMFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public final class WPKFile implements Extractable
{
    private final WPKHeader     header;
    private final List<Integer> offsets;
    private final List<WEMFile> WEMFiles;
    
    public WPKFile(WPKHeader header, List<Integer> offsets, List<WEMFile> WEMFiles)
    {
        this.header = header;
        this.offsets = offsets;
        this.WEMFiles = WEMFiles;
    }
    
    public WPKHeader getHeader()
    {
        return header;
    }
    
    public List<Integer> getOffsets()
    {
        return offsets;
    }
    
    public List<WEMFile> getWEMFiles()
    {
        return WEMFiles;
    }
    
    public void extract(Path output)
    {
        for (WEMFile file : WEMFiles)
        {
            try
            {
                Files.write(output.resolve(file.getFilename()), file.getRawData());
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
}
