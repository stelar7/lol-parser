package no.stelar7.cdragon.types.bnk.data;


public enum BNKHIRCSettingType
{
    VOICE_VOLUME(0),
    VOICE_LOW_PASS_FILTER(3),
    UNKNOWN(-1);
    
    int value;
    
    BNKHIRCSettingType(int type)
    {
        this.value = type;
    }
    
    public static BNKHIRCSettingType fromByte(byte b)
    {
        return fromValue(Byte.toUnsignedInt(b));
    }
    
    /**
     * @return the type with this value, or UNKNOWN
     */
    public static BNKHIRCSettingType fromValue(int v)
    {
        for (BNKHIRCSettingType type : values())
        {
            if (type.value == v && type != UNKNOWN)
            {
                return type;
            }
        }
        return UNKNOWN;
    }
    
    public int getValue()
    {
        return value;
    }
    
}
