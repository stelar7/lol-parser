package no.stelar7.cdragon.types.ngrid.data.flags;

import no.stelar7.cdragon.util.handlers.UtilHandler;

import java.util.*;
import java.util.stream.Collectors;

public enum VisionPathFlag
{
    WALKABLE(0b0),
    BRUSH(0b1),
    WALL(0b10),
    STRUCTURE(0b100),
    UNOBSERVED8(0b1000),
    UNOBSERVED16(0b10000),
    UNOBSERVED32(0b100000),
    TRANSPARENT_WALL(0b1000000),
    UNOBSERVED128(0b10000000),  // marks the difference between two otherwise-equivalent cells, spread sporadically throughout the map, ignored for a cleaner image since it doesn't seem useful at all
    ALWAYS_VISIBLE(0b100000000),
    UNKNOWN512(0b1000000000), // only ever found on the original Nexus Blitz map, and it was only present in two sections of what would otherwise be normal wall
    BLUE_TEAM_ONLY(0b10000000000),
    RED_TEAM_ONLY(0b100000000000),
    NEUTRAL_ZONE_VISIBILITY(0b1000000000000);
    
    int flag;
    
    VisionPathFlag(int flag)
    {
        this.flag = flag;
    }
    
    VisionPathFlag(VisionPathFlag... flags)
    {
        for (VisionPathFlag vpath : flags)
        {
            this.flag |= vpath.flag;
        }
    }
    
    public static List<VisionPathFlag> valueOf(int flag)
    {
        return Arrays.stream(VisionPathFlag.values()).filter(f -> UtilHandler.isBitflagSet(f.flag, flag)).collect(Collectors.toList());
    }
    
    public static int valueFrom(List<VisionPathFlag> flags)
    {
        int value = 0;
        for (VisionPathFlag flag : flags)
        {
            value += flag.flag;
        }
        return value;
    }
}
