package no.stelar7.cdragon.types.wad.data.content;

import no.stelar7.cdragon.types.wad.data.WADCompressionType;

/**
 * An entry of the WAD table of contents. Immutable.
 * <p>
 * WAD 1.x entries are this class, 2.x and 3.x entries are {@link WADContentHeaderV2}, which adds the subchunk and checksum fields.
 * (The base type is used for every entry, so code that only needs the common fields works for every version.)
 */
public sealed class WADContentHeaderV1 permits WADContentHeaderV2
{
    protected final String             pathHash;
    protected final long               offset;
    protected final long               compressedFileSize;
    protected final long               fileSize;
    protected final WADCompressionType compressionType;
    protected final int                subChunkCount;

    public WADContentHeaderV1(String pathHash, long offset, long compressedFileSize, long fileSize, WADCompressionType compressionType, int subChunkCount)
    {
        this.pathHash = pathHash;
        this.offset = offset;
        this.compressedFileSize = compressedFileSize;
        this.fileSize = fileSize;
        this.compressionType = compressionType;
        this.subChunkCount = subChunkCount;
    }

    public boolean isCompressed()
    {
        return compressionType != WADCompressionType.NONE;
    }

    public String getPathHash()
    {
        return pathHash;
    }

    public long getOffset()
    {
        return offset;
    }

    public long getCompressedFileSize()
    {
        return compressedFileSize;
    }

    public long getFileSize()
    {
        return fileSize;
    }

    public WADCompressionType getCompressionType()
    {
        return compressionType;
    }

    public int getSubChunkCount()
    {
        return subChunkCount;
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
