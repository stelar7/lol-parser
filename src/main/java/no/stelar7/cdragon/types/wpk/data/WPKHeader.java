package no.stelar7.cdragon.types.wpk.data;

public final class WPKHeader
{
    private final String magic;
    private final int    version;
    private final int    fileCount;
    
    public WPKHeader(String magic, int version, int fileCount)
    {
        this.magic = magic;
        this.version = version;
        this.fileCount = fileCount;
    }
    
    public String getMagic()
    {
        return magic;
    }
    
    public int getVersion()
    {
        return version;
    }
    
    public int getFileCount()
    {
        return fileCount;
    }
}
