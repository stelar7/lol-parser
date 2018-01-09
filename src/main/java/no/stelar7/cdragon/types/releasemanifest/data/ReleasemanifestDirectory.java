package no.stelar7.cdragon.types.releasemanifest.data;

import lombok.Data;

import java.util.*;

@Data
public class ReleasemanifestDirectory
{
    private String name;
    private List<ReleasemanifestContentFile> files          = new ArrayList<>();
    private List<ReleasemanifestDirectory>   subDirectories = new ArrayList<>();
    
    public void printLines(String pre, String post)
    {
        printFolder(this, pre, post);
    }
    
    private void printFolder(ReleasemanifestDirectory dir, String current, String ending)
    {
        String inner = (current + dir.name + "/").replace(" ", "%20");
        for (ReleasemanifestContentFile file : dir.files)
        {
            String filename = inner + file.getName().replace(" ", "%20") + ending;
            System.out.println(filename);
        }
        
        for (ReleasemanifestDirectory sub : dir.subDirectories)
        {
            printFolder(sub, inner, ending);
        }
    }
}
