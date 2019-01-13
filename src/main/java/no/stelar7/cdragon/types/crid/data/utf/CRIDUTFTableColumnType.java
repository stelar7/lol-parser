package no.stelar7.cdragon.types.crid.data.utf;

import java.util.stream.Stream;

public enum CRIDUTFTableColumnType
{
    COLUMN_TYPE_MASK(0x0f),
    COLUMN_TYPE_DATA(0x0b),
    COLUMN_TYPE_STRING(0x0a),
    COLUMN_TYPE_FLOAT(0x08),
    /* 0x07 signed 8byte? */
    COLUMN_TYPE_8BYTE(0x06),
    COLUMN_TYPE_4BYTE2(0x05),
    COLUMN_TYPE_4BYTE(0x04),
    COLUMN_TYPE_2BYTE2(0x03),
    COLUMN_TYPE_2BYTE(0x02),
    COLUMN_TYPE_1BYTE2(0x01),
    COLUMN_TYPE_1BYTE(0x00),
    ;
    
    int value;
    
    public int getValue()
    {
        return value;
    }
    
    public static CRIDUTFTableColumnType valueOf(int value)
    {
        return Stream.of(values()).filter(t -> t.value == value).findAny().orElseThrow(() -> new RuntimeException("Unknown Type: " + value));
    }
    
    CRIDUTFTableColumnType(int i)
    {
        this.value = i;
    }
}
