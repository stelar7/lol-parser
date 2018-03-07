package no.stelar7.cdragon.types.bin.data;

import java.util.stream.Stream;

public enum BINValueType
{
    BOOLEAN(1),
    BYTE(3),
    CONTAINER(18),
    INT(7),
    LINK_OFFSET(21),
    LONG(9),
    M4X4_FLOAT(14),
    OPTIONAL_DATA(22),
    PAIR(23),
    RGBA_BYTE(15),
    SHORT(5),
    SIGNED_BYTE(2),
    SIGNED_INT(6),
    SIGNED_LONG(8),
    SIGNED_SHORT(4),
    STRING(16),
    STRING_HASH(17),
    STRUCTURE(19),
    UNKNOWN_BYTE(24),
    V2_FLOAT(11),
    V3_FLOAT(12),
    V3_SHORT(0),
    V4_FLOAT(13),
    EMBEDDED(20),
    FLOAT(10),;
    
    public int value;
    
    public static BINValueType valueOf(int value)
    {
        return Stream.of(values()).filter(t -> t.value == value).findAny().orElseThrow(() -> new RuntimeException("Unknown Type: " + value));
    }
    
    BINValueType(int value)
    {
        this.value = value;
    }
}
