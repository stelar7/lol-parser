package no.stelar7.cdragon.types.ngrid.data.flags;

import no.stelar7.cdragon.util.handlers.UtilHandler;

import java.util.*;
import java.util.stream.Collectors;

public enum RingFlag
{
    BLUE_SPAWN_TO_NEXUS(0b0),
    BLUE_NEXUS_TO_INHIB(0b1),
    BLUE_INHIB_TO_INNER(0b10),
    BLUE_INNER_TO_OUTER(0b11),
    BLUE_OUTER_TO_NEUTRAL(0b100),
    
    RED_SPAWN_TO_NEXUS(0b101),
    RED_NEXUS_TO_INHIB(0b110),
    RED_INHIB_TO_INNER(0b111),
    RED_INNER_TO_OUTER(0b1000),
    RED_OUTER_TO_NEUTRAL(0b1001);
    
    int flag;
    
    RingFlag(int flag)
    {
        this.flag = flag;
    }
    
    RingFlag(RingFlag... flags)
    {
        for (RingFlag vpath : flags)
        {
            this.flag |= vpath.flag;
        }
    }
    
    public static List<RingFlag> valueOf(int flag)
    {
        return Arrays.stream(RingFlag.values()).filter(f -> UtilHandler.isBitflagSet(f.flag, flag)).collect(Collectors.toList());
    }
    
    public static int valueFrom(List<RingFlag> flags)
    {
        int value = 0;
        for (RingFlag flag : flags)
        {
            value += flag.flag;
        }
        return value;
    }
}
