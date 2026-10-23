package no.stelar7.cdragon.types.crid.data.utf;

/**
 * The low nibble of a @UTF column's flag byte
 */
public enum CRIDUTFTableColumnType
{
    COLUMN_TYPE_U8(0x00, 1),
    COLUMN_TYPE_S8(0x01, 1),
    COLUMN_TYPE_U16(0x02, 2),
    COLUMN_TYPE_S16(0x03, 2),
    COLUMN_TYPE_U32(0x04, 4),
    COLUMN_TYPE_S32(0x05, 4),
    COLUMN_TYPE_U64(0x06, 8),
    COLUMN_TYPE_S64(0x07, 8),
    COLUMN_TYPE_FLOAT(0x08, 4),
    COLUMN_TYPE_DOUBLE(0x09, 8),
    COLUMN_TYPE_STRING(0x0a, 4),
    COLUMN_TYPE_DATA(0x0b, 8),
    COLUMN_TYPE_GUID(0x0c, 16),
    ;

    public static final int COLUMN_TYPE_MASK = 0x0f;

    private static final CRIDUTFTableColumnType[] BY_VALUE = new CRIDUTFTableColumnType[16];

    static
    {
        for (CRIDUTFTableColumnType type : values())
        {
            BY_VALUE[type.value] = type;
        }
    }

    private final int value;
    private final int size;

    CRIDUTFTableColumnType(int value, int size)
    {
        this.value = value;
        this.size = size;
    }

    public int getValue()
    {
        return value;
    }

    /**
     * The number of bytes a value of this type takes in a row (or as a constant)
     */
    public int getSize()
    {
        return size;
    }

    /**
     * Returns the type for the low nibble of flags, or null if it is unknown
     */
    public static CRIDUTFTableColumnType fromFlags(int flags)
    {
        return BY_VALUE[flags & COLUMN_TYPE_MASK];
    }
}
