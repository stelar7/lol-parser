package no.stelar7.cdragon.types.ogg.data;

import no.stelar7.cdragon.util.readers.RandomAccessReader;

public class BitField
{
    private int size;
    private int value;
    
    public BitField(int size)
    {
        this.size = size;
    }
    
    public BitField(int size, int value)
    {
        this.size = size;
        this.value = value;
    }
    
    public int getValue()
    {
        return value;
    }
    
    
    public void read(RandomAccessReader reader)
    {
        value = reader.readBits(size);
    }
    
    public void write(OGGStream stream)
    {
        write(stream, size);
    }
    
    public void write(OGGStream stream, int bits)
    {
        stream.bitWrite(value, bits);
    }
}
