package no.stelar7.cdragon.types.filemanifest;

import java.util.*;

public final class ManifestContentFileAtlas
{
    private final String                    atlasFile;
    private final List<Map<String, Object>> items;
    
    public ManifestContentFileAtlas(String atlasFile, List<Map<String, Object>> items)
    {
        this.atlasFile = atlasFile;
        this.items = List.copyOf(items);
    }
    
    /**
     * Creates the item map used by {@link #getItems()}
     */
    public static Map<String, Object> item(String itemName, int x, int y, int z, int w, int h)
    {
        return Map.of("name", itemName, "x", x, "y", y, "z", z, "w", w, "h", h);
    }
    
    public String getAtlasFile()
    {
        return atlasFile;
    }
    
    public List<Map<String, Object>> getItems()
    {
        return items;
    }
}
