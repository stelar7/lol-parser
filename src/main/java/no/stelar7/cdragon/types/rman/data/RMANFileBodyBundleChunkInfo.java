package no.stelar7.cdragon.types.rman.data;

import no.stelar7.cdragon.util.handlers.HashHandler;

import java.util.Objects;

public final class RMANFileBodyBundleChunkInfo
{
    private final String bundleId;
    private final long   chunkId;
    private final long   offsetToChunk;
    private final long   compressedSize;
    private final long   uncompressedSize;

    public RMANFileBodyBundleChunkInfo(String bundleId, long chunkId, long offsetToChunk, long compressedSize, long uncompressedSize)
    {
        this.bundleId = bundleId;
        this.chunkId = chunkId;
        this.offsetToChunk = offsetToChunk;
        this.compressedSize = compressedSize;
        this.uncompressedSize = uncompressedSize;
    }

    public String getBundleId()
    {
        return bundleId;
    }

    public long getChunkId()
    {
        return chunkId;
    }

    public long getOffsetToChunk()
    {
        return offsetToChunk;
    }

    public long getCompressedSize()
    {
        return compressedSize;
    }

    public long getUncompressedSize()
    {
        return uncompressedSize;
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
        RMANFileBodyBundleChunkInfo that = (RMANFileBodyBundleChunkInfo) o;
        return offsetToChunk == that.offsetToChunk &&
               compressedSize == that.compressedSize &&
               uncompressedSize == that.uncompressedSize &&
               chunkId == that.chunkId &&
               Objects.equals(bundleId, that.bundleId);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(bundleId, chunkId, offsetToChunk, compressedSize, uncompressedSize);
    }

    @Override
    public String toString()
    {
        return "RMANFileBodyBundleChunkInfo{" +
               "bundleId='" + bundleId + '\'' +
               ", chunkId='" + HashHandler.toHex(chunkId, 16) + '\'' +
               ", offsetToChunk=" + offsetToChunk +
               ", compressedSize=" + compressedSize +
               ", uncompressedSize=" + uncompressedSize +
               '}';
    }
}
