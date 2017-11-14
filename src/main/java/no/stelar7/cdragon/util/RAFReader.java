package no.stelar7.cdragon.util;


import sun.nio.ch.DirectBuffer;

import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;

public class RAFReader implements AutoCloseable
{
    private MappedByteBuffer buffer;
    
    public RAFReader(Path path, ByteOrder order)
    {
        try
        {
            RandomAccessFile raf = new RandomAccessFile(path.toFile(), "r");
            
            this.buffer = raf.getChannel().map(MapMode.READ_ONLY, 0, raf.getChannel().size());
            this.buffer.order(order);
            raf.close();
        } catch (IOException e)
        {
            e.printStackTrace();
            throw new RuntimeException("Invalid file?");
        }
    }
    
    @Override
    public void close()
    {
        /*
         This is really hacky, but its a workaround to http://bugs.java.com/bugdatabase/view_bug.do?bug_id=4715154
          */
        ((DirectBuffer) buffer).cleaner().clean();
    }
    
    
    public void seek(int pos)
    {
        buffer.position(pos);
    }
    
    public String readString(int length)
    {
        return new String(readBytes(length), StandardCharsets.UTF_8).trim();
    }
    
    
    /**
     * Reads untill 0x00 is read.
     */
    public String readString()
    {
        byte[] temp  = new byte[65536];
        byte   b;
        int    index = 0;
        while ((b = readByte()) != 0)
        {
            temp[index++] = b;
        }
        
        return new String(temp, StandardCharsets.UTF_8).trim();
    }
    
    public long readLong()
    {
        return buffer.getLong();
    }
    
    public int readInt()
    {
        return buffer.getInt();
    }
    
    public short readShort()
    {
        return buffer.getShort();
    }
    
    public byte readByte()
    {
        return buffer.get();
    }
    
    public byte[] readBytes(int length)
    {
        return readBytes(length, 0);
    }
    
    public byte[] readBytes(int length, int offset)
    {
        byte[] tempData = new byte[length];
        buffer.get(tempData, offset, length);
        return Arrays.copyOf(tempData, length);
    }
    
}
