package no.stelar7.cdragon.types.wpk.data;

public class WPKHeader
{
    private String magic;
    private int    version;
    private int    fileCount;
    
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
    
    public int getFileCount()
    {
        return fileCount;
    }
    
    public void setFileCount(int fileCount)
    {
        this.fileCount = fileCount;
    }
}
