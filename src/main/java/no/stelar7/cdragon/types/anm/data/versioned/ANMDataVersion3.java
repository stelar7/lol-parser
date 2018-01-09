package no.stelar7.cdragon.types.anm.data.versioned;

import lombok.Data;
import no.stelar7.cdragon.types.anm.data.ANMBone;

import java.util.*;

@Data
public class ANMDataVersion3
{
    private int designerId;
    private int boneCount;
    private int frameCount;
    private int fps;
    
    private List<ANMBone> bones = new ArrayList<>();
}
