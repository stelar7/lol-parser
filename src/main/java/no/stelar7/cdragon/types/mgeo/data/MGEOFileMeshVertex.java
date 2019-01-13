package no.stelar7.cdragon.types.mgeo.data;

import no.stelar7.cdragon.util.types.math.*;

import java.util.Objects;

public class MGEOFileMeshVertex
{
    private Vector3f position;
    private Vector3f normal;
    private Vector2f uv1;
    private Vector2f uv2;
    
    public Vector3f getPosition()
    {
        return position;
    }
    
    public void setPosition(Vector3f position)
    {
        this.position = position;
    }
    
    public Vector3f getNormal()
    {
        return normal;
    }
    
    public void setNormal(Vector3f normal)
    {
        this.normal = normal;
    }
    
    public Vector2f getUv1()
    {
        return uv1;
    }
    
    public void setUv1(Vector2f uv1)
    {
        this.uv1 = uv1;
    }
    
    public Vector2f getUv2()
    {
        return uv2;
    }
    
    public void setUv2(Vector2f uv2)
    {
        this.uv2 = uv2;
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
        MGEOFileMeshVertex that = (MGEOFileMeshVertex) o;
        return Objects.equals(position, that.position) &&
               Objects.equals(normal, that.normal) &&
               Objects.equals(uv1, that.uv1) &&
               Objects.equals(uv2, that.uv2);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash(position, normal, uv1, uv2);
    }
    
    @Override
    public String toString()
    {
        return "MGEOFileMeshVertex{" +
               "position=" + position +
               ", normal=" + normal +
               ", uv1=" + uv1 +
               ", uv2=" + uv2 +
               '}';
    }
}
