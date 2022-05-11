package no.stelar7.cdragon.types.wad.data;

import java.util.stream.Stream;

public enum WADCompressionType
{
    NONE(0),
    GZIP(1),
    REFERENCE(2),
    ZSTD(3),
    ZSTD_MULTI(4);
    
    int code;
    
    public static WADCompressionType valueOf(int value)
    {
        return Stream.of(values()).filter(t -> t.code == value).findAny().orElseThrow(() -> new RuntimeException("Unknown compression: " + value));
    }
    
    WADCompressionType(int code)
    {
        this.code = code;
    }
}
