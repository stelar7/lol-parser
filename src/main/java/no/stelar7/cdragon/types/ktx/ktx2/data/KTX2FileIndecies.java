package no.stelar7.cdragon.types.ktx.ktx2.data;

import java.util.Objects;

public class KTX2FileIndecies
{
    private int dfdByteOffset;
    private int dfdByteLength;
    private int kvdByteOffset;
    private int kvdByteLength;
    private int sgdByteOffset;
    private int sgdByteLength;
    
    public int getDfdByteOffset()
    {
        return dfdByteOffset;
    }
    
    public void setDfdByteOffset(int dfdByteOffset)
    {
        this.dfdByteOffset = dfdByteOffset;
    }
    
    public int getDfdByteLength()
    {
        return dfdByteLength;
    }
    
    public void setDfdByteLength(int dfdByteLength)
    {
        this.dfdByteLength = dfdByteLength;
    }
    
    public int getKvdByteOffset()
    {
        return kvdByteOffset;
    }
    
    public void setKvdByteOffset(int kvdByteOffset)
    {
        this.kvdByteOffset = kvdByteOffset;
    }
    
    public int getKvdByteLength()
    {
        return kvdByteLength;
    }
    
    public void setKvdByteLength(int kvdByteLength)
    {
        this.kvdByteLength = kvdByteLength;
    }
    
    public int getSgdByteOffset()
    {
        return sgdByteOffset;
    }
    
    public void setSgdByteOffset(int sgdByteOffset)
    {
        this.sgdByteOffset = sgdByteOffset;
    }
    
    public int getSgdByteLength()
    {
        return sgdByteLength;
    }
    
    public void setSgdByteLength(int sgdByteLength)
    {
        this.sgdByteLength = sgdByteLength;
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
        KTX2FileIndecies that = (KTX2FileIndecies) o;
        return dfdByteOffset == that.dfdByteOffset &&
               dfdByteLength == that.dfdByteLength &&
               kvdByteOffset == that.kvdByteOffset &&
               kvdByteLength == that.kvdByteLength &&
               sgdByteOffset == that.sgdByteOffset &&
               sgdByteLength == that.sgdByteLength;
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(dfdByteOffset, dfdByteLength, kvdByteOffset, kvdByteLength, sgdByteOffset, sgdByteLength);
    }
    
    @Override
    public String toString()
    {
        return "KTXFileIndecies{" +
               "dfdByteOffset=" + dfdByteOffset +
               ", dfdByteLength=" + dfdByteLength +
               ", kvdByteOffset=" + kvdByteOffset +
               ", kvdByteLength=" + kvdByteLength +
               ", sgdByteOffset=" + sgdByteOffset +
               ", sgdByteLength=" + sgdByteLength +
               '}';
    }
}
