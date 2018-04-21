package no.stelar7.cdragon.types.anm.data;

public class ANMHeader
{
    private String magic;
    private int    version;
    
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
}
