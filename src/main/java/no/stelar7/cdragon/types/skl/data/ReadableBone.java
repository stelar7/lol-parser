package no.stelar7.cdragon.types.skl.data;

import no.stelar7.cdragon.util.types.*;

public class ReadableBone
{
    private String       name;
    private long         hash;
    private ReadableBone parent;
    private Vector3f     position;
    private Vector3f     scale;
    private Vector4f     rotation;
    
    public String getName()
    {
        return name;
    }
    
    public void setName(String name)
    {
        this.name = name;
    }
    
    public long getHash()
    {
        return hash;
    }
    
    public void setHash(long hash)
    {
        this.hash = hash;
    }
    
    public ReadableBone getParent()
    {
        return parent;
    }
    
    public void setParent(ReadableBone parent)
    {
        this.parent = parent;
    }
    
    public Vector3f getPosition()
    {
        return position;
    }
    
    public void setPosition(Vector3f position)
    {
        this.position = position;
    }
    
    public Vector3f getScale()
    {
        return scale;
    }
    
    public void setScale(Vector3f scale)
    {
        this.scale = scale;
    }
    
    public Vector4f getRotation()
    {
        return rotation;
    }
    
    public void setRotation(Vector4f rotation)
    {
        this.rotation = rotation;
    }
    
    @Override
    public String toString()
    {
        return "ReadableBone{" +
               "name='" + name + '\'' +
               ", hash='" + hash + '\'' +
               ", parent=" + parent +
               ", position=" + position +
               ", scale=" + scale +
               ", rotation=" + rotation +
               '}';
    }
}
