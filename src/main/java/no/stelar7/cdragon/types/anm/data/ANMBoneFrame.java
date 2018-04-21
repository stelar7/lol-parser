package no.stelar7.cdragon.types.anm.data;

import org.joml.*;

public class ANMBoneFrame
{
    private Quaternionf rotation;
    private Vector3f    translation;
    
    public Quaternionf getRotation()
    {
        return rotation;
    }
    
    public void setRotation(Quaternionf rotation)
    {
        this.rotation = rotation;
    }
    
    public Vector3f getTranslation()
    {
        return translation;
    }
    
    public void setTranslation(Vector3f translation)
    {
        this.translation = translation;
    }
}
