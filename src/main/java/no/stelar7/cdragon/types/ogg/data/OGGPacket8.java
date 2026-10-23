package no.stelar7.cdragon.types.ogg.data;

import no.stelar7.cdragon.util.readers.*;

public class OGGPacket8
{
    private final int offset;
    private final int size;
    private final int absGranule;
    
    
    public OGGPacket8(RandomAccessReader stream, int offset)
    {
        this.offset = offset;
        if (offset < 0 || offset + getHeaderSize() > stream.size())
        {
            throw new ParseException("Packet header out of range", offset);
        }
        
        stream.seek(offset);
        size = stream.readInt();
        absGranule = stream.readInt();
        
        if (size < 0)
        {
            throw new ParseException("Packet size out of range", offset);
        }
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
