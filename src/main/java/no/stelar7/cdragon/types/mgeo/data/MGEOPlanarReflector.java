package no.stelar7.cdragon.types.mgeo.data;

import no.stelar7.cdragon.util.types.math.*;

public class MGEOPlanarReflector
{
    private Matrix4f    transform;
    private BoundingBox plane;
    private Vector3f    normal;
    
    public Matrix4f getTransform()
    {
        return transform;
    }
    
    public void setTransform(Matrix4f transform)
    {
        this.transform = transform;
    }
    
    public BoundingBox getPlane()
    {
        return plane;
    }
    
    public void setPlane(BoundingBox plane)
    {
        this.plane = plane;
    }
    
    public Vector3f getNormal()
    {
        return normal;
    }
    
    public void setNormal(Vector3f normal)
    {
        this.normal = normal;
    }
}
