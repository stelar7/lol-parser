package no.stelar7.cdragon.types.mgeo.data;

import java.util.Objects;

public class MGEOBucketGeometryBucket
{
    private float maxStickOutX;
    private float maxStickOutY;
    private int   startIndex;
    private int   vertex;
    private short insideFaceCount;
    private short stickingOutFaceCount;
    
    public float getMaxStickOutX()
    {
        return maxStickOutX;
    }
    
    public void setMaxStickOutX(float maxStickOutX)
    {
        this.maxStickOutX = maxStickOutX;
    }
    
    public float getMaxStickOutY()
    {
        return maxStickOutY;
    }
    
    public void setMaxStickOutY(float maxStickOutY)
    {
        this.maxStickOutY = maxStickOutY;
    }
    
    public int getStartIndex()
    {
        return startIndex;
    }
    
    public void setStartIndex(int startIndex)
    {
        this.startIndex = startIndex;
    }
    
    public int getVertex()
    {
        return vertex;
    }
    
    public void setVertex(int vertex)
    {
        this.vertex = vertex;
    }
    
    public short getInsideFaceCount()
    {
        return insideFaceCount;
    }
    
    public void setInsideFaceCount(short insideFaceCount)
    {
        this.insideFaceCount = insideFaceCount;
    }
    
    public short getStickingOutFaceCount()
    {
        return stickingOutFaceCount;
    }
    
    public void setStickingOutFaceCount(short stickingOutFaceCount)
    {
        this.stickingOutFaceCount = stickingOutFaceCount;
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
        MGEOBucketGeometryBucket that = (MGEOBucketGeometryBucket) o;
        return Float.compare(that.maxStickOutX, maxStickOutX) == 0 &&
               Float.compare(that.maxStickOutY, maxStickOutY) == 0 &&
               startIndex == that.startIndex &&
               vertex == that.vertex &&
               insideFaceCount == that.insideFaceCount &&
               stickingOutFaceCount == that.stickingOutFaceCount;
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(maxStickOutX, maxStickOutY, startIndex, vertex, insideFaceCount, stickingOutFaceCount);
    }
    
    @Override
    public String toString()
    {
        return "MGEOBucketGeometryBucket{" +
               "maxStickOutX=" + maxStickOutX +
               ", maxStickOutY=" + maxStickOutY +
               ", startIndex=" + startIndex +
               ", vertex=" + vertex +
               ", insideFaceCount=" + insideFaceCount +
               ", stickingOutFaceCount=" + stickingOutFaceCount +
               '}';
    }
}
