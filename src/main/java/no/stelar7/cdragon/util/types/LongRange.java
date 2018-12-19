package no.stelar7.cdragon.util.types;

import java.util.Objects;

public class LongRange
{
    private long from;
    private long to;
    
    public LongRange(long from, long to)
    {
        this.from = from;
        this.to = to;
    }
    
    public long getFrom()
    {
        return from;
    }
    
    public void setFrom(long from)
    {
        this.from = from;
    }
    
    public long getTo()
    {
        return to;
    }
    
    public void setTo(long to)
    {
        this.to = to;
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
        LongRange longRange = (LongRange) o;
        return from == longRange.from &&
               to == longRange.to;
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(from, to);
    }
    
    @Override
    public String toString()
    {
        return "LongRange{" +
               "from=" + from +
               ", to=" + to +
               '}';
    }
}
