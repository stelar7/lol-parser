package no.stelar7.cdragon.types.bbq;

import java.util.Objects;

public class BBQBundleEntry
{
    private String name;
    private long   offset;
    private long   size;
    private int    flags;
    
    public String getName()
    {
        return name;
    }
    
    public void setName(String name)
    {
        this.name = name;
    }
    
    public long getOffset()
    {
        return offset;
    }
    
    public void setOffset(long offset)
    {
        this.offset = offset;
    }
    
    public long getSize()
    {
        return size;
    }
    
    public void setSize(long size)
    {
        this.size = size;
    }
    
    public int getFlags()
    {
        return flags;
    }
    
    public void setFlags(int flags)
    {
        this.flags = flags;
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
        BBQBundleEntry that = (BBQBundleEntry) o;
        return offset == that.offset &&
               size == that.size &&
               flags == that.flags &&
               Objects.equals(name, that.name);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(name, offset, size, flags);
    }
    
    @Override
    public String toString()
    {
        return "BBQBundleEntry{" +
               "name='" + name + '\'' +
               ", offset=" + offset +
               ", size=" + size +
               ", flags=" + flags +
               '}';
    }
}
