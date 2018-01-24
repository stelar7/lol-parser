package no.stelar7.cdragon.types.skl.data.versioned.bone;

import lombok.Data;
import org.joml.*;

@Data
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
    
}
