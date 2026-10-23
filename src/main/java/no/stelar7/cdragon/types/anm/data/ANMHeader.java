package no.stelar7.cdragon.types.anm.data;

public final class ANMHeader
{
    private final String magic;
    private final int    version;
    
    public ANMHeader(String magic, int version)
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
