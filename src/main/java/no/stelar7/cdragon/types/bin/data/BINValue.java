package no.stelar7.cdragon.types.bin.data;

public final class BINValue
{
    private final String       hash;
    private final BINValueType type;
    private final Object       value;
    
    public BINValue(String hash, BINValueType type, Object value)
    {
        this.hash = hash;
        this.type = type;
        this.value = value;
    }
    
    public String getHash()
    {
        return hash;
    }
    
    public BINValueType getType()
    {
        return type;
    }
    
    public Object getValue()
    {
        return value;
    }
    
    @Override
    public String toString()
    {
        return "BINValue{" +
               "hash='" + hash + '\'' +
               ", type=" + type +
               ", value=" + value +
               '}';
    }
}
