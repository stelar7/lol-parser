package no.stelar7.cdragon.types.bin.data;


import java.util.*;

public final class BINData
{
    private final BINValueType type;
    private final byte         count;
    private final List<Object> data;
    
    /**
     * @param data the value (0 or 1 items), kept as given
     */
    public BINData(BINValueType type, byte count, List<Object> data)
    {
        this.type = type;
        this.count = count;
        this.data = data;
    }
    
    public BINValueType getType()
    {
        return type;
    }
    
    public byte getCount()
    {
        return count;
    }
    
    public List<Object> getData()
    {
        return data;
    }
}
