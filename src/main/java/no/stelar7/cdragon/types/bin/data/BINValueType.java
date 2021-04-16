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
    WAD_LINK(18),
    CONTAINER(19),
    CONTAINER2(20),
    STRUCTURE(21),
    EMBEDDED(22),
    LINK_OFFSET(23),
    OPTIONAL_DATA(24),
    PAIR(25),
    BOOLEAN_FLAGS(26),
    ;
    
    public int value;
    
    private static final int COMPLEX_FLAG = 128;
    
    
    public static BINValueType valueOf(byte value)
    {
        int   firstComplexType = CONTAINER.value;
        int   check            = Byte.toUnsignedInt(value);
        int[] use              = {check};
        if ((check & COMPLEX_FLAG) == COMPLEX_FLAG)
        {
            use[0] -= COMPLEX_FLAG;
            use[0] += firstComplexType;
        }
        
        return Stream.of(values()).filter(t -> t.value == use[0]).findAny().orElseThrow(() -> new RuntimeException("Unknown Type: " + value));
    }
    
    BINValueType(int value)
    {
        this.value = value;
    }
}
