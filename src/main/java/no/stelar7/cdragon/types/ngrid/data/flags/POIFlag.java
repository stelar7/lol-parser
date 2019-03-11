package no.stelar7.cdragon.types.ngrid.data.flags;

import no.stelar7.cdragon.util.handlers.UtilHandler;

import java.util.*;
import java.util.stream.Collectors;

public enum POIFlag
{
    NONE(0b0),
    TURRET(0b1),
    BASE_GATE(0b10),
    
    BARON(0b11),
    DRAGON(0b100),
    RED_BUFF(0b101),
    
    BLUE_BUFF(0b110),
    GROMP(0b111),
    KRUGS(0b1000),
    
    RAPTORS(0b1001),
    MURKWOLFS(0b1010);
    
    int flag;
    
    POIFlag(int flag)
    {
        this.flag = flag;
    }
    
    POIFlag(POIFlag... flags)
    {
        for (POIFlag vpath : flags)
        {
            this.flag |= vpath.flag;
        }
    }
    
    public static List<POIFlag> valueOf(int flag)
    {
        return Arrays.stream(POIFlag.values()).filter(f -> UtilHandler.isBitflagSet(f.flag, flag)).collect(Collectors.toList());
    }
}
