package no.stelar7.cdragon.types.skl.data.versioned.bone;

import org.joml.Matrix4x3f;

public class SKLBoneV1
{
    private String     name;
    private int        parent;
    private float      scale;
    private Matrix4x3f matrix;
    
    public String getName()
    {
        return name;
    }
    
    public void setName(String name)
    {
        this.name = name;
    }
    
    public int getParent()
    {
        return parent;
    }
    
    public void setParent(int parent)
    {
        this.parent = parent;
    }
    
    public float getScale()
    {
        return scale;
    }
    
    public void setScale(float scale)
    {
        this.scale = scale;
    }
    
    public Matrix4x3f getMatrix()
    {
        return matrix;
    }
    
    public void setMatrix(Matrix4x3f matrix)
    {
        this.matrix = matrix;
    }
}
