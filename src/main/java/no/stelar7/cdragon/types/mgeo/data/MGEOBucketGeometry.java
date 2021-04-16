package no.stelar7.cdragon.types.mgeo.data;

import no.stelar7.cdragon.util.types.math.Vector3f;

import java.util.*;

public class MGEOBucketGeometry
{
    private float                        minx;
    private float                        maxx;
    private float                        miny;
    private float                        maxy;
    private float                        minz;
    private float                        maxz;
    private float                        centerx;
    private float                        centerz;
    private List<Vector3f>               vertices;
    private List<Short>                  indices;
    private MGEOBucketGeometryBucket[][] buckets;
    
    public float getMinx()
    {
        return minx;
    }
    
    public void setMinx(float minx)
    {
        this.minx = minx;
    }
    
    public float getMaxx()
    {
        return maxx;
    }
    
    public void setMaxx(float maxx)
    {
        this.maxx = maxx;
    }
    
    public float getMiny()
    {
        return miny;
    }
    
    public void setMiny(float miny)
    {
        this.miny = miny;
    }
    
    public float getMaxy()
    {
        return maxy;
    }
    
    public void setMaxy(float maxy)
    {
        this.maxy = maxy;
    }
    
    public float getMinz()
    {
        return minz;
    }
    
    public void setMinz(float minz)
    {
        this.minz = minz;
    }
    
    public float getMaxz()
    {
        return maxz;
    }
    
    public void setMaxz(float maxz)
    {
        this.maxz = maxz;
    }
    
    public float getCenterx()
    {
        return centerx;
    }
    
    public void setCenterx(float centerx)
    {
        this.centerx = centerx;
    }
    
    public float getCenterz()
    {
        return centerz;
    }
    
    public void setCenterz(float centerz)
    {
        this.centerz = centerz;
    }
    
    public List<Vector3f> getVertices()
    {
        return vertices;
    }
    
    public void setVertices(List<Vector3f> vertices)
    {
        this.vertices = vertices;
    }
    
    public List<Short> getIndices()
    {
        return indices;
    }
    
    public void setIndices(List<Short> indices)
    {
        this.indices = indices;
    }
    
    public MGEOBucketGeometryBucket[][] getBuckets()
    {
        return buckets;
    }
    
    public void setBuckets(MGEOBucketGeometryBucket[][] buckets)
    {
        this.buckets = buckets;
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
        MGEOBucketGeometry that = (MGEOBucketGeometry) o;
        return Float.compare(that.minx, minx) == 0 &&
               Float.compare(that.maxx, maxx) == 0 &&
               Float.compare(that.miny, miny) == 0 &&
               Float.compare(that.maxy, maxy) == 0 &&
               Float.compare(that.minz, minz) == 0 &&
               Float.compare(that.maxz, maxz) == 0 &&
               Float.compare(that.centerx, centerx) == 0 &&
               Float.compare(that.centerz, centerz) == 0 &&
               Objects.equals(vertices, that.vertices) &&
               Objects.equals(indices, that.indices) &&
               Arrays.deepEquals(buckets, that.buckets);
    }
    
    @Override
    public int hashCode()
    {
        int result = Objects.hash(minx, maxx, miny, maxy, minz, maxz, centerx, centerz, vertices, indices);
        result = 31 * result + Arrays.deepHashCode(buckets);
        return result;
    }
    
    @Override
    public String toString()
    {
        return "MGEOBucketGeometry{" +
               "minx=" + minx +
               ", maxx=" + maxx +
               ", miny=" + miny +
               ", maxy=" + maxy +
               ", minz=" + minz +
               ", maxz=" + maxz +
               ", centerx=" + centerx +
               ", centerz=" + centerz +
               ", vertices=" + vertices +
               ", indices=" + indices +
               ", buckets=" + Arrays.toString(buckets) +
               '}';
    }
}
