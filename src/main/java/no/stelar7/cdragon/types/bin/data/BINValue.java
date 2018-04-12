package no.stelar7.cdragon.types.bin.data;

import lombok.Data;

@Data
public class BINValue
{
    private String       hash;
    private BINValueType type;
    private Object       value;
}
