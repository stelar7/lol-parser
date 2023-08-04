package no.stelar7.cdragon.types.atlas.data;

import java.util.*;

public class AtlasFile
{
    private Map<String, List<AtlasEntry>> data;
    
    public Map<String, List<AtlasEntry>> getData()
    {
        return data;
    }
    
    public void setData(Map<String, List<AtlasEntry>> data)
    {
        this.data = data;
    }
    
    @Override
    public String toString()
    {
        return "AtlasFile{" +
               "data=" + data +
               '}';
    }
    
    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        AtlasFile atlasFile = (AtlasFile) o;
        return Objects.equals(data, atlasFile.data);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(data);
    }
}
