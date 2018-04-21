package no.stelar7.cdragon.types.wad.data.header;

public class WADHeaderV2 extends WADHeaderBase
{
    private int    ECDSALength;
    private byte[] ECDSA;
    private byte[] ECDSAPadding;
    private long   fileChecksum;
    private int    entryHeaderOffset;
    private int    entryHeaderCellSize;
    
    public WADHeaderV2(WADHeaderBase base)
    {
        this.magic = base.magic;
        this.major = base.major;
        this.minor = base.minor;
        this.fileCount = base.fileCount;
    }
    
    public int getECDSALength()
    {
        return ECDSALength;
    }
    
    public void setECDSALength(int ECDSALength)
    {
        this.ECDSALength = ECDSALength;
    }
    
    public byte[] getECDSA()
    {
        return ECDSA;
    }
    
    public void setECDSA(byte[] ECDSA)
    {
        this.ECDSA = ECDSA;
    }
    
    public byte[] getECDSAPadding()
    {
        return ECDSAPadding;
    }
    
    public void setECDSAPadding(byte[] ECDSAPadding)
    {
        this.ECDSAPadding = ECDSAPadding;
    }
    
    public long getFileChecksum()
    {
        return fileChecksum;
    }
    
    public void setFileChecksum(long fileChecksum)
    {
        this.fileChecksum = fileChecksum;
    }
    
    public int getEntryHeaderOffset()
    {
        return entryHeaderOffset;
    }
    
    public void setEntryHeaderOffset(int entryHeaderOffset)
    {
        this.entryHeaderOffset = entryHeaderOffset;
    }
    
    public int getEntryHeaderCellSize()
    {
        return entryHeaderCellSize;
    }
    
    public void setEntryHeaderCellSize(int entryHeaderCellSize)
    {
        this.entryHeaderCellSize = entryHeaderCellSize;
    }
}