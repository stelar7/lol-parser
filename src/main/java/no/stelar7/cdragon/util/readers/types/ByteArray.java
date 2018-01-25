package no.stelar7.cdragon.util.readers.types;

import java.util.Arrays;

public final class ByteArray
{
    private final byte[] data;
    
    public ByteArray(byte[] data)
    {
        this.data = data;
    }
    
    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof ByteArray))
        {
            return false;
        }
        
        int len = Math.min(((ByteArray) other).data.length, data.length);
        
        byte[] otherData = Arrays.copyOfRange(((ByteArray) other).data, 0, len);
        byte[] selfData  = Arrays.copyOfRange(data, 0, len);
        
        return Arrays.equals(otherData, selfData);
    }
    
    public byte[] getData()
    {
        return Arrays.copyOf(data, data.length);
    }
    
    @Override
    public int hashCode()
    {
        return Arrays.hashCode(data);
    }
    
    
    @Override
    public String toString()
    {
        return Arrays.toString(data);
    }
}