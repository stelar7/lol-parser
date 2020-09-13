package no.stelar7.cdragon.types.bbq;

import java.util.Arrays;

public enum BBQNodeFlag
{
    DEFAULT(0),
    DIRECTORY(1),
    DELETED(2),
    SERIALIZED_FILE(3);
    
    private int flag;
    
    BBQNodeFlag(int flag)
    {
        this.flag = flag;
    }
    
    public static BBQNodeFlag from(int flag)
    {
        return Arrays.stream(values()).filter(v -> v.flag == flag).findFirst().get();
    }
    
}
