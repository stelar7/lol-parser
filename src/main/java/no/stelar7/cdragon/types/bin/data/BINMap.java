package no.stelar7.cdragon.types.bin.data;

import no.stelar7.cdragon.util.types.Vector2;

import java.util.*;

public class BINMap
{
    private BINValueType                  type1;
    private BINValueType                  type2;
    private int                           size;
    private int                           count;
    private List<Vector2<Object, Object>> data = new ArrayList<>();
    
    public BINValueType getType1()
    {
        return type1;
    }
    
    public void setType1(BINValueType type1)
    {
        this.type1 = type1;
    }
    
    public BINValueType getType2()
    {
        return type2;
    }
    
    public void setType2(BINValueType type2)
    {
        this.type2 = type2;
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
    
    public List<Vector2<Object, Object>> getData()
    {
        return data;
    }
    
    public void setData(List<Vector2<Object, Object>> data)
    {
        this.data = data;
    }
}
