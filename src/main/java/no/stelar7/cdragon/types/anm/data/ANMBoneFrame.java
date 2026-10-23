package no.stelar7.cdragon.types.anm.data;

import org.joml.*;

public final class ANMBoneFrame
{
    private final Quaternionf rotation;
    private final Vector3f    translation;
    
    public ANMBoneFrame(Quaternionf rotation, Vector3f translation)
    {
        this.rotation = rotation;
        this.translation = translation;
    }
    
    public Quaternionf getRotation()
    {
        return rotation;
    }
    
    public Vector3f getTranslation()
    {
        return translation;
    }
}
