package no.stelar7.cdragon.types.skl.data.versioned.bone;

import lombok.Data;
import no.stelar7.cdragon.util.reader.types.Matrix4x3;

@Data
public class SKLBoneV1
{
    private String           name;
    private int              parent;
    private float            scale;
    private Matrix4x3<Float> matrix;
}
