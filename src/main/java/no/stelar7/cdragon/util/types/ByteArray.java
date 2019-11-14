package no.stelar7.cdragon.util.types;

import java.io.*;
import java.nio.file.*;
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
    
    public static ByteArray fromFile(Path p)
    {
        try
        {
            return new ByteArray(Files.readAllBytes(p));
        } catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
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
        
        ByteArray otherObj = (ByteArray) other;
        
        int    len       = Math.min(otherObj.data.length, data.length);
        byte[] otherData = Arrays.copyOfRange(otherObj.data, 0, len);
        byte[] selfData  = Arrays.copyOfRange(data, 0, len);
        
        return Arrays.equals(otherData, selfData);
    }
    
    public byte[] getDataCopy()
    {
        return Arrays.copyOf(data, data.length);
    }
    
    public byte[] getDataRaw()
    {
        return data;
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
        return new ByteArray(Arrays.copyOfRange(getDataRaw(), start, end));
    }
    
    /**
     * Returns the remaining bytes from start to data.length
     */
    public ByteArray copyOfRange(int start)
    {
        return copyOfRange(start, this.data.length);
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
    
    public int size()
    {
        return this.getDataRaw().length;
    }
    
    public boolean startsWith(ByteArray bytes)
    {
        return this.equals(bytes);
    }
    
    public int indexOf(int value, int offset)
    {
        for (int j = offset; j < this.data.length; j++)
        {
            if (this.data[j] == value)
            {
                return j;
            }
        }
        return -1;
    }
}