package no.stelar7.cdragon.types.scb.data;

public final class SCBHeader
{
    private final String magic;
    private final short  major;
    private final short  minor;
    private final String filename;
    
    public SCBHeader(String magic, short major, short minor, String filename)
    {
        this.magic = magic;
        this.major = major;
        this.minor = minor;
        this.filename = filename;
    }
    
    public String getMagic()
    {
        return magic;
    }
    
    public short getMajor()
    {
        return major;
    }
    
    public short getMinor()
    {
        return minor;
    }
    
    public String getFilename()
    {
        return filename;
    }
}
