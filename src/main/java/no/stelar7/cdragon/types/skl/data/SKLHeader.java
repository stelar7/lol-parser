package no.stelar7.cdragon.types.skl.data;

public final class SKLHeader
{
    private final String magic;
    private final int    version;
    
    public SKLHeader(String magic, int version)
    {
        this.magic = magic;
        this.version = version;
    }
    
    public String getMagic()
    {
        return magic;
    }
    
    public int getVersion()
    {
        return version;
    }
}
