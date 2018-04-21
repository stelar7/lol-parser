package no.stelar7.cdragon.types.bin.data;


import java.util.*;

public class BINHeader
{
    private String magic;
    private int    version;
    private int    entryCount;
    private int    linkedFileCount;
    private List<Integer> entryTypes  = new ArrayList<>();
    private List<String>  linkedFiles = new ArrayList<>();
    
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
    
    public int getLinkedFileCount()
    {
        return linkedFileCount;
    }
    
    public void setLinkedFileCount(int linkedFileCount)
    {
        this.linkedFileCount = linkedFileCount;
    }
    
    public List<Integer> getEntryTypes()
    {
        return entryTypes;
    }
    
    public void setEntryTypes(List<Integer> entryTypes)
    {
        this.entryTypes = entryTypes;
    }
    
    public List<String> getLinkedFiles()
    {
        return linkedFiles;
    }
    
    public void setLinkedFiles(List<String> linkedFiles)
    {
        this.linkedFiles = linkedFiles;
    }
}
