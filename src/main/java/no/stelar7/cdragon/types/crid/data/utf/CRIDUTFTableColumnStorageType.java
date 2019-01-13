package no.stelar7.cdragon.types.crid.data.utf;

import java.util.stream.Stream;

public enum CRIDUTFTableColumnStorageType
{
    
    COLUMN_STORAGE_MASK(0xf0),
    COLUMN_STORAGE_PERROW(0x50),
    COLUMN_STORAGE_CONSTANT(0x30),
    COLUMN_STORAGE_ZERO(0x10),
    ;
    
    int value;
    
    public int getValue()
    {
        return value;
    }
    
    public static CRIDUTFTableColumnStorageType valueOf(int value)
    {
        return Stream.of(values()).filter(t -> t.value == value).findAny().orElseThrow(() -> new RuntimeException("Unknown Type: " + value));
    }
    
    CRIDUTFTableColumnStorageType(int i)
    {
        this.value = i;
    }
}
