package no.stelar7.cdragon.types.releasemanifest.data;

import lombok.Data;

import java.util.*;

@Data
public class ReleasemanifestDirectory
{
    private String name;
    private List<ReleasemanifestContentFile> files          = new ArrayList<>();
    private List<ReleasemanifestDirectory>   subDirectories = new ArrayList<>();
    
    public List<String> printLines(String pre, String post)
    {
        System.out.format("prefix: %s%nsuffix: %s", pre, post);
        
        List<String> output = new ArrayList<>();
        printFolder(this, "", output);
        return output;
    }
    
    private void printFolder(ReleasemanifestDirectory dir, String current, List<String> output)
    {
        String inner = (current + dir.name + "/").replace(" ", "%20");
        for (ReleasemanifestContentFile file : dir.files)
        {
            String filename = inner + file.getName().replace(" ", "%20");
            output.add(filename);
        }
        
        for (ReleasemanifestDirectory sub : dir.subDirectories)
        {
            printFolder(sub, inner, output);
        }
    }
}
