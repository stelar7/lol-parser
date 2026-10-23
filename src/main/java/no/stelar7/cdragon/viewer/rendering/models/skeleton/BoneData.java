package no.stelar7.cdragon.viewer.rendering.models.skeleton;

import org.joml.*;

/**
 * The local transform of one joint at one point in time
 */
public class BoneData
{
    int         hash;
    String      name;
    Vector3f    position;
    Quaternionf rotation;
    Vector3f    scale;

    public BoneData(int hash, String name, Vector3f position, Quaternionf rotation, Vector3f scale)
    {
        this.hash = hash;
        this.name = name;
        this.position = position;
        this.rotation = rotation;
        this.scale = scale;
    }

    /**
     * The joint hash (ELF hash of the lowercase joint name), as stored in the skeleton and the animation
     */
    public int getHash()
    {
        return hash;
    }

    /**
     * The joint name, or null if the joint is not in the skeleton the animation was built with
     */
    public String getName()
    {
        return name;
    }

    public Vector3f getPosition()
    {
        return position;
    }

    public Quaternionf getRotation()
    {
        return rotation;
    }

    public Vector3f getScale()
    {
        return scale;
    }

    /**
     * translation * rotation * scale
     */
    public Matrix4f toMatrix()
    {
        return new Matrix4f().translationRotateScale(position, rotation, scale);
    }

    @Override
    public String toString()
    {
        return "BoneData{" + (name != null ? name : Integer.toHexString(hash)) + ", position=" + position + ", rotation=" + rotation + ", scale=" + scale + "}";
    }
}
