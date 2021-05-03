package no.stelar7.cdragon.types.bin.data;

import no.stelar7.cdragon.util.handlers.HashHandler;

import java.util.Objects;

public class BINPatchEntry
{
    private final String hash;
    private final String name;
    private final Object value;
    
    public BINPatchEntry(int hash, String name, Object value)
    {
        this.hash = HashHandler.getBINHash(hash);
        this.name = name;
        this.value = value;
    }
    
    public String getHash()
    {
        return hash;
    }
    
    public String getName()
    {
        return name;
    }
    
    public Object getValue()
    {
        return value;
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
        BINPatchEntry that = (BINPatchEntry) o;
        return hash == that.hash && Objects.equals(name, that.name) && Objects.equals(value, that.value);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(hash, name, value);
    }
    
    @Override
    public String toString()
    {
        return "BINPatchEntry{" +
               "hash=" + hash +
               ", name='" + name + '\'' +
               ", value=" + value +
               '}';
    }
}
