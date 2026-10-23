package no.stelar7.cdragon.types.raf.data;

public final class RAFHeader
{
    private final int magic;
    private final int version;
    private final int managerIndex;
    private final int filesOffset;
    private final int pathsOffset;
    
    public RAFHeader(int magic, int version, int managerIndex, int filesOffset, int pathsOffset)
    {
        this.magic = magic;
        this.version = version;
        this.managerIndex = managerIndex;
        this.filesOffset = filesOffset;
        this.pathsOffset = pathsOffset;
    }
    
    public int getMagic()
    {
        return magic;
    }
    
    public int getVersion()
    {
        return version;
    }
    
    public int getManagerIndex()
    {
        return managerIndex;
    }
    
    public int getFilesOffset()
    {
        return filesOffset;
    }
    
    public int getPathsOffset()
    {
        return pathsOffset;
    }
}
