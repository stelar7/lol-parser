package no.stelar7.cdragon.types.wpk.data;

import no.stelar7.cdragon.interfaces.Extractable;
import no.stelar7.cdragon.types.wem.data.WEMFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class WPKFile implements Extractable
{
    private WPKHeader header;
    private List<Integer> offsets  = new ArrayList<>();
    private List<WEMFile> WEMFiles = new ArrayList<>();
    
    public WPKHeader getHeader()
    {
        return header;
    }
    
    public void setHeader(WPKHeader header)
    {
        this.header = header;
    }
    
    public List<Integer> getOffsets()
    {
        return offsets;
    }
    
    public void setOffsets(List<Integer> offsets)
    {
        this.offsets = offsets;
    }
    
    public List<WEMFile> getWEMFiles()
    {
        return WEMFiles;
    }
    
    public void setWEMFiles(List<WEMFile> WEMFiles)
    {
        this.WEMFiles = WEMFiles;
    }
    
    public void extract(Path output)
    {
        for (WEMFile file : WEMFiles)
        {
            try
            {
                Files.write(output.resolve(file.getFilename()), file.getData().getDataBytes());
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
    
}
