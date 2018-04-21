package no.stelar7.cdragon.types.raf.data;

public class RAFHeader
{
    private int magic;
    private int version;
    private int managerIndex;
    private int filesOffset;
    private int pathsOffset;
    
    public int getMagic()
    {
        return magic;
    }
    
    public void setMagic(int magic)
    {
        this.magic = magic;
    }
    
    public int getVersion()
    {
        return version;
    }
    
    public void setVersion(int version)
    {
        this.version = version;
    }
    
    public int getManagerIndex()
    {
        return managerIndex;
    }
    
    public void setManagerIndex(int managerIndex)
    {
        this.managerIndex = managerIndex;
    }
    
    public int getFilesOffset()
    {
        return filesOffset;
    }
    
    public void setFilesOffset(int filesOffset)
    {
        this.filesOffset = filesOffset;
    }
    
    public int getPathsOffset()
    {
        return pathsOffset;
    }
    
    public void setPathsOffset(int pathsOffset)
    {
        this.pathsOffset = pathsOffset;
    }
}
