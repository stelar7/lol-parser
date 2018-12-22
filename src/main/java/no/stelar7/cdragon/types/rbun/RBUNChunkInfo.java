package no.stelar7.cdragon.types.rbun;

import java.util.Objects;

public class RBUNChunkInfo
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
        RBUNChunkInfo that = (RBUNChunkInfo) o;
        return compressedSize == that.compressedSize &&
               uncompressedSize == that.uncompressedSize &&
               Objects.equals(chunkId, that.chunkId);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(compressedSize, uncompressedSize, chunkId);
    }
    
    @Override
    public String toString()
    {
        return "RBUNChunkInfo{" +
               "compressedSize=" + compressedSize +
               ", uncompressedSize=" + uncompressedSize +
               ", chunkId='" + chunkId + '\'' +
               '}';
    }
}
