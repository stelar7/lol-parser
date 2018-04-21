package no.stelar7.cdragon.types.raf.data;

public class RAFContentPath
{
    private final int offset;
    private final int length;
    
    public RAFContentPath(int offset, int length)
    {
        this.offset = offset;
        this.length = length;
    }
    
    
    public int getOffset()
    {
        return offset;
    }
    
    public int getLength()
    {
        return length;
    }
}
