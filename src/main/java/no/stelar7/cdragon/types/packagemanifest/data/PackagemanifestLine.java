package no.stelar7.cdragon.types.packagemanifest.data;

public class PackagemanifestLine
{
    private String filePath;
    private String containedInFile;
    private int    containedOffset;
    private int    fileSize;
    private int    unknown;
    
    public String getFilePath()
    {
        return filePath;
    }
    
    public void setFilePath(String filePath)
    {
        this.filePath = filePath;
    }
    
    public String getContainedInFile()
    {
        return containedInFile;
    }
    
    public void setContainedInFile(String containedInFile)
    {
        this.containedInFile = containedInFile;
    }
    
    public int getContainedOffset()
    {
        return containedOffset;
    }
    
    public void setContainedOffset(int containedOffset)
    {
        this.containedOffset = containedOffset;
    }
    
    public int getFileSize()
    {
        return fileSize;
    }
    
    public void setFileSize(int fileSize)
    {
        this.fileSize = fileSize;
    }
    
    public int getUnknown()
    {
        return unknown;
    }
    
    public void setUnknown(int unknown)
    {
        this.unknown = unknown;
    }
    
    @Override
    public String toString()
    {
        return filePath;
    }
}
