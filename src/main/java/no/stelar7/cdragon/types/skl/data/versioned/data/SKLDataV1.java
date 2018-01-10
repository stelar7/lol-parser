package no.stelar7.cdragon.types.skl.data.versioned.data;

import lombok.Data;
import no.stelar7.cdragon.types.skl.data.versioned.bone.SKLBoneV1;

import java.util.*;

@Data
public class SKLDataV1
{
    private int designerId;
    private int boneCounter;
    private List<SKLBoneV1> bones = new ArrayList<>();
}
