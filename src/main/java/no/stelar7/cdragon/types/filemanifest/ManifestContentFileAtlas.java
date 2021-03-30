package no.stelar7.cdragon.types.filemanifest;

import java.util.*;

public class ManifestContentFileAtlas
{
    private       String                    atlasFile;
    private final List<Map<String, Object>> items = new ArrayList<>();
    
    public void setAtlasFile(String name)
    {
        this.atlasFile = name;
    }
    
    public void addItem(String itemName, int x, int y, int z, int w, int h)
    {
        items.add(Map.of("name", itemName, "x", x, "y", y, "z", z, "w", w, "h", h));
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
