package no.stelar7.cdragon.types.bin.data;


import java.util.*;

public class BINHeader
{
    private String        magic;
    private int           version;
    private int           entryCount;
    private List<Integer> entryTypes = new ArrayList<>();
    
    public String getMagic()
    {
        return magic;
    }
    
    public void setMagic(String magic)
    {
        this.magic = magic;
    }
    
    public int getVersion()
    {
        return version;
    }
    
    public void setVersion(int version)
    {
        this.version = version;
    }
    
    public int getEntryCount()
    {
        return entryCount;
    }
    
    public void setEntryCount(int entryCount)
    {
        this.entryCount = entryCount;
    }
    
    public List<Integer> getEntryTypes()
    {
        return entryTypes;
    }
    
    public void setEntryTypes(List<Integer> entryTypes)
    {
        this.entryTypes = entryTypes;
    }
    
}
