package no.stelar7.cdragon.types.bbq;

import java.util.Arrays;

public enum BBQCompressionType
{
    NONE(0),
    LZMA(1),
    LZ4(2),
    LZ4HC(3),
    LZHAM(4);
    
    private final int flag;
    
    BBQCompressionType(int flag)
    {
        this.flag = flag;
    }
    
    public static BBQCompressionType from(int flag)
    {
        return Arrays.stream(values()).filter(v -> v.flag == flag).findFirst().get();
    }
}
