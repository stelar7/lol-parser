package no.stelar7.cdragon.types.releasemanifest.data;

public final class ReleasemanifestHeader
{
    private final String magic;
    private final int    type;
    private final int    entries;
    private final int    version;
    private final int    directoriesCount;
    
    public ReleasemanifestHeader(String magic, int type, int entries, int version)
    {
        this.magic = magic;
        this.type = type;
        this.entries = entries;
        this.version = version;
        // never read from the file
        this.directoriesCount = 0;
    }
    
    public String getMagic()
    {
        return magic;
    }
    
    public int getType()
    {
        return type;
    }
    
    public int getEntries()
    {
        return entries;
    }
    
    public int getVersion()
    {
        return version;
    }
    
    public int getDirectoriesCount()
    {
        return directoriesCount;
    }
}
