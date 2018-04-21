package no.stelar7.cdragon.types.raf.data;

public class RAFContentFile
{
    private final int hash;
    private final int offset;
    private final int size;
    private final int pathIndex;
    
    public RAFContentFile(int hash, int offset, int size, int pathIndex)
    {
        this.hash = hash;
        this.offset = offset;
        this.size = size;
        this.pathIndex = pathIndex;
    }
    
    public int getHash()
    {
        return hash;
    }
    
    public int getOffset()
    {
        return offset;
    }
    
    public int getSize()
    {
        return size;
    }
    
    public int getPathIndex()
    {
        return pathIndex;
    }
}
