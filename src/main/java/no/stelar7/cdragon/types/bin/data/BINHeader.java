package no.stelar7.cdragon.types.bin.data;

import lombok.Data;

@Data
public class BINHeader
{
    private String magic;
    private int    version;
    private int    entryCount;
}
