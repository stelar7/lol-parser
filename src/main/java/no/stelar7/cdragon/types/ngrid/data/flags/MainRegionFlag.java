package no.stelar7.cdragon.types.ngrid.data.flags;

import no.stelar7.cdragon.util.handlers.UtilHandler;

import java.util.*;
import java.util.stream.Collectors;

public enum MainRegionFlag
{
    SPAWN(0b0),
    BASE(0b1),
    
    TOP_LANE(0b10),
    MID_LANE(0b11),
    BOT_LANE(0b100),
    
    TOP_JUNGLE(0b101),
    BOT_JUNGLE(0b110),
    
    TOP_RIVER(0b111),
    BOT_RIVER(0b1000),
    
    TOP_BASE(0b1001),
    BOT_BASE(0b1010);
    
    int flag;
    
    MainRegionFlag(int flag)
    {
        this.flag = flag;
    }
    
    MainRegionFlag(MainRegionFlag... flags)
    {
        for (MainRegionFlag vpath : flags)
        {
            this.flag |= vpath.flag;
        }
    }
    
    public static List<MainRegionFlag> valueOf(int flag)
    {
        return Arrays.stream(MainRegionFlag.values()).filter(f -> UtilHandler.isBitflagSet(f.flag, flag)).collect(Collectors.toList());
    }
    
    public static int valueFrom(List<MainRegionFlag> flags)
    {
        int value = 0;
        for (MainRegionFlag flag : flags)
        {
            value += flag.flag;
        }
        return value;
    }
}
