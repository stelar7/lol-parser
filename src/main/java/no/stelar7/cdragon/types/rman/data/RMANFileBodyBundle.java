package no.stelar7.cdragon.types.rman.data;

import java.util.*;

public class RMANFileBodyBundle
{
    private int                           headerSize;
    private String                        bundleId;
    private byte[]                        skipped;
    private List<RMANFileBodyBundleChunk> chunks;
    
    public int getHeaderSize()
    {
        return headerSize;
    }
    
    public void setHeaderSize(int headerSize)
    {
        this.headerSize = headerSize;
    }
    
    public String getBundleId()
    {
        return bundleId;
    }
    
    public void setBundleId(String bundleId)
    {
        this.bundleId = bundleId;
    }
    
    public byte[] getSkipped()
    {
        return skipped;
    }
    
    public void setSkipped(byte[] skipped)
    {
        this.skipped = skipped;
    }
    
    public List<RMANFileBodyBundleChunk> getChunks()
    {
        return chunks;
    }
    
    public void setChunks(List<RMANFileBodyBundleChunk> chunks)
    {
        this.chunks = chunks;
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
        RMANFileBodyBundle that = (RMANFileBodyBundle) o;
        return headerSize == that.headerSize &&
               Objects.equals(bundleId, that.bundleId) &&
               Arrays.equals(skipped, that.skipped) &&
               Objects.equals(chunks, that.chunks);
    }
    
    @Override
    public String toString()
    {
        return bundleId;
    }
    
    /*
    @Override
    public int hashCode()
    {
        int result = Objects.hash(offset, offsetTableOffset, headerSize, bundleId, chunks);
        result = 31 * result + Arrays.hashCode(skipped);
        return result;
    }
    @Override
    public String toString()
    {
        return "RMANFileBodyBundle{" +
               "offset=" + offset +
               ", offsetTableOffset=" + offsetTableOffset +
               ", headerSize=" + headerSize +
               ", bundleId=" + bundleId +
               ", skipped=" + skipped +
               ", chunks=" + chunks +
               '}';
    }
    */
}
