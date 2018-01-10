package no.stelar7.cdragon.types.bin.data;

import lombok.Data;

@Data
public class BINValue
{
    private int    hash;
    private byte   type;
    private Object value;
}
