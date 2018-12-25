package no.stelar7.cdragon.util.types;

import java.util.Objects;

public class BoundingBox
{
    private Vector3f min;
    private Vector3f max;
    
    public Vector3f getMin()
    {
        return min;
    }
    
    public void setMin(Vector3f min)
    {
        this.min = min;
    }
    
    public Vector3f getMax()
    {
        return max;
    }
    
    public void setMax(Vector3f max)
    {
        this.max = max;
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
        BoundingBox that = (BoundingBox) o;
        return Objects.equals(min, that.min) &&
               Objects.equals(max, that.max);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(min, max);
    }
    
    @Override
    public String toString()
    {
        return "BoundingBox{" +
               "min=" + min +
               ", max=" + max +
               '}';
    }
}
