package no.stelar7.cdragon.util.types;

import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;

public final class ByteArray
{
    private byte[] data;
    
    /**
     * Number of valid bytes in data; data may be larger than this while appending.
     * Every method other than append calls compact() first, so data.length == length there.
     */
    private int length;
    
    public ByteArray()
    {
        this.data = new byte[0];
        this.length = 0;
    }
    
    public ByteArray(byte[] data)
    {
        this.data = data;
        this.length = data.length;
    }
    
    public ByteArray(byte[] data, int length)
    {
        this.data = new byte[length];
        this.length = length;
        System.arraycopy(data, 0, this.data, 0, data.length);
    }
    
    /**
     * Trims away the spare capacity left by append
     */
    private void compact()
    {
        if (this.data.length != this.length)
        {
            this.data = Arrays.copyOf(this.data, this.length);
        }
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
    
    public static ByteArray fromString(String data)
    {
        String[] split = data.replaceAll("..(?!$)", "$0 ").split(" ");
        
        ByteArray array = new ByteArray();
        array.data = new byte[split.length];
        array.length = split.length;
    
        for (int i = 0, splitLength = split.length; i < splitLength; i++)
        {
            String s      = split[i];
            byte   inByte = Integer.decode("0x" + s).byteValue();
            array.data[i] = inByte;
        }
        
        return array;
    }
    
    
    public boolean indexMatch(int index, byte b)
    {
        compact();
        return index >= 0 && index < data.length && data[index] == b;
    }
    
    public ByteArray removeLastByte(int i)
    {
        compact();
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
        compact();
        otherObj.compact();
        
        return Arrays.equals(data, otherObj.data);
    }
    
    public byte[] getDataCopy()
    {
        compact();
        return Arrays.copyOf(data, data.length);
    }
    
    public byte[] getDataRaw()
    {
        compact();
        return data;
    }
    
    @Override
    public int hashCode()
    {
        compact();
        return Arrays.hashCode(data);
    }
    
    
    @Override
    public String toString()
    {
        compact();
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
        return copyOfRange(start, this.length);
    }
    
    public boolean endsWith(ByteArray bytes)
    {
        compact();
        bytes.compact();
        if (this.data.length < bytes.data.length)
        {
            return false;
        }
        ByteArray last = copyOfRange(this.data.length - bytes.data.length, this.data.length);
        return Arrays.equals(last.data, bytes.data);
    }
    
    public int size()
    {
        return this.length;
    }
    
    public boolean startsWith(ByteArray bytes)
    {
        compact();
        bytes.compact();
        return this.data.length >= bytes.data.length && Arrays.equals(this.data, 0, bytes.data.length, bytes.data, 0, bytes.data.length);
    }
    
    public int indexOf(int value, int offset)
    {
        compact();
        for (int j = offset; j < this.data.length; j++)
        {
            if (this.data[j] == value)
            {
                return j;
            }
        }
        return -1;
    }
    
    /**
     * Appends input to the end of this array.
     * The backing buffer grows geometrically, so repeated appends are amortized O(n) instead of copying everything every time.
     */
    public void append(byte[] input)
    {
        int needed = Math.addExact(this.length, input.length);
        if (needed > this.data.length)
        {
            long grown       = Math.max((long) needed, (long) this.data.length * 2);
            int  newCapacity = (int) Math.min(grown, Integer.MAX_VALUE - 8);
            this.data = Arrays.copyOf(this.data, Math.max(newCapacity, needed));
        }
        System.arraycopy(input, 0, this.data, this.length, input.length);
        this.length = needed;
    }
}