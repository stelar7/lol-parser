package no.stelar7.cdragon.types.filemanifest;

import java.util.*;

public final class ManifestContentFileV1
{
    private final List<String> content;
    
    public ManifestContentFileV1(List<String> content)
    {
        this.content = List.copyOf(content);
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
