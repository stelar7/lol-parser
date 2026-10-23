package no.stelar7.cdragon.types.releasemanifest.data;


import java.util.*;

public final class ReleasemanifestDirectory
{
    private final String                           name;
    private final List<ReleasemanifestContentFile> files;
    private final List<ReleasemanifestDirectory>   subDirectories;
    private final List<String>                     outputList     = new ArrayList<>();
    
    public ReleasemanifestDirectory(String name, List<ReleasemanifestContentFile> files, List<ReleasemanifestDirectory> subDirectories)
    {
        this.name = name;
        this.files = files;
        this.subDirectories = subDirectories;
    }
    
    public String getName()
    {
        return name;
    }
    
    public List<ReleasemanifestContentFile> getFiles()
    {
        return files;
    }
    
    public List<ReleasemanifestDirectory> getSubDirectories()
    {
        return subDirectories;
    }
    
    public List<String> generateFilelist(String pre, String post)
    {
        if (outputList.isEmpty())
        {
            generateListFromFolder(this, pre, post, outputList);
        }
        
        return outputList;
    }
    
    private void generateListFromFolder(ReleasemanifestDirectory dir, String current, String suffix, List<String> output)
    {
        String inner = (current + dir.name + "/").replace(" ", "%20");
        for (ReleasemanifestContentFile file : dir.files)
        {
            String filename = inner + file.getName().replace(" ", "%20") + suffix;
            output.add(filename);
        }
        
        for (ReleasemanifestDirectory sub : dir.subDirectories)
        {
            generateListFromFolder(sub, inner, suffix, output);
        }
    }
}
