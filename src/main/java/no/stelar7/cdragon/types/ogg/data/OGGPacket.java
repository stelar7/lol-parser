package no.stelar7.cdragon.types.ogg.data;

import no.stelar7.cdragon.util.readers.*;

public class OGGPacket
{
    private final int     offset;
    private final int     size;
    private       int     absGranule;
    private final boolean noGranule;
    
    public OGGPacket(RandomAccessReader stream, int offset, boolean isNoGranule)
    {
        this.offset = offset;
        absGranule = 0;
        noGranule = isNoGranule;
        
        if (offset < 0 || offset + getHeaderSize() > stream.size())
        {
            throw new ParseException("Packet header out of range", offset);
        }
        
        stream.seek(offset);
        size = stream.readU16();
        if (!noGranule)
        {
            absGranule = stream.readInt();
        }
    }
    
    public int getHeaderSize()
    {
        return noGranule ? 2 : 6;
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
