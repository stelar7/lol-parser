package no.stelar7.cdragon.types.bin.data;


import java.util.*;

public class BINStruct
{
    private String         hash;
    private int            size;
    private short          count;
    private List<BINValue> data = new ArrayList<>();
    
    public BINValue getIfPresent(String hash)
    {
        return data.stream().filter(v -> v.getHash().equalsIgnoreCase(hash)).findFirst().get();
    }
    
    public Optional<BINValue> get(String hash)
    {
        return data.stream().filter(v -> v.getHash().equalsIgnoreCase(hash)).findFirst();
    }
    
    public String getHash()
    {
        return hash;
    }
    
    public void setHash(String hash)
    {
        this.hash = hash;
    }
    
    public int getSize()
    {
        return size;
    }
    
    public void setSize(int size)
    {
        this.size = size;
    }
    
    public short getCount()
    {
        return count;
    }
    
    public void setCount(short count)
    {
        this.count = count;
    }
    
    public List<BINValue> getData()
    {
        return data;
    }
    
    public void setData(List<BINValue> data)
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
        BINStruct binStruct = (BINStruct) o;
        return size == binStruct.size &&
               count == binStruct.count &&
               Objects.equals(hash, binStruct.hash) &&
               Objects.equals(data, binStruct.data);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(hash, size, count, data);
    }
    
    @Override
    public String toString()
    {
        return "BINStruct{" +
               "hash='" + hash + '\'' +
               ", size=" + size +
               ", count=" + count +
               ", data=" + data +
               '}';
    }
}
