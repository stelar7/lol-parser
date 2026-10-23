package no.stelar7.cdragon.types.releasemanifest.data;

public final class ReleasemanifestContentFile
{
    private final String name;
    private final int    size;
    private final String hash;
    
    public ReleasemanifestContentFile(String name, int size, String hash)
    {
        this.name = name;
        this.size = size;
        this.hash = hash;
    }
    
    public String getName()
    {
        return name;
    }
    
    public int getSize()
    {
        return size;
    }
    
    public String getHash()
    {
        return hash;
    }
}
