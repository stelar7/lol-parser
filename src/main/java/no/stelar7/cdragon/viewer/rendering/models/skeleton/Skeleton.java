package no.stelar7.cdragon.viewer.rendering.models.skeleton;

import no.stelar7.cdragon.types.skl.data.*;

import java.util.List;

public class Skeleton
{
    public List<ReadableBone> bones;
    
    public Skeleton(SKLFile skl)
    {
        bones = skl.toReadableBones();
    }
    
}
