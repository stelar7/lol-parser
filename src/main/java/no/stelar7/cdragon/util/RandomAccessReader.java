package no.stelar7.cdragon.util;


import sun.nio.ch.DirectBuffer;

import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;

public class RandomAccessReader implements AutoCloseable
{
    private MappedByteBuffer buffer;
    private Path             path;
    
    public RandomAccessReader(Path path, ByteOrder order)
    {
        try
        {
            this.path = path;
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
        
        if (buffer != null && ((DirectBuffer) buffer).cleaner() != null)
        {
            ((DirectBuffer) buffer).cleaner().clean();
        }
    }
    
    public int pos()
    {
        return buffer.position();
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
        byte[] tempData = new byte[length];
        buffer.get(tempData, 0, length);
        return Arrays.copyOf(tempData, length);
    }
    
    public Path getPath()
    {
        return path;
    }
    
    public float readFloat()
    {
        return buffer.getFloat();
    }
    
    public boolean readBoolean()
    {
        return buffer.get() > 0;
    }
    
    /**
     * Reads untill 0x00 is read.
     */
    public String readFromOffset(int offset)
    {
        int pos = buffer.position();
        buffer.position(0);
        byte[] tempData = new byte[buffer.remaining()];
        buffer.get(tempData, 0, buffer.remaining());
        buffer.position(pos);
        
        byte[] temp  = new byte[65536];
        byte   b;
        int    index = offset;
        while ((b = tempData[index++]) != 0)
        {
            temp[index - offset] = b;
        }
        
        return new String(temp, StandardCharsets.UTF_8).trim();
    }
    
    public void printBuffer()
    {
        int pos = buffer.position();
        while (buffer.hasRemaining())
        {
            System.out.print(buffer.get() + ", ");
        }
        System.out.println();
        buffer.position(pos);
    }
}
