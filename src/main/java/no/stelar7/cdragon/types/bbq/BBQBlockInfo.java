package no.stelar7.cdragon.types.bbq;

import no.stelar7.cdragon.util.handlers.CompressionHandler;
import no.stelar7.cdragon.util.readers.ParseException;

import java.util.Objects;

public class BBQBlockInfo
{
    private int  uncompressedSize;
    private int  compressedSize;
    private int  flags;
    /**
     * Offset of the compressed data relative to the start of the block data, only stored by bundle format 9+ (-1 if not stored)
     */
    private long offset = -1;

    public int getUncompressedSize()
    {
        return uncompressedSize;
    }

    public void setUncompressedSize(int uncompressedSize)
    {
        this.uncompressedSize = uncompressedSize;
    }

    public int getCompressedSize()
    {
        return compressedSize;
    }

    public void setCompressedSize(int compressedSize)
    {
        this.compressedSize = compressedSize;
    }

    public int getFlags()
    {
        return flags;
    }

    public void setFlags(int flags)
    {
        this.flags = flags;
    }

    public long getOffset()
    {
        return offset;
    }

    public void setOffset(long offset)
    {
        this.offset = offset;
    }

    public BBQCompressionType getCompressionType()
    {
        return BBQCompressionType.from(this.flags & 0x3f);
    }

    public byte[] decompress(byte[] buffer)
    {
        return decompress(getCompressionType(), buffer, this.uncompressedSize);
    }

    /**
     * Decompresses data stored with one of the bundle compression types
     */
    public static byte[] decompress(BBQCompressionType type, byte[] buffer, int uncompressedSize)
    {
        if (uncompressedSize == 0)
        {
            return new byte[0];
        }

        byte[] result = switch (type)
        {
            case NONE -> buffer;
            case LZMA -> CompressionHandler.uncompressLZMA(buffer, uncompressedSize);
            case LZ4, LZ4HC -> CompressionHandler.uncompressLZ4(buffer, uncompressedSize);
            // Unity only used LZHAM in a few 5.x versions and never documented its stream parameters (dictionary size, flags);
            // UnityPy and AssetRipper do not support it either, so a decoder could not be checked against real data
            case LZHAM -> throw new ParseException("LZHAM-compressed bundles are not supported (Unity's LZHAM parameters are undocumented, no reference implementation decodes them)", -1);
        };

        if (result.length != uncompressedSize)
        {
            throw new ParseException("Expected " + uncompressedSize + " bytes after decompression (" + type + "), got " + result.length, -1);
        }

        return result;
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
        BBQBlockInfo that = (BBQBlockInfo) o;
        return uncompressedSize == that.uncompressedSize &&
               compressedSize == that.compressedSize &&
               flags == that.flags &&
               offset == that.offset;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(uncompressedSize, compressedSize, flags, offset);
    }

    @Override
    public String toString()
    {
        return "BBQBlockInfo{" +
               "uncompressedSize=" + uncompressedSize +
               ", compressedSize=" + compressedSize +
               ", flags=" + flags +
               ", offset=" + offset +
               '}';
    }
}
