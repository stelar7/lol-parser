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
        return Arrays.equals(data, ((ByteArrayWrapper) other).data);
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