package no.stelar7.cdragon.types.anm.data.versioned;

import lombok.Data;
import no.stelar7.cdragon.types.anm.data.ANMEntry;
import no.stelar7.cdragon.util.reader.types.Vector3f;

import java.util.*;

@Data
public class ANMDataVersion1
{
    private int    dataSize;
    private String subMagic;
    private int    subVersion;
    private int    boneCount;
    private int    entryCount;
    private int    unknown1;
    
    private float animationLength;
    private float FPS;
    
    private int unknown2;
    private int unknown3;
    private int unknown4;
    private int unknown5;
    private int unknown6;
    private int unknown7;
    
    private Vector3f minTranslation;
    private Vector3f maxTranslation;
    private Vector3f minScale;
    private Vector3f maxScale;
    
    private int entryOffset;
    private int indexOffset;
    private int hashOffset;
    
    private List<ANMEntry> entries    = new ArrayList<>();
    private List<Short>    indecies   = new ArrayList<>();
    private List<Integer>  boneHashes = new ArrayList<>();
    
    
}
