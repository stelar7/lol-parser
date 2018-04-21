package no.stelar7.cdragon.types.skn.data;

import no.stelar7.cdragon.util.types.*;

public class SKNData
{
    private Vector3f position;
    private Vector4b boneIndecies;
    private Vector4f weight;
    private Vector3f normals;
    private Vector2f uv;
    
    public Vector3f getPosition()
    {
        return position;
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
