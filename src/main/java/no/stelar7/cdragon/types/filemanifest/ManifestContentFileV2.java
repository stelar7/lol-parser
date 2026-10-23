package no.stelar7.cdragon.types.filemanifest;

import java.util.*;

public final class ManifestContentFileV2
{
    private final Map<String, List<String>> content;
    
    /**
     * @param content the items grouped by their header (an unmodifiable view of the map is kept, so it should not be modified afterwards)
     */
    public ManifestContentFileV2(Map<String, List<String>> content)
    {
        this.content = Collections.unmodifiableMap(content);
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
