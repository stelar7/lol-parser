package no.stelar7.cdragon.util.types;

import java.util.Arrays;

public final class ByteArray
{
    private final byte[] data;
    
    public ByteArray(byte[] data)
    {
        this.data = data;
    }
    
    public ByteArray(byte[] data, int length)
    {
        this.data = new byte[length];
        System.arraycopy(data, 0, this.data, 0, data.length);
    }
    
    public boolean indexMatch(int index, byte b)
    {
        return (data.length > index) && data[index] == b;
    }
    
    public ByteArray removeLastByte(int i)
    {
        byte[]  newData = new byte[data.length];
        boolean found   = false;
        for (int in = data.length - 1; in >= 0; in--)
        {
            if (!found && data[in] == i)
            {
                found = true;
                newData[in] = data[in - 1];
            }
            
            if (found)
            {
                if (in - 1 == -1)
                {
                    System.arraycopy(newData, 1, newData, 0, newData.length - 1);
                    return new ByteArray(newData);
                }
                
                newData[in] = data[in - 1];
            } else
            {
                newData[in] = data[in];
            }
        }
        return new ByteArray(newData);
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
    
    public ByteArray copyOfRange(int start, int end)
    {
        return new ByteArray(Arrays.copyOfRange(getData(), start, end));
    }
    
    public boolean endsWith(ByteArray bytes)
    {
        if (this.data.length < bytes.data.length)
        {
            return false;
        }
        ByteArray last = copyOfRange(this.data.length - bytes.data.length, this.data.length);
        return Arrays.equals(last.data, bytes.data);
    }
}