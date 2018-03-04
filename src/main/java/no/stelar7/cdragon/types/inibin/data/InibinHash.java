package no.stelar7.cdragon.types.inibin.data;

import no.stelar7.cdragon.util.handlers.HashHandler;

import java.util.*;
import java.util.function.Function;

public final class InibinHash
{
    private static Map<String, Function<Object, Object>> transforms = new HashMap<>();
    
    private InibinHash()
    {
        // hide public constructor
    }
    
    public static String getHash(String hash)
    {
        return HashHandler.getINIHash(Integer.parseUnsignedInt(hash));
    }
    
    public static String getTransformed(String key, Object hash)
    {
        return transforms.getOrDefault(key, i -> i).apply(hash).toString();
    }
    
    private static final Function<Object, Object> percent          = (Object v) -> {
        if (v instanceof Integer)
        {
            return (Integer.valueOf((String) v) * 10);
        }
        if (v instanceof Float)
        {
            return (Float.valueOf((String) v) * 100f);
        }
        
        return 0;
    };
    private static final Function<Object, Object> attckspeed       = (Object v) -> {
        if (v instanceof Float)
        {
            return (0.625f / (1.0f + (float) v));
        }
        
        return 0;
    };
    private static final Function<Object, Object> attckspeedPerLvl = (Object v) -> {
        if (v instanceof Float)
        {
            return (float) v * 0.01f;
        }
        
        return 0;
    };
    private static final Function<Object, Object> regenPer5        = (Object v) -> {
        if (v instanceof Float)
        {
            return (float) v * 5f;
        }
        return 0;
    };
    
    
    static
    {
        // attackspeed
        transforms.put(Integer.toUnsignedString(-2103674057), attckspeed);
        transforms.put(Integer.toUnsignedString(770205030), attckspeedPerLvl);
        
        // mp and hp regen
        transforms.put(Integer.toUnsignedString(-166675978), regenPer5);
        transforms.put(Integer.toUnsignedString(-1232864324), regenPer5);
        transforms.put(Integer.toUnsignedString(619143803), regenPer5);
        transforms.put(Integer.toUnsignedString(1248483905), regenPer5);
        
        
        // @CharAbilityPower@ and @CharBonusPhysical@
        transforms.put(Integer.toUnsignedString(-797683884), percent);
        transforms.put(Integer.toUnsignedString(-797683884), percent);
    }
}
