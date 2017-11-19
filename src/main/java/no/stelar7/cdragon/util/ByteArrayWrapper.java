package no.stelar7.cdragon.util;

import java.util.Arrays;

public final class ByteArrayWrapper
{
    private final byte[] data;
    
    public ByteArrayWrapper(byte[] data)
    {
        this.data = data;
    }
    
    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof ByteArrayWrapper))
        {
            return false;
        }
        
        int len = Math.min(((ByteArrayWrapper) other).data.length, data.length);
        
        byte[] otherData = Arrays.copyOfRange(((ByteArrayWrapper) other).data, 0, len);
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