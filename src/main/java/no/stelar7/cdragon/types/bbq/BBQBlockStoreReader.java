package no.stelar7.cdragon.types.bbq;

import no.stelar7.cdragon.util.readers.BinaryReader;

import java.nio.*;
import java.nio.charset.StandardCharsets;

public class BBQBlockStoreReader implements BinaryReader
{
    BBQBlockStore storage;
    ByteOrder     endian;
    
    public BBQBlockStoreReader(BBQBlockStore storage)
    {
        this.storage = storage;
    }
    
    @Override
    public String readString(int length)
    {
        return StandardCharsets.UTF_8.decode(ByteBuffer.wrap(storage.read(length)).order(endian)).toString();
    }
    
    @Override
    public String readString()
    {
        StringBuilder sb   = new StringBuilder();
        char          data = 0;
        while ((data = readChar()) != 0)
        {
            sb.append(data);
        }
        return sb.toString();
    }
    
    @Override
    public long readLong()
    {
        return ByteBuffer.wrap(storage.read(8)).order(endian).getLong();
    }
    
    @Override
    public int readInt()
    {
        return ByteBuffer.wrap(storage.read(4)).order(endian).getInt();
    }
    
    @Override
    public short readShort()
    {
        return ByteBuffer.wrap(storage.read(2)).order(endian).getShort();
    }
    
    @Override
    public byte[] readBytes(int i)
    {
        return storage.read(i);
    }
    
    @Override
    public byte readByte()
    {
        return ByteBuffer.wrap(storage.read(1)).order(endian).get();
    }
    
    @Override
    public char readChar()
    {
        return (char) readByte();
    }
    
    @Override
    public boolean readBoolean()
    {
        return readByte() > 0;
    }
    
    @Override
    public byte[] readRemaining()
    {
        return storage.read(storage.getMaxPos() - storage.pos());
    }
    
    @Override
    public int pos()
    {
        return storage.pos();
    }
    
    @Override
    public void seek(int pos)
    {
        storage.seek(pos, 0);
    }
    
    public void seek(int pos, int direction)
    {
        storage.seek(pos, direction);
    }
    
    @Override
    public void setEndian(ByteOrder order)
    {
        this.endian = order;
    }
    
    @Override
    public void align()
    {
        int current = pos();
        int newPos  = (current + 4) & -4;
        if (newPos > current)
        {
            this.seek(newPos - current, 1);
        }
    }
}
