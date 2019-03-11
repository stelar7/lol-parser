package no.stelar7.cdragon.types.ngrid.data.flags;

import no.stelar7.cdragon.util.handlers.UtilHandler;

import java.util.*;
import java.util.stream.Collectors;

public enum NearestLaneFlag
{
    BLUE_TOP_LANE(0b0),
    BLUE_MID_LANE(0b1),
    BLUE_BOT_LANE(0b10),
    
    RED_TOP_LANE(0b11),
    RED_MID_LANE(0b100),
    RED_BOT_LANE(0b101),
    
    BLUE_TOP_NEUTRAL_ZONE(0b110),
    BLUE_MID_NEUTRAL_ZONE(0b111),
    BLUE_BOT_NEUTRAL_ZONE(0b1000),
    
    RED_TOP_NEUTRAL_ZONE(0b1001),
    RED_MID_NEUTRAL_ZONE(0b1010),
    RED_BOT_NEUTRAL_ZONE(0b1011);
    
    
    
    int flag;
    
    NearestLaneFlag(int flag)
    {
        this.flag = flag;
    }
    
    NearestLaneFlag(NearestLaneFlag... flags)
    {
        for (NearestLaneFlag vpath : flags)
        {
            this.flag |= vpath.flag;
        }
    }
    
    public static List<NearestLaneFlag> valueOf(int flag)
    {
        return Arrays.stream(NearestLaneFlag.values()).filter(f -> UtilHandler.isBitflagSet(f.flag, flag)).collect(Collectors.toList());
    }
}
