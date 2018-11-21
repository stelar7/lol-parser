package no.stelar7.cdragon.types.releasemanifest.data;


import java.util.*;

public class ReleasemanifestDirectory
{
    private String                           name;
    private List<ReleasemanifestContentFile> files          = new ArrayList<>();
    private List<ReleasemanifestDirectory>   subDirectories = new ArrayList<>();
    private List<String>                     outputList     = new ArrayList<>();
    
    public String getName()
    {
        return name;
    }
    
    public void setName(String name)
    {
        this.name = name;
    }
    
    public List<ReleasemanifestContentFile> getFiles()
    {
        return files;
    }
    
    public void setFiles(List<ReleasemanifestContentFile> files)
    {
        this.files = files;
    }
    
    public List<ReleasemanifestDirectory> getSubDirectories()
    {
        return subDirectories;
    }
    
    public void setSubDirectories(List<ReleasemanifestDirectory> subDirectories)
    {
        this.subDirectories = subDirectories;
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
