package no.stelar7.cdragon.types.anm.data.versioned;

import lombok.Data;
import no.stelar7.cdragon.types.anm.data.ANMFrame;
import no.stelar7.cdragon.util.reader.types.*;

import java.util.*;

@Data
public class ANMDataVersion5
{
    private int unknown1;
    private int unknown2;
    private int unknown3;
    
    private int boneCount;
    private int frameCount;
    private int FPS;
    private int hashOffset;
    
    private int unknown4;
    private int unknown5;
    
    private int positionOffset;
    private int rotationOffset;
    private int frameOffset;
    
    private int unknown6;
    private int unknown7;
    private int unknown8;
    
    private List<Vector3<Float>>         positions = new ArrayList<>();
    private List<Vector3<Short>>         rotations = new ArrayList<>();
    private List<Integer>                hashes    = new ArrayList<>();
    private Map<Integer, List<ANMFrame>> frames    = new HashMap<>();
}
