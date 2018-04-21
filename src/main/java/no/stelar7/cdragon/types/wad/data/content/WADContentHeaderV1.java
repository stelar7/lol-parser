package no.stelar7.cdragon.types.wad.data.content;

public class WADContentHeaderV1
{
    protected long pathHash;
    protected int  offset;
    protected int  compressedFileSize;
    protected int  fileSize;
    protected byte compressed;
    
    public boolean isCompressed()
    {
        return compressed > 0;
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
    
    public byte getCompressed()
    {
        return compressed;
    }
    
    public void setCompressed(byte compressed)
    {
        this.compressed = compressed;
    }
}
