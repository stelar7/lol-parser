package no.stelar7.cdragon.types.anm.data;

import lombok.Data;
import no.stelar7.cdragon.util.reader.types.*;

@Data
public class ANMBoneFrame
{
    private Quaternion<Float> rotation;
    private Vector3<Float>    translation;
}
