package no.stelar7.cdragon.types.bin.data;


import java.util.*;

public final class BINContainer
{
    private final BINValueType type;
    private final int          size;
    private final int          count;
    private final List<Object> data;
    
    /**
     * @param data the items, kept as given (not copied)
     */
    public BINContainer(BINValueType type, int size, int count, List<Object> data)
    {
        this.type = type;
        this.size = size;
        this.count = count;
        this.data = data;
    }
    
    public BINValueType getType()
    {
        return type;
    }
    
    public int getSize()
    {
        return size;
    }
    
    public int getCount()
    {
        return count;
    }
    
    public List<Object> getData()
    {
        return data;
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
