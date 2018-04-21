package no.stelar7.cdragon.types.wad.data.header;

public class WADHeaderV3 extends WADHeaderBase
{
    private byte[] ECDSA;
    private long   checksum;
    
    
    public WADHeaderV3(WADHeaderBase base)
    {
        this.magic = base.magic;
        this.major = base.major;
        this.minor = base.minor;
        this.fileCount = base.fileCount;
    }
    
    public byte[] getECDSA()
    {
        return ECDSA;
    }
    
    public void setECDSA(byte[] ECDSA)
    {
        this.ECDSA = ECDSA;
    }
    
    public long getChecksum()
    {
        return checksum;
    }
    
    public void setChecksum(long checksum)
    {
        this.checksum = checksum;
    }
}