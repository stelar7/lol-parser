package no.stelar7.cdragon.types.skn.data;

import no.stelar7.cdragon.util.types.math.*;

public final class SKNData
{
    private final Vector3f position;
    private final Vector4b boneIndecies;
    private final Vector4f weight;
    private final Vector3f normals;
    private final Vector2f uv;
    private final Vector4f tangent;
    private final Vector4b color;
    
    public SKNData(Vector3f position, Vector4b boneIndecies, Vector4f weight, Vector3f normals, Vector2f uv, Vector4f tangent, Vector4b color)
    {
        this.position = position;
        this.boneIndecies = boneIndecies;
        this.weight = weight;
        this.normals = normals;
        this.uv = uv;
        this.tangent = tangent;
        this.color = color;
    }
    
    public Vector3f getPosition()
    {
        return position;
    }
    
    public Vector4b getColor()
    {
        return color;
    }
    
    public Vector4f getTangent()
    {
        return tangent;
    }
    
    public Vector4b getBoneIndecies()
    {
        return boneIndecies;
    }
    
    public Vector4f getWeight()
    {
        return weight;
    }
    
    public Vector3f getNormals()
    {
        return normals;
    }
    
    public Vector2f getUv()
    {
        return uv;
    }
}
