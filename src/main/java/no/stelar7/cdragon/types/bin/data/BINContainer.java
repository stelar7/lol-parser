package no.stelar7.cdragon.types.bin.data;


import java.util.*;

public class BINContainer
{
    private BINValueType type;
    private int          size;
    private int          count;
    private List<Object> data = new ArrayList<>();
    
    public BINValueType getType()
    {
        return type;
    }
    
    public void setType(BINValueType type)
    {
        this.type = type;
    }
    
    public int getSize()
    {
        return size;
    }
    
    public void setSize(int size)
    {
        this.size = size;
    }
    
    public int getCount()
    {
        return count;
    }
    
    public void setCount(int count)
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
