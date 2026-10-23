package no.stelar7.cdragon.types.mgeo.data;

import no.stelar7.cdragon.util.types.math.Vector3f;
import java.util.*;

/**
 * Bucketed (scene graph) geometry used for ground height / picking.
 * Field names are kept from older versions of this class: centerx/centerz are the max stick out in x/z,
 * miny/maxy are the bucket size in x/z.
 */
public class MGEOBucketGeometry
{
    // version 18+, read before the visibility controller hash (was "unknownFloat" in older versions of this class)
    private int                          regionPathHash;
    // version 15+
    private int                          visibilityControllerPathHash;
    private float                        minx;
    private float                        minz;
    private float                        maxx;
    private float                        maxz;
    // max stick out x
    private float                        centerx;
    // max stick out z
    private float                        centerz;
    // bucket size x
    private float                        miny;
    // bucket size z
    private float                        maxy;
    private int                          bucketsPerSide;
    private boolean                      disabled;
    // bit 0 = has face visibility flags
    private int                          flags;
    private List<Vector3f>               vertices = new ArrayList<>();
    private int[]                        indices = new int[0];
    private MGEOBucketGeometryBucket[][] buckets = new MGEOBucketGeometryBucket[0][0];
    private byte[]                       faceVisibilityFlags;
    
    public int getVisibilityControllerPathHash()
    {
        return visibilityControllerPathHash;
    }
    
    public void setVisibilityControllerPathHash(int visibilityControllerPathHash)
    {
        this.visibilityControllerPathHash = visibilityControllerPathHash;
    }
    
    /**
     * Container-key (path) hash of the region placeable in the map's MapPlaceableContainer this graph is anchored to, 0 if none (version 18+)
     */
    public int getRegionPathHash()
    {
        return regionPathHash;
    }
    
    public void setRegionPathHash(int regionPathHash)
    {
        this.regionPathHash = regionPathHash;
    }
    
    public float getMinx()
    {
        return minx;
    }
    
    public void setMinx(float minx)
    {
        this.minx = minx;
    }
    
    public float getMinz()
    {
        return minz;
    }
    
    public void setMinz(float minz)
    {
        this.minz = minz;
    }
    
    public float getMaxx()
    {
        return maxx;
    }
    
    public void setMaxx(float maxx)
    {
        this.maxx = maxx;
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
    
    public int getBucketsPerSide()
    {
        return bucketsPerSide;
    }
    
    public void setBucketsPerSide(int bucketsPerSide)
    {
        this.bucketsPerSide = bucketsPerSide;
    }
    
    public boolean isDisabled()
    {
        return disabled;
    }
    
    public void setDisabled(boolean disabled)
    {
        this.disabled = disabled;
    }
    
    public int getFlags()
    {
        return flags;
    }
    
    public void setFlags(int flags)
    {
        this.flags = flags;
    }
    
    public List<Vector3f> getVertices()
    {
        return vertices;
    }
    
    public void setVertices(List<Vector3f> vertices)
    {
        this.vertices = vertices;
    }
    
    public int[] getIndices()
    {
        return indices;
    }
    
    public void setIndices(int[] indices)
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
    
    public byte[] getFaceVisibilityFlags()
    {
        return faceVisibilityFlags;
    }
    
    public void setFaceVisibilityFlags(byte[] faceVisibilityFlags)
    {
        this.faceVisibilityFlags = faceVisibilityFlags;
    }
}
