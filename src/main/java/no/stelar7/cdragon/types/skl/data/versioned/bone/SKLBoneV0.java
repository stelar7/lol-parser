package no.stelar7.cdragon.types.skl.data.versioned.bone;

import no.stelar7.cdragon.util.types.math.*;

public class SKLBoneV0
{
    private short    unknown1;
    private short    id;
    private short    parent;
    private short    unknown2;
    private int      hash;
    private float    TPO;
    private Vector3f position;
    private Vector3f scale;
    private Vector4f rotation;
    private Vector3f ct;
    private String   padding;
    
    public short getUnknown1()
    {
        return unknown1;
    }
    
    public void setUnknown1(short unknown1)
    {
        this.unknown1 = unknown1;
    }
    
    public short getId()
    {
        return id;
    }
    
    public void setId(short id)
    {
        this.id = id;
    }
    
    public short getParent()
    {
        return parent;
    }
    
    public void setParent(short parent)
    {
        this.parent = parent;
    }
    
    public short getUnknown2()
    {
        return unknown2;
    }
    
    public void setUnknown2(short unknown2)
    {
        this.unknown2 = unknown2;
    }
    
    public int getHash()
    {
        return hash;
    }
    
    public void setHash(int hash)
    {
        this.hash = hash;
    }
    
    public float getTPO()
    {
        return TPO;
    }
    
    public void setTPO(float TPO)
    {
        this.TPO = TPO;
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
    
    public Vector3f getCt()
    {
        return ct;
    }
    
    public void setCt(Vector3f ct)
    {
        this.ct = ct;
    }
    
    public String getPadding()
    {
        return padding;
    }
    
    public void setPadding(String padding)
    {
        this.padding = padding;
    }
}
