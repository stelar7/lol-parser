package no.stelar7.cdragon.types.skl.data.versioned.data;

import lombok.Data;

import java.util.*;

@Data
public class SKLDataV2 extends SKLDataV1
{
    private int boneIndexCounter;
    private List<Integer> boneIndecies = new ArrayList<>();
}
