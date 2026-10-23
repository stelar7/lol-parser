package no.stelar7.cdragon.types.skl.data.versioned.bone;


import no.stelar7.cdragon.util.types.math.Matrix4x3f;

public final class SKLBoneV1
{
    private final String     name;
    private final int        parent;
    private final float      scale;
    private final Matrix4x3f matrix;
    
    public SKLBoneV1(String name, int parent, float scale, Matrix4x3f matrix)
    {
        this.name = name;
        this.parent = parent;
        this.scale = scale;
        this.matrix = matrix;
    }
    
    public String getName()
    {
        return name;
    }
    
    public int getParent()
    {
        return parent;
    }
    
    public float getScale()
    {
        return scale;
    }
    
    public Matrix4x3f getMatrix()
    {
        return matrix;
    }
}
