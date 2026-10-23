package no.stelar7.cdragon.types.bin.data;


import java.util.*;

public final class BINEntry
{
    private final int            length;
    private final String         type;
    private final String         hash;
    private final short          valueCount;
    private final List<BINValue> values;
    
    /**
     * @param values the values, kept as given (it is not copied, so edits through {@link #getValues()} are written by {@link BINFile#toBytes()})
     */
    public BINEntry(int length, String type, String hash, short valueCount, List<BINValue> values)
    {
        this.length = length;
        this.type = type;
        this.hash = hash;
        this.valueCount = valueCount;
        this.values = values;
    }
    
    public boolean has(String hash)
    {
        return values.stream().anyMatch(v -> v.getHash().equalsIgnoreCase(hash));
    }
    
    public Optional<BINValue> get(String hash)
    {
        return values.stream().filter(v -> v.getHash().equalsIgnoreCase(hash)).findFirst();
    }
    
    public BINValue getIfPresent(String hash)
    {
        return get(hash).get();
    }
    
    public <T> T getValue(String hash, T def)
    {
        Optional<BINValue> key = get(hash);
        return key.map(b -> (T) b.getValue()).orElse(def);
    }
    
    public String getType()
    {
        return type;
    }
    
    public int getLength()
    {
        return length;
    }
    
    public String getHash()
    {
        return hash;
    }
    
    public short getValueCount()
    {
        return valueCount;
    }
    
    public List<BINValue> getValues()
    {
        return values;
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
