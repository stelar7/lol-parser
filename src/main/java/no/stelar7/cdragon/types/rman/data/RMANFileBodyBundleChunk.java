package no.stelar7.cdragon.types.rman.data;

import java.util.Objects;

public class RMANFileBodyBundleChunk
{
    private int    compressedSize;
    private int    uncompressedSize;
    private String chunkId;
    
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
    
    public String getChunkId()
    {
        return chunkId;
    }
    
    public void setChunkId(String chunkId)
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
        return compressedSize == that.compressedSize &&
               uncompressedSize == that.uncompressedSize &&
               Objects.equals(chunkId, that.chunkId);
    }
    
    @Override
    public String toString()
    {
        return chunkId;
    }
    
    /*
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
    }*/
}
