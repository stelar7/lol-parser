package no.stelar7.cdragon.types.bbq;

import no.stelar7.cdragon.util.handlers.CompressionHandler;
import no.stelar7.cdragon.util.types.ByteArray;

import java.util.Objects;

public class BBQBlockInfo
{
    private int uncompressedSize;
    private int compressedSize;
    private int flags;
    
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
    
    public BBQCompressionType getCompressionType()
    {
        return BBQCompressionType.from(this.flags & 0x3f);
    }
    
    public byte[] decompress(byte[] buffer)
    {
        if (this.getCompressionType() == BBQCompressionType.NONE) {
            return buffer;
        }
        
        if(this.getCompressionType() == BBQCompressionType.LZMA)
        {
            return CompressionHandler.uncompressLZMA(buffer);
        }
        
        if(this.getCompressionType() == BBQCompressionType.LZ4 ||
           this.getCompressionType() == BBQCompressionType.LZ4HC ||
           this.getCompressionType() == BBQCompressionType.LZHAM) {
            ByteArray array = new ByteArray(buffer);
            ByteArray sized = array.copyOfRange(0, this.compressedSize);
           return CompressionHandler.uncompressLZ4(sized.getDataRaw(), this.uncompressedSize);
        }
        
        throw new UnsupportedOperationException("Unknown compression method in block info");
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
               flags == that.flags;
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(uncompressedSize, compressedSize, flags);
    }
    
    @Override
    public String toString()
    {
        return "BBQBlockInfo{" +
               "uncompressedSize=" + uncompressedSize +
               ", compressedSize=" + compressedSize +
               ", flags=" + flags +
               '}';
    }
}
