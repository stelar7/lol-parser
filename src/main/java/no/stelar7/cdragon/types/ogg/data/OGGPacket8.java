package no.stelar7.cdragon.types.ogg.data;

import no.stelar7.cdragon.util.readers.RandomAccessReader;

public class OGGPacket8
{
    private int offset;
    private int size;
    private int absGranule;
    
    
    public OGGPacket8(RandomAccessReader stream, int offset)
    {
        this.offset = offset;
        stream.seek(offset);
        size = stream.readInt();
        absGranule = stream.readInt();
    }
    
    public int getHeaderSize()
    {
        return 8;
    }
    
    public int getOffset()
    {
        return getHeaderSize() + offset;
    }
    
    public int getSize()
    {
        return size;
    }
    
    public int getGranule()
    {
        return absGranule;
    }
    
    public int nextOffset()
    {
        return offset + getHeaderSize() + size;
    }
    
}
