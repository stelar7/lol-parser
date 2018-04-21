package no.stelar7.cdragon.types.releasemanifest.data;

public class ReleasemanifestHeader
{
    private String magic;
    private int    type;
    private int    entries;
    private int    version;
    private int    directoriesCount;
    
    
    public String getMagic()
    {
        return magic;
    }
    
    public void setMagic(String magic)
    {
        this.magic = magic;
    }
    
    public int getType()
    {
        return type;
    }
    
    public void setType(int type)
    {
        this.type = type;
    }
    
    public int getEntries()
    {
        return entries;
    }
    
    public void setEntries(int entries)
    {
        this.entries = entries;
    }
    
    public int getVersion()
    {
        return version;
    }
    
    public void setVersion(int version)
    {
        this.version = version;
    }
    
    public int getDirectoriesCount()
    {
        return directoriesCount;
    }
    
    public void setDirectoriesCount(int directoriesCount)
    {
        this.directoriesCount = directoriesCount;
    }
}
