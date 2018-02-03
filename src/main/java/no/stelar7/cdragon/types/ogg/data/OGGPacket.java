package no.stelar7.cdragon.types.ogg.data;

import no.stelar7.cdragon.util.readers.RandomAccessReader;

public class OGGPacket
{
    private int     offset;
    private int     size;
    private int     absGranule;
    private boolean noGranule;
    
    public OGGPacket(RandomAccessReader stream, int offset, boolean isNoGranule)
    {
        this.offset = offset;
        absGranule = 0;
        noGranule = isNoGranule;
        
        stream.seek(offset);
        size = stream.readShort();
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
