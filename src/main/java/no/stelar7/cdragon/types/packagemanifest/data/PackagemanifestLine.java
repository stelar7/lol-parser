package no.stelar7.cdragon.types.packagemanifest.data;

public final class PackagemanifestLine
{
    private final String filePath;
    private final String containedInFile;
    private final int    containedOffset;
    private final int    fileSize;
    private final int    unknown;
    
    public PackagemanifestLine(String filePath, String containedInFile, int containedOffset, int fileSize, int unknown)
    {
        this.filePath = filePath;
        this.containedInFile = containedInFile;
        this.containedOffset = containedOffset;
        this.fileSize = fileSize;
        this.unknown = unknown;
    }
    
    public String getFilePath()
    {
        return filePath;
    }
    
    public String getContainedInFile()
    {
        return containedInFile;
    }
    
    public int getContainedOffset()
    {
        return containedOffset;
    }
    
    public int getFileSize()
    {
        return fileSize;
    }
    
    public int getUnknown()
    {
        return unknown;
    }
    
    @Override
    public String toString()
    {
        return filePath;
    }
}
