package no.stelar7.cdragon.types.releasemanifest.data;


import java.util.List;

public final class ReleasemanifestDataFile
{
    private final ReleasemanifestHeader                header;
    private final List<ReleasemanifestDataDirectory>   directories;
    private final List<ReleasemanifestDataContentFile> files;
    private final List<String>                         strings;
    
    public ReleasemanifestDataFile(ReleasemanifestHeader header, List<ReleasemanifestDataDirectory> directories, List<ReleasemanifestDataContentFile> files, List<String> strings)
    {
        this.header = header;
        this.directories = directories;
        this.files = files;
        this.strings = strings;
    }
    
    public ReleasemanifestHeader getHeader()
    {
        return header;
    }
    
    public List<ReleasemanifestDataDirectory> getDirectories()
    {
        return directories;
    }
    
    public List<ReleasemanifestDataContentFile> getFiles()
    {
        return files;
    }
    
    public List<String> getStrings()
    {
        return strings;
    }
}
