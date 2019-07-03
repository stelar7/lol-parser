package no.stelar7.cdragon.types.ngrid.data.flags;

import no.stelar7.cdragon.util.handlers.UtilHandler;

import java.util.*;
import java.util.stream.Collectors;

public enum JungleQuadrantFlag
{
    NONE(0b0),
    NORTH(0b1),
    EAST(0b10),
    WEST(0b11),
    SOUTH(0b100),
    UNOBSERVED8(0b1000);
    
    int flag;
    
    JungleQuadrantFlag(int flag)
    {
        this.flag = flag;
    }
    
    JungleQuadrantFlag(JungleQuadrantFlag... flags)
    {
        for (JungleQuadrantFlag vpath : flags)
        {
            this.flag |= vpath.flag;
        }
    }
    
    public static List<JungleQuadrantFlag> valueOf(int flag)
    {
        return Arrays.stream(JungleQuadrantFlag.values()).filter(f -> UtilHandler.isBitflagSet(f.flag, flag)).collect(Collectors.toList());
    }
    
    public static int valueFrom(List<JungleQuadrantFlag> flags)
    {
        int value = 0;
        for (JungleQuadrantFlag flag : flags)
        {
            value += flag.flag;
        }
        return value;
    }
}
