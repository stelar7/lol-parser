package no.stelar7.cdragon.types.bin.data;

import java.util.stream.Stream;

public enum BINValueType
{
    V3_SHORT(0),
    BOOLEAN(1),
    SIGNED_BYTE(2),
    BYTE(3),
    SIGNED_SHORT(4),
    SHORT(5),
    SIGNED_INT(6),
    INT(7),
    SIGNED_LONG(8),
    LONG(9),
    FLOAT(10),
    V2_FLOAT(11),
    V3_FLOAT(12),
    V4_FLOAT(13),
    M4X4_FLOAT(14),
    RGBA_BYTE(15),
    STRING(16),
    STRING_HASH(17),
    CONTAINER(18),
    STRUCTURE(19),
    EMBEDDED(20),
    LINK_OFFSET(21),
    OPTIONAL_DATA(22),
    PAIR(23),
    BOOLEAN_FLAGS(24),
    ;
    
    public int value;
    
    
    public static BINValueType valueOf(byte value)
    {
        // values greater than 128 have an extra bit set, so unset it and shift acordingly
        int[] check = {Byte.toUnsignedInt(value)};
        if (check[0] >= 128)
        {
            check[0] = check[0] - 110;
        }
        return Stream.of(values()).filter(t -> t.value == check[0]).findAny().orElseThrow(() -> new RuntimeException("Unknown Type: " + value));
    }
    
    BINValueType(int value)
    {
        this.value = value;
    }
}
