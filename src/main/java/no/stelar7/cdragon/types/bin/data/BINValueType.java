package no.stelar7.cdragon.types.bin.data;

public enum BINValueType
{
    // type 0 is NONE/EMPTY. CDTB reads it as 3 u16 (6 bytes), so it is kept as a "V3_SHORT"
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

    private static final int            COMPLEX_FLAG = 0x80;
    private static final BINValueType[] BY_VALUE;

    static
    {
        BINValueType[] types = values();
        BY_VALUE = new BINValueType[types.length];
        for (BINValueType type : types)
        {
            BY_VALUE[type.value] = type;
        }
    }

    BINValueType(int value)
    {
        this.value = value;
    }

    /**
     * Returns the type for the raw type byte in a bin file, or null if it is not a known type
     */
    public static BINValueType fromRaw(byte value)
    {
        int check = Byte.toUnsignedInt(value);
        if ((check & COMPLEX_FLAG) == COMPLEX_FLAG)
        {
            check = check - COMPLEX_FLAG + CONTAINER.value;
        }

        return (check >= 0 && check < BY_VALUE.length) ? BY_VALUE[check] : null;
    }

    public static BINValueType valueOf(byte value)
    {
        BINValueType type = fromRaw(value);
        if (type == null)
        {
            throw new IllegalArgumentException("Unknown Type: " + Byte.toUnsignedInt(value));
        }
        return type;
    }

    /**
     * The byte used for this type in a bin file (complex types have the 0x80 flag set)
     */
    public byte toRaw()
    {
        if (value >= CONTAINER.value)
        {
            return (byte) (COMPLEX_FLAG + value - CONTAINER.value);
        }
        return (byte) value;
    }
}
