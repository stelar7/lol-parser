package no.stelar7.cdragon.types.wad.data.content;

import no.stelar7.cdragon.types.wad.data.WADCompressionType;

/**
 * A WAD 2.x / 3.x table of contents entry. Immutable.
 */
public final class WADContentHeaderV2 extends WADContentHeaderV1
{
    private final boolean duplicate;
    private final int     subChunkOffset;
    private final long    sha256;

    public WADContentHeaderV2(String pathHash, long offset, long compressedFileSize, long fileSize, WADCompressionType compressionType, int subChunkCount, boolean duplicate, int subChunkOffset, long sha256)
    {
        super(pathHash, offset, compressedFileSize, fileSize, compressionType, subChunkCount);
        this.duplicate = duplicate;
        this.subChunkOffset = subChunkOffset;
        this.sha256 = sha256;
    }

    /**
     * Always false for 3.4, which reuses the byte for the subchunk index
     */
    public boolean isDuplicate()
    {
        return duplicate;
    }

    /**
     * The 64-bit entry checksum (named sha256 for compatibility)
     */
    public long getSha256()
    {
        return sha256;
    }

    public int getSubChunkOffset()
    {
        return subChunkOffset;
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
