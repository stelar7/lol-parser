package no.stelar7.cdragon.types.releasemanifest.data;


import java.util.List;

public class ReleasemanifestDataFile
{
    private ReleasemanifestHeader                header;
    private List<ReleasemanifestDataDirectory>   directories;
    private List<ReleasemanifestDataContentFile> files;
    private List<String>                         strings;
    
    public ReleasemanifestHeader getHeader()
    {
        return header;
    }
    
    public void setHeader(ReleasemanifestHeader header)
    {
        this.header = header;
    }
    
    public List<ReleasemanifestDataDirectory> getDirectories()
    {
        return directories;
    }
    
    public void setDirectories(List<ReleasemanifestDataDirectory> directories)
    {
        this.directories = directories;
    }
    
    public List<ReleasemanifestDataContentFile> getFiles()
    {
        return files;
    }
    
    public void setFiles(List<ReleasemanifestDataContentFile> files)
    {
        this.files = files;
    }
    
    public List<String> getStrings()
    {
        return strings;
    }
    
    public void setStrings(List<String> strings)
    {
        this.strings = strings;
    }
}
