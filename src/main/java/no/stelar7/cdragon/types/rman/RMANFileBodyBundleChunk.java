package no.stelar7.cdragon.types.rman;

import java.util.Objects;

public class RMANFileBodyBundleChunk
{
    private int  offsetTableOffset;
    private int  compressedSize;
    private int  uncompressedSize;
    private long chunkId;
    
    public int getOffsetTableOffset()
    {
        return offsetTableOffset;
    }
    
    public void setOffsetTableOffset(int offsetTableOffset)
    {
        this.offsetTableOffset = offsetTableOffset;
    }
    
    public int getCompressedSize()
    {
        return compressedSize;
    }
    
    public void setCompressedSize(int compressedSize)
    {
        this.compressedSize = compressedSize;
    }
    
    public int getUncompressedSize()
    {
        return uncompressedSize;
    }
    
    public void setUncompressedSize(int uncompressedSize)
    {
        this.uncompressedSize = uncompressedSize;
    }
    
    public long getChunkId()
    {
        return chunkId;
    }
    
    public void setChunkId(long chunkId)
    {
        this.chunkId = chunkId;
    }
    
    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        RMANFileBodyBundleChunk that = (RMANFileBodyBundleChunk) o;
        return offsetTableOffset == that.offsetTableOffset &&
               compressedSize == that.compressedSize &&
               uncompressedSize == that.uncompressedSize &&
               chunkId == that.chunkId;
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(offsetTableOffset, compressedSize, uncompressedSize, chunkId);
    }
    
    @Override
    public String toString()
    {
        return "RMANFileBodyBundleChunk{" +
               "offsetTableOffset=" + offsetTableOffset +
               ", compressedSize=" + compressedSize +
               ", uncompressedSize=" + uncompressedSize +
               ", chunkId=" + chunkId +
               '}';
    }
}
