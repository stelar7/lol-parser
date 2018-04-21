package no.stelar7.cdragon.types.bin.data;


import java.util.*;

public class BINEntry
{
    private int            lenght;
    private String         hash;
    private short          valueCount;
    private List<BINValue> values = new ArrayList<>();
    
    public int getLenght()
    {
        return lenght;
    }
    
    public void setLenght(int lenght)
    {
        this.lenght = lenght;
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
}
