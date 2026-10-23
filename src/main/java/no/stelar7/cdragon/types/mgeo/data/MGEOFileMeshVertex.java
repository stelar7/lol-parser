package no.stelar7.cdragon.types.mgeo.data;

import no.stelar7.cdragon.util.types.math.*;
import java.util.*;

/**
 * A single vertex, combined from every vertex buffer the mesh uses.
 * Elements without a dedicated field are stored in "extra", keyed by their element name.
 */
public class MGEOFileMeshVertex
{
    private Vector3f            position;
    private Vector3f            normal;
    // TEXCOORD0
    private Vector2f            uv1;
    // TEXCOORD1 (usually the lightmap uv)
    private Vector2f            uv2;
    // BGRA
    private Vector4b            primaryColor;
    // BGRA
    private Vector4b            secondaryColor;
    private Vector4f            tangent;
    private Map<String,float[]> extra;
    
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
    
    public Vector4b getPrimaryColor()
    {
        return primaryColor;
    }
    
    public void setPrimaryColor(Vector4b primaryColor)
    {
        this.primaryColor = primaryColor;
    }
    
    public Vector4b getSecondaryColor()
    {
        return secondaryColor;
    }
    
    public void setSecondaryColor(Vector4b secondaryColor)
    {
        this.secondaryColor = secondaryColor;
    }
    
    public Vector4f getTangent()
    {
        return tangent;
    }
    
    public void setTangent(Vector4f tangent)
    {
        this.tangent = tangent;
    }
    
    public Map<String,float[]> getExtra()
    {
        return extra;
    }
    
    public void setExtra(Map<String,float[]> extra)
    {
        this.extra = extra;
    }
}
