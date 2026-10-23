package no.stelar7.cdragon.types.bin.data;

import no.stelar7.cdragon.util.handlers.HashHandler;

import java.util.Objects;

public class BINPatchEntry
{
    private final String       hash;
    private final String       name;
    private final BINValueType type;
    private final Object       value;

    public BINPatchEntry(int hash, String name, Object value)
    {
        this(hash, name, null, value);
    }

    public BINPatchEntry(int hash, String name, BINValueType type, Object value)
    {
        this.hash = HashHandler.getBINHash(hash);
        this.name = name;
        this.type = type;
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

    /**
     * The type of the value, or null if it is unknown
     */
    public BINValueType getType()
    {
        return type;
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
        return Objects.equals(hash, that.hash) && Objects.equals(name, that.name) && type == that.type && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(hash, name, type, value);
    }

    @Override
    public String toString()
    {
        return "BINPatchEntry{" +
               "hash=" + hash +
               ", name='" + name + '\'' +
               ", type=" + type +
               ", value=" + value +
               '}';
    }
}
