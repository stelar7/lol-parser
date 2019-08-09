package no.stelar7.cdragon.types.bin.data;


import java.util.*;

public class BINEntry
{
    private int            length;
    private String         type;
    private String         hash;
    private short          valueCount;
    private List<BINValue> values = new ArrayList<>();
    
    public BINValue getIfPresent(String hash)
    {
        return values.stream().filter(v -> v.getHash().equalsIgnoreCase(hash)).findFirst().get();
    }
    
    public Optional<BINValue> get(String hash)
    {
        return values.stream().filter(v -> v.getHash().equalsIgnoreCase(hash)).findFirst();
    }
    
    public String getType()
    {
        return type;
    }
    
    public void setType(String type)
    {
        this.type = type;
    }
    
    public int getLength()
    {
        return length;
    }
    
    public void setLength(int length)
    {
        this.length = length;
    }
    
    public String getHash()
    {
        return hash;
    }
    
    public void setHash(String hash)
    {
        this.hash = hash;
    }
    
    public short getValueCount()
    {
        return valueCount;
    }
    
    public void setValueCount(short valueCount)
    {
        this.valueCount = valueCount;
    }
    
    public List<BINValue> getValues()
    {
        return values;
    }
    
    public void setValues(List<BINValue> values)
    {
        this.values = values;
    }
    
    @Override
    public String toString()
    {
        return "BINEntry{" +
               "length=" + length +
               ", type='" + type + '\'' +
               ", hash='" + hash + '\'' +
               ", valueCount=" + valueCount +
               ", values=" + values +
               '}';
    }
}
