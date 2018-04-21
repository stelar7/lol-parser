package no.stelar7.cdragon.types.scb.data;

public class SCBHeader
{
    private String magic;
    private short  major;
    private short  minor;
    private String filename;
    
    public String getMagic()
    {
        return magic;
    }
    
    public void setMagic(String magic)
    {
        this.magic = magic;
    }
    
    public short getMajor()
    {
        return major;
    }
    
    public void setMajor(short major)
    {
        this.major = major;
    }
    
    public short getMinor()
    {
        return minor;
    }
    
    public void setMinor(short minor)
    {
        this.minor = minor;
    }
    
    public String getFilename()
    {
        return filename;
    }
    
    public void setFilename(String filename)
    {
        this.filename = filename;
    }
}
