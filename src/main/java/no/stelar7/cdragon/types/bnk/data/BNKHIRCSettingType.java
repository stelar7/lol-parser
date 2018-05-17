package no.stelar7.cdragon.types.bnk.data;

import java.util.stream.Stream;

public enum BNKHIRCSettingType
{
    VOICE_VOLUME(0),
    VOICE_LOW_PASS_FILTER(3),;
    
    int value;
    
    BNKHIRCSettingType(int type)
    {
        this.value = type;
    }
    
    public static BNKHIRCSettingType fromByte(byte b)
    {
        return Stream.of(values()).filter(i -> i.value == b).findFirst().get();
    }
    
}
