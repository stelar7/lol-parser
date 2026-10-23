package no.stelar7.cdragon.types.crid.data.utf;

/**
 * The high nibble of a @UTF column's flag byte.
 * It is a bit set: 0x10 = has a name, 0x20 = has a constant (default) value in the schema, 0x40 = has a value in every row.
 * So 0x10 is always zero, 0x30 is constant, 0x50 is per-row, and 0x70 is per-row with a default.
 */
public enum CRIDUTFTableColumnStorageType
{
    COLUMN_STORAGE_ZERO(0x10),
    COLUMN_STORAGE_CONSTANT(0x30),
    COLUMN_STORAGE_PERROW(0x50),
    COLUMN_STORAGE_PERROW_DEFAULT(0x70),
    ;

    public static final int COLUMN_STORAGE_MASK = 0xf0;
    public static final int FLAG_NAME           = 0x10;
    public static final int FLAG_DEFAULT        = 0x20;
    public static final int FLAG_ROW            = 0x40;

    private final int value;

    CRIDUTFTableColumnStorageType(int i)
    {
        this.value = i;
    }

    public int getValue()
    {
        return value;
    }

    public static boolean hasDefault(int flags)
    {
        return (flags & FLAG_DEFAULT) != 0;
    }

    public static boolean isPerRow(int flags)
    {
        return (flags & FLAG_ROW) != 0;
    }
}
