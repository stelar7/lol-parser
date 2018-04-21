package no.stelar7.cdragon.types.skl.data.versioned.data;

import no.stelar7.cdragon.types.skl.data.versioned.bone.*;

import java.util.List;

public class SKLDataV0
{
    private short                unknown1;
    private short                boneCount;
    private short                boneIndexCount;
    private int                  boneStartOffset;
    private int                  animationStartOffset;
    private int                  boneIndexStartOffset;
    private int                  boneIndexEndOffset;
    private int                  boneStringOffset;
    private int                  boneNameIndex;
    private String               padding;
    private List<SKLBoneV0>      bones;
    private List<SKLBoneV0Extra> boneExtra;
    private List<Short>          boneIndecies;
    private List<String>         boneNames;
    
}