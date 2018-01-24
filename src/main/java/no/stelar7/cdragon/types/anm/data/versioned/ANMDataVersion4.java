package no.stelar7.cdragon.types.anm.data.versioned;

import lombok.Data;
import no.stelar7.cdragon.types.anm.data.ANMFrame;
import org.joml.*;

import java.util.*;

@Data
public class ANMDataVersion4
{
    private int dataSize;
    private int designerId;
    
    private int unknown1;
    private int unknown2;
    
    private int boneCount;
    private int frameCount;
    private int FPS;
    
    private int unknown3;
    private int unknown4;
    private int unknown5;
    
    private int positionOffset;
    private int rotationOffset;
    private int frameOffset;
    
    private int unknown6;
    private int unknown7;
    private int unknown8;
    
    private List<Vector3f>               positions = new ArrayList<>();
    private List<Quaternionf>            rotations = new ArrayList<>();
    private Map<Integer, List<ANMFrame>> frames    = new HashMap<>();
}
