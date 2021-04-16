package no.stelar7.cdragon.types.filemanifest;

import java.util.*;

public class ManifestContentFileV0
{
    private final List<Integer> content = new ArrayList<>();
    
    public void addItem(Integer item)
    {
        content.add(item);
    }
    
    public int size()
    {
        return content.size();
    }
    
    public Collection<Integer> getItems()
    {
        return content;
    }
}
