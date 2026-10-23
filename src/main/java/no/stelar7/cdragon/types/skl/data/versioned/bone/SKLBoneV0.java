package no.stelar7.cdragon.types.skl.data.versioned.bone;

import no.stelar7.cdragon.util.types.math.*;

public final class SKLBoneV0
{
    private final short    unknown1;
    private final short    id;
    private final short    parent;
    private final short    unknown2;
    private final int      hash;
    private final float    TPO;
    private final Vector3f position;
    private final Vector3f scale;
    private final Vector4f rotation;
    // inverse bind translation
    private final Vector3f ct;
    private final Vector3f inverseBindScale;
    private final Vector4f inverseBindRotation;
    // relative to the position of the name offset field
    private final int    nameOffset;
    private final String name;
    
    public SKLBoneV0(short unknown1, short id, short parent, short unknown2, int hash, float TPO, Vector3f position, Vector3f scale, Vector4f rotation, Vector3f ct, Vector3f inverseBindScale, Vector4f inverseBindRotation, int nameOffset, String name)
    {
        this.unknown1 = unknown1;
        this.id = id;
        this.parent = parent;
        this.unknown2 = unknown2;
        this.hash = hash;
        this.TPO = TPO;
        this.position = position;
        this.scale = scale;
        this.rotation = rotation;
        this.ct = ct;
        this.inverseBindScale = inverseBindScale;
        this.inverseBindRotation = inverseBindRotation;
        this.nameOffset = nameOffset;
        this.name = name;
    }
    
    public short getUnknown1()
    {
        return unknown1;
    }
    
    public short getId()
    {
        return id;
    }
    
    public short getParent()
    {
        return parent;
    }
    
    public short getUnknown2()
    {
        return unknown2;
    }
    
    public int getHash()
    {
        return hash;
    }
    
    public float getTPO()
    {
        return TPO;
    }
    
    public Vector3f getPosition()
    {
        return position;
    }
    
    public Vector3f getScale()
    {
        return scale;
    }
    
    public Vector4f getRotation()
    {
        return rotation;
    }
    
    public Vector3f getCt()
    {
        return ct;
    }
    
    public Vector3f getInverseBindScale()
    {
        return inverseBindScale;
    }
    
    public Vector4f getInverseBindRotation()
    {
        return inverseBindRotation;
    }
    
    public int getNameOffset()
    {
        return nameOffset;
    }
    
    public String getName()
    {
        return name;
    }
}
