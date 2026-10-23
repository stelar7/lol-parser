package no.stelar7.cdragon.types.releasemanifest.data;

public final class ReleasemanifestDataDirectory
{
    private final int nameIndex;
    private final int subdirectoryStartIndex;
    private final int subdirectoryCount;
    private final int fileStartIndex;
    private final int fileCount;
    
    public ReleasemanifestDataDirectory(int nameIndex, int subdirectoryStartIndex, int subdirectoryCount, int fileStartIndex, int fileCount)
    {
        this.nameIndex = nameIndex;
        this.subdirectoryStartIndex = subdirectoryStartIndex;
        this.subdirectoryCount = subdirectoryCount;
        this.fileStartIndex = fileStartIndex;
        this.fileCount = fileCount;
    }
    
    public int getNameIndex()
    {
        return nameIndex;
    }
    
    public int getSubdirectoryStartIndex()
    {
        return subdirectoryStartIndex;
    }
    
    public int getSubdirectoryCount()
    {
        return subdirectoryCount;
    }
    
    public int getFileStartIndex()
    {
        return fileStartIndex;
    }
    
    public int getFileCount()
    {
        return fileCount;
    }
}
