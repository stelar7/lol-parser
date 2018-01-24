package no.stelar7.cdragon.types.anm.data;

import lombok.Data;
import org.joml.*;

@Data
public class ANMBoneFrame
{
    private Quaternionf rotation;
    private Vector3f    translation;
}
