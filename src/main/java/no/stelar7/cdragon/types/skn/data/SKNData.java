package no.stelar7.cdragon.types.skn.data;

import no.stelar7.cdragon.util.types.math.*;

public class SKNData
{
    private Vector3f position;
    private Vector4b boneIndecies;
    private Vector4f weight;
    private Vector3f normals;
    private Vector2f uv;
    private Vector4f tangent;
    private Vector4b color;
    
    public Vector3f getPosition()
    {
        return position;
    }
    
    public void setTangent(Vector4f tangent)
    {
        this.tangent = tangent;
    }
    
    public Vector4b getColor()
    {
        return color;
    }
    
    public void setColor(Vector4b color)
    {
        this.color = color;
    }
    
    public Vector4f getTangent()
    {
        return tangent;
    }
    
    public void setPosition(Vector3f position)
    {
        this.position = position;
    }
    
    public Vector4b getBoneIndecies()
    {
        return boneIndecies;
    }
    
    public void setBoneIndecies(Vector4b boneIndecies)
    {
        this.boneIndecies = boneIndecies;
    }
    
    public Vector4f getWeight()
    {
        return weight;
    }
    
    public void setWeight(Vector4f weight)
    {
        this.weight = weight;
    }
    
    public Vector3f getNormals()
    {
        return normals;
    }
    
    public void setNormals(Vector3f normals)
    {
        this.normals = normals;
    }
    
    public Vector2f getUv()
    {
        return uv;
    }
    
    public void setUv(Vector2f uv)
    {
        this.uv = uv;
    }
}
