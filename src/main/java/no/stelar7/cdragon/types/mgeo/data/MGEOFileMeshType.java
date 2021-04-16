package no.stelar7.cdragon.types.mgeo.data;

import java.util.Arrays;

public enum MGEOFileMeshType
{
    MODEL(0),
    DOUBLE_BUFFER_SIMPLE_GEO(2),
    GROUND(4),
    SIMPLE_GEO_2(5);
    
    private final int value;
    
    MGEOFileMeshType(int value)
    {
        this.value = value;
    }
    
    public int getValue()
    {
        return value;
    }
    
    public static MGEOFileMeshType getFromValue(int value)
    {
        return Arrays.stream(MGEOFileMeshType.values()).filter(v -> value == value).findAny().orElse(null);
    }
}
