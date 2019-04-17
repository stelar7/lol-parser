package no.stelar7.cdragon.types.filemanifest;

import java.util.*;

public class ManifestContentFileV2
{
    private Map<String, List<String>> content = new HashMap<>();
    
    public void addItem(String header, String item)
    {
        List<String> data = content.getOrDefault(header, new ArrayList<>());
        data.add(item);
        content.put(header, data);
    }
    
    public int size()
    {
        return content.size();
    }
    
    public Map<String, List<String>> getItems()
    {
        return content;
    }
}
