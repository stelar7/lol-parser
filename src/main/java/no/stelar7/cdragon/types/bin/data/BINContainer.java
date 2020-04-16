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
    
    
    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        BINContainer that = (BINContainer) o;
        return size == that.size &&
               count == that.count &&
               type == that.type &&
               Objects.equals(data, that.data);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(type, size, count, data);
    }
    
    @Override
    public String toString()
    {
        return "BINContainer{" +
               "type=" + type +
               ", size=" + size +
               ", count=" + count +
               ", data=" + data +
               '}';
    }
}
