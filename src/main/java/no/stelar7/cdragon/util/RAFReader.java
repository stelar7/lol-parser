package no.stelar7.cdragon.util;

import java.io.*;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class RAFReader implements AutoCloseable
{
    private final RandomAccessFile raf;
    private final ByteOrder        order;
    
    private long pointer;
    
    public RAFReader(RandomAccessFile raf, ByteOrder order)
    {
        this.raf = raf;
        this.order = order;
    }
    
    public void mark() throws IOException
    {
        this.pointer = raf.getFilePointer();
    }
    
    public void reset() throws IOException
    {
        raf.seek(pointer);
    }
    
    public void seek(long pos) throws IOException
    {
        raf.seek(pos);
    }
    
    @Override
    public void close() throws IOException
    {
        raf.close();
    }
    
    public String readString(int length) throws IOException
    {
        byte[] tempData = new byte[length];
        raf.read(tempData, 0, length);
        return new String(tempData, StandardCharsets.UTF_8).trim();
    }
    
    public long readULong() throws IOException
    {
        long data = raf.readLong();
        return order == ByteOrder.BIG_ENDIAN ? data : Long.reverseBytes(data);
    }
    
    public long readUInt() throws IOException
    {
        int data = raf.readInt();
        return order == ByteOrder.BIG_ENDIAN ? data : Integer.reverseBytes(data);
    }
    
    public int readUShort() throws IOException
    {
        short data = raf.readShort();
        return order == ByteOrder.BIG_ENDIAN ? data : Short.reverseBytes(data);
    }
    
    public int readUByte() throws IOException
    {
        return raf.readUnsignedByte();
    }
    
    public byte[] readBytes(int length) throws IOException
    {
        byte[] tempData = new byte[length];
        raf.readFully(tempData, 0, length);
        return Arrays.copyOf(tempData, length);
    }
    
    public byte[] readBytes(int length, int offset) throws IOException
    {
        byte[] tempData = new byte[length];
        raf.readFully(tempData, offset, length);
        return Arrays.copyOf(tempData, length);
    }
}
