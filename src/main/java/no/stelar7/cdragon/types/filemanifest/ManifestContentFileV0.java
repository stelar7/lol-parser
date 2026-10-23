package no.stelar7.cdragon.types.filemanifest;

import java.util.*;

public final class ManifestContentFileV0
{
    private final List<Integer> content;
    
    public ManifestContentFileV0(List<Integer> content)
    {
        this.content = List.copyOf(content);
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
