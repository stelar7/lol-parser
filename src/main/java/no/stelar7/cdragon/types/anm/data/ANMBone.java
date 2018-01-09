package no.stelar7.cdragon.types.anm.data;

import lombok.Data;

import java.util.*;

@Data
public class ANMBone
{
    private String name;
    private int    flag;
    private List<ANMBoneFrame> frames = new ArrayList<>();
}
