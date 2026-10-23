package no.stelar7.cdragon.types.rman.data;

import no.stelar7.cdragon.util.handlers.HashHandler;

import java.util.Objects;

public final class RMANFileBodyBundleChunk
{
    private final long compressedSize;
    private final long uncompressedSize;
    private final long chunkId;
    
    public RMANFileBodyBundleChunk(long compressedSize, long uncompressedSize, long chunkId)
    {
        this.compressedSize = compressedSize;
        this.uncompressedSize = uncompressedSize;
        this.chunkId = chunkId;
    }
    
    public long getCompressedSize()
    {
        return compressedSize;
    }
    
    public long getUncompressedSize()
    {
        return uncompressedSize;
    }
    
    public long getChunkId()
    {
        return chunkId;
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
               chunkId == that.chunkId;
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(compressedSize, uncompressedSize, chunkId);
    }
    
    @Override
    public String toString()
    {
        return HashHandler.toHex(chunkId, 16);
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
