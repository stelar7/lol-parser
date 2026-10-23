package no.stelar7.cdragon.types.mgeo.data;

public class MGEOBucketGeometryBucket
{
    private float maxStickOutX;
    // Z
    private float maxStickOutY;
    private int   startIndex;
    // base vertex
    private int   vertex;
    // u16
    private int   insideFaceCount;
    // u16
    private int   stickingOutFaceCount;
    
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
    
    public int getInsideFaceCount()
    {
        return insideFaceCount;
    }
    
    public void setInsideFaceCount(int insideFaceCount)
    {
        this.insideFaceCount = insideFaceCount;
    }
    
    public int getStickingOutFaceCount()
    {
        return stickingOutFaceCount;
    }
    
    public void setStickingOutFaceCount(int stickingOutFaceCount)
    {
        this.stickingOutFaceCount = stickingOutFaceCount;
    }
}
