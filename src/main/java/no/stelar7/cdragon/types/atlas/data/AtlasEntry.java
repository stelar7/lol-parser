package no.stelar7.cdragon.types.atlas.data;

import java.util.Objects;

public class AtlasEntry
{
    private String name;
    private float  startX;
    private float  startY;
    private float  endX;
    private float  endY;
    private int    bundleIndex;
    
    public String getName()
    {
        return name;
    }
    
    public void setName(String name)
    {
        this.name = name;
    }
    
    public float getStartX()
    {
        return startX;
    }
    
    public void setStartX(float startX)
    {
        this.startX = startX;
    }
    
    public float getStartY()
    {
        return startY;
    }
    
    public void setStartY(float startY)
    {
        this.startY = startY;
    }
    
    public float getEndX()
    {
        return endX;
    }
    
    public void setEndX(float endX)
    {
        this.endX = endX;
    }
    
    public float getEndY()
    {
        return endY;
    }
    
    public void setEndY(float endY)
    {
        this.endY = endY;
    }
    
    public int getBundleIndex()
    {
        return bundleIndex;
    }
    
    public void setBundleIndex(int bundleIndex)
    {
        this.bundleIndex = bundleIndex;
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
        AtlasEntry that = (AtlasEntry) o;
        return Float.compare(that.startX, startX) == 0 && Float.compare(that.startY, startY) == 0 && Float.compare(that.endX, endX) == 0 && Float.compare(that.endY, endY) == 0 && bundleIndex == that.bundleIndex && Objects.equals(name, that.name);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(name, startX, startY, endX, endY, bundleIndex);
    }
    
    @Override
    public String toString()
    {
        return "AtlasEntry{" +
               "name='" + name + '\'' +
               ", startX=" + startX +
               ", startY=" + startY +
               ", endX=" + endX +
               ", endY=" + endY +
               ", bundleIndex=" + bundleIndex +
               '}';
    }
}
