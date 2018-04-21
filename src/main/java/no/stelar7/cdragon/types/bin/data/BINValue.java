package no.stelar7.cdragon.types.bin.data;

public class BINValue
{
    private String       hash;
    private BINValueType type;
    private Object       value;
    
    public String getHash()
    {
        return hash;
    }
    
    public void setHash(String hash)
    {
        this.hash = hash;
    }
    
    public BINValueType getType()
    {
        return type;
    }
    
    public void setType(BINValueType type)
    {
        this.type = type;
    }
    
    public Object getValue()
    {
        return value;
    }
    
    public void setValue(Object value)
    {
        this.value = value;
    }
}
