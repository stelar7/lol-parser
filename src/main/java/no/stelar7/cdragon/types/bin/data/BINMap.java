package no.stelar7.cdragon.types.bin.data;

import no.stelar7.cdragon.util.types.math.Vector2;

import java.util.*;

public final class BINMap
{
    private final BINValueType                  type1;
    private final BINValueType                  type2;
    private final int                           size;
    private final int                           count;
    private final List<Vector2<Object, Object>> data;
    
    /**
     * @param data the key/value pairs, kept as given (not copied)
     */
    public BINMap(BINValueType type1, BINValueType type2, int size, int count, List<Vector2<Object, Object>> data)
    {
        this.type1 = type1;
        this.type2 = type2;
        this.size = size;
        this.count = count;
        this.data = data;
    }
    
    public Optional<Object> get(Object key)
    {
        return data.stream().filter(f -> f.getFirst().equals(key)).findFirst().map(Vector2::getSecond);
    }
    
    public Object getIfPresent(Object key)
    {
        return data.stream().filter(f -> f.getFirst().equals(key)).findFirst().map(Vector2::getSecond).get();
    }
    
    public BINValueType getType1()
    {
        return type1;
    }
    
    public BINValueType getType2()
    {
        return type2;
    }
    
    public int getSize()
    {
        return size;
    }
    
    public int getCount()
    {
        return count;
    }
    
    public List<Vector2<Object, Object>> getData()
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
        BINMap binMap = (BINMap) o;
        return size == binMap.size && count == binMap.count && type1 == binMap.type1 && type2 == binMap.type2 && Objects.equals(data, binMap.data);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(type1, type2, size, count, data);
    }
    
    @Override
    public String toString()
    {
        return "BINMap{" +
               "type1=" + type1 +
               ", type2=" + type2 +
               ", size=" + size +
               ", count=" + count +
               ", data=" + data +
               '}';
    }
}
