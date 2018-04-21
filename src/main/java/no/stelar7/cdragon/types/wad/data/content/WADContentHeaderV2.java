package no.stelar7.cdragon.types.wad.data.content;

public class WADContentHeaderV2 extends WADContentHeaderV1
{
    private boolean duplicate;
    private short   padding;
    private long    sha256;
    
    public WADContentHeaderV2(WADContentHeaderV1 header)
    {
        this.pathHash = header.pathHash;
        this.offset = header.offset;
        this.compressedFileSize = header.compressedFileSize;
        this.fileSize = header.fileSize;
        this.compressed = header.compressed;
    }
    
    public boolean isDuplicate()
    {
        return duplicate;
    }
    
    public void setDuplicate(boolean duplicate)
    {
        this.duplicate = duplicate;
    }
    
    public short getPadding()
    {
        return padding;
    }
    
    public void setPadding(short padding)
    {
        this.padding = padding;
    }
    
    public long getSha256()
    {
        return sha256;
    }
    
    public void setSha256(long sha256)
    {
        this.sha256 = sha256;
    }
}
