package no.stelar7.cdragon.types.releasemanifest.data;

public class ReleasemanifestDataDirectory
{
    private int nameIndex;
    private int subdirectoryStartIndex;
    private int subdirectoryCount;
    private int fileStartIndex;
    private int fileCount;
    
    public int getNameIndex()
    {
        return nameIndex;
    }
    
    public void setNameIndex(int nameIndex)
    {
        this.nameIndex = nameIndex;
    }
    
    public int getSubdirectoryStartIndex()
    {
        return subdirectoryStartIndex;
    }
    
    public void setSubdirectoryStartIndex(int subdirectoryStartIndex)
    {
        this.subdirectoryStartIndex = subdirectoryStartIndex;
    }
    
    public int getSubdirectoryCount()
    {
        return subdirectoryCount;
    }
    
    public void setSubdirectoryCount(int subdirectoryCount)
    {
        this.subdirectoryCount = subdirectoryCount;
    }
    
    public int getFileStartIndex()
    {
        return fileStartIndex;
    }
    
    public void setFileStartIndex(int fileStartIndex)
    {
        this.fileStartIndex = fileStartIndex;
    }
    
    public int getFileCount()
    {
        return fileCount;
    }
    
    public void setFileCount(int fileCount)
    {
        this.fileCount = fileCount;
    }
}
