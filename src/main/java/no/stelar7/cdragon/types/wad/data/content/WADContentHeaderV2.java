package no.stelar7.cdragon.types.wad.data.content;

public class WADContentHeaderV2 extends WADContentHeaderV1
{
    private boolean duplicate;
    private short   subChunkOffset;
    private long    sha256;
    
    public WADContentHeaderV2(WADContentHeaderV1 header)
    {
        this.pathHash = header.pathHash;
        this.offset = header.offset;
        this.compressedFileSize = header.compressedFileSize;
        this.fileSize = header.fileSize;
        this.compressionType = header.compressionType;
        this.subChunkCount = header.subChunkCount;
    }
    
    
    public boolean isDuplicate()
    {
        return duplicate;
    }
    
    public void setDuplicate(boolean duplicate)
    {
        this.duplicate = duplicate;
    }
    
    
    public long getSha256()
    {
        return sha256;
    }
    
    public void setSha256(long sha256)
    {
        this.sha256 = sha256;
    }
    
    public short getSubChunkOffset()
    {
        return subChunkOffset;
    }
    
    public void setSubChunkOffset(short subChunkOffset)
    {
        this.subChunkOffset = subChunkOffset;
    }
    
    @Override
    public String toString()
    {
        return "WADContentHeaderV2{" +
               "duplicate=" + duplicate +
               ", subChunkOffset=" + subChunkOffset +
               ", sha256=" + sha256 +
               ", pathHash='" + pathHash + '\'' +
               ", offset=" + offset +
               ", compressedFileSize=" + compressedFileSize +
               ", fileSize=" + fileSize +
               ", compressionType=" + compressionType +
               ", subChunkCount=" + subChunkCount +
               '}';
    }
}
