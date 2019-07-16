package no.stelar7.cdragon.types.ktx.ktx2.data;

import java.util.Objects;

public class KTX2FileLevel
{
    private long byteOffset;
    private long byteLength;
    private long uncompressedByteLength;
    
    
    public long getByteOffset()
    {
        return byteOffset;
    }
    
    public void setByteOffset(long byteOffset)
    {
        this.byteOffset = byteOffset;
    }
    
    public long getByteLength()
    {
        return byteLength;
    }
    
    public void setByteLength(long byteLength)
    {
        this.byteLength = byteLength;
    }
    
    public long getUncompressedByteLength()
    {
        return uncompressedByteLength;
    }
    
    public void setUncompressedByteLength(long uncompressedByteLength)
    {
        this.uncompressedByteLength = uncompressedByteLength;
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
        KTX2FileLevel that = (KTX2FileLevel) o;
        return byteOffset == that.byteOffset &&
               byteLength == that.byteLength &&
               uncompressedByteLength == that.uncompressedByteLength;
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(byteOffset, byteLength, uncompressedByteLength);
    }
    
    @Override
    public String toString()
    {
        return "KTXFileLevel{" +
               "byteOffset=" + byteOffset +
               ", byteLength=" + byteLength +
               ", uncompressedByteLength=" + uncompressedByteLength +
               '}';
    }
}
