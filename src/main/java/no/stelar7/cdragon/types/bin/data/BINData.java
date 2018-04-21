package no.stelar7.cdragon.types.bin.data;


import java.util.*;

public class BINData
{
    private BINValueType type;
    private byte         count;
    private List<Object> data = new ArrayList<>();
    
    public BINValueType getType()
    {
        return type;
    }
    
    public void setType(BINValueType type)
    {
        this.type = type;
    }
    
    public byte getCount()
    {
        return count;
    }
    
    public void setCount(byte count)
    {
        this.count = count;
    }
    
    public List<Object> getData()
    {
        return data;
    }
    
    public void setData(List<Object> data)
    {
        this.data = data;
    }
}
