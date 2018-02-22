package no.stelar7.cdragon.types.skl.data.versioned.data;

import lombok.*;

import java.util.*;

@Data
@EqualsAndHashCode(callSuper = true)
public class SKLDataV2 extends SKLDataV1
{
    private int boneIndexCounter;
    private List<Integer> boneIndecies = new ArrayList<>();
}
