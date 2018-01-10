package no.stelar7.cdragon.types.skl.data.versioned.bone;

import lombok.Data;
import no.stelar7.cdragon.util.reader.types.*;

@Data
public class SKLBoneV0
{
    private short          unknown1;
    private short          id;
    private short          parent;
    private short          unknown2;
    private int            hash;
    private float          TPO;
    private Vector3<Float> position;
    private Vector3<Float> scale;
    private Vector4<Float> rotation;
    private Vector3<Float> ct;
    private String         padding;
    
}
