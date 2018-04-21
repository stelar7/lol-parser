package no.stelar7.cdragon.types.wad.data.header;


public class WADHeaderBase
{
    protected String magic;
    protected int    major;
    protected int    minor;
    protected long   fileCount;
    
    public String getMagic()
    {
        return magic;
    }
    
    public void setMagic(String magic)
    {
        this.magic = magic;
    }
    
    public int getMajor()
    {
        return major;
    }
    
    public void setMajor(int major)
    {
        this.major = major;
    }
    
    public int getMinor()
    {
        return minor;
    }
    
    public void setMinor(int minor)
    {
        this.minor = minor;
    }
    
    public long getFileCount()
    {
        return fileCount;
    }
    
    public void setFileCount(long fileCount)
    {
        this.fileCount = fileCount;
    }
}
