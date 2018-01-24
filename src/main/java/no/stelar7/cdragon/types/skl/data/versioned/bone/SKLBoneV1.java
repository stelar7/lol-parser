package no.stelar7.cdragon.types.skl.data.versioned.bone;

import lombok.Data;
import org.joml.Matrix4x3f;

@Data
public class SKLBoneV1
{
    private String     name;
    private int        parent;
    private float      scale;
    private Matrix4x3f matrix;
}
