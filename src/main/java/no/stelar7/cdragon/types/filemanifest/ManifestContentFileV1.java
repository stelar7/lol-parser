package no.stelar7.cdragon.types.filemanifest;

import java.util.*;

public class ManifestContentFileV1
{
    private List<String> content = new ArrayList<>();
    
    public void addItem(String item)
    {
        content.add(item);
    }
    
    public int size()
    {
        return content.size();
    }
    
    public Collection<String> getItems()
    {
        return content;
    }
}
