package no.stelar7.cdragon.types.rman.data;

import java.util.*;

public final class RMANFileBodyBundle
{
    private final int                           headerSize;
    private final String                        bundleId;
    private final byte[]                        skipped;
    private final List<RMANFileBodyBundleChunk> chunks;
    
    public RMANFileBodyBundle(String bundleId, List<RMANFileBodyBundleChunk> chunks)
    {
        // never read from the file
        this.headerSize = 0;
        this.bundleId = bundleId;
        // never read from the file
        this.skipped = null;
        this.chunks = chunks;
    }
    
    public int getHeaderSize()
    {
        return headerSize;
    }
    
    public String getBundleId()
    {
        return bundleId;
    }
    
    public byte[] getSkipped()
    {
        return skipped;
    }
    
    public List<RMANFileBodyBundleChunk> getChunks()
    {
        return chunks;
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
