package no.stelar7.cdragon.types.bin.data;


import java.util.*;

public final class BINStruct
{
    private final String         hash;
    private final int            size;
    private final short          count;
    private final List<BINValue> data;
    
    /**
     * @param data the fields, kept as given (not copied)
     */
    public BINStruct(String hash, int size, short count, List<BINValue> data)
    {
        this.hash = hash;
        this.size = size;
        this.count = count;
        this.data = data;
    }
    
    public BINValue getIfPresent(String hash)
    {
        return data.stream().filter(v -> v.getHash().equalsIgnoreCase(hash)).findFirst().get();
    }
    
    public Optional<BINValue> get(String hash)
    {
        return data.stream().filter(v -> v.getHash().equalsIgnoreCase(hash)).findFirst();
    }
    
    public boolean has(String hash)
    {
        return data.stream().anyMatch(v -> v.getHash().equalsIgnoreCase(hash));
    }
    
    public <T> T getOrDefault(String hash, T other)
    {
        Optional<BINValue> maybe = get(hash);
    
        if (maybe.isEmpty())
        {
            return other;
        }
    
        return (T) maybe.get().getValue();
    }
    
    public String getHash()
    {
        return hash;
    }
    
    public int getSize()
    {
        return size;
    }
    
    public short getCount()
    {
        return count;
    }
    
    public List<BINValue> getData()
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
