package no.stelar7.cdragon.types.wad.data.content;

import no.stelar7.cdragon.types.wad.data.WADCompressionType;

public class WADContentHeaderV1
{
    protected long               pathHash;
    protected int                offset;
    protected int                compressedFileSize;
    protected int                fileSize;
    protected WADCompressionType compressionType;
    
    public boolean isCompressed()
    {
        return compressionType != WADCompressionType.NONE;
    }
    
    public long getPathHash()
    {
        return pathHash;
    }
    
    public void setPathHash(long pathHash)
    {
        this.pathHash = pathHash;
    }
    
    public int getOffset()
    {
        return offset;
    }
    
    public void setOffset(int offset)
    {
        this.offset = offset;
    }
    
    public int getCompressedFileSize()
    {
        return compressedFileSize;
    }
    
    public void setCompressedFileSize(int compressedFileSize)
    {
        this.compressedFileSize = compressedFileSize;
    }
    
    public int getFileSize()
    {
        return fileSize;
    }
    
    public void setFileSize(int fileSize)
    {
        this.fileSize = fileSize;
    }
    
    public WADCompressionType getCompressionType()
    {
        return compressionType;
    }
    
    public void setCompressionType(WADCompressionType compressionType)
    {
        this.compressionType = compressionType;
    }
    
    @Override
    public String toString()
    {
        return "WADContentHeaderV1{" +
               "pathHash=" + pathHash +
               ", offset=" + offset +
               ", compressedFileSize=" + compressedFileSize +
               ", fileSize=" + fileSize +
               ", compressionType=" + compressionType +
               '}';
    }
}
