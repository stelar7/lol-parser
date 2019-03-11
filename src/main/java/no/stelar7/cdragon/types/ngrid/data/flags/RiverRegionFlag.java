package no.stelar7.cdragon.types.ngrid.data.flags;

import no.stelar7.cdragon.util.handlers.UtilHandler;

import java.util.*;
import java.util.stream.Collectors;

public enum RiverRegionFlag
{
    NON_JUNGLE(0b0),
    JUNGLE(0b1),
    BARON(0b10),
    UNOBSERVED4(0b100),
    UNOBSERVED8(0b1000),
    RIVER(0b10000),
    UNKNOWN32(0b100000), // only ever found on the original Nexus Blitz map, where it was instead used to represent the river (other flags were shuffled too)
    RIVER_ENTRANCE(0b1000000);
    
    int flag;
    
    RiverRegionFlag(int flag)
    {
        this.flag = flag;
    }
    
    RiverRegionFlag(RiverRegionFlag... flags)
    {
        for (RiverRegionFlag vpath : flags)
        {
            this.flag |= vpath.flag;
        }
    }
    
    public static List<RiverRegionFlag> valueOf(int flag)
    {
        return Arrays.stream(RiverRegionFlag.values()).filter(f -> UtilHandler.isBitflagSet(f.flag, flag)).collect(Collectors.toList());
    }
}
