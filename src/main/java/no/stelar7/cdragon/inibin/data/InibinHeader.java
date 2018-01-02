package no.stelar7.cdragon.inibin.data;

import lombok.Data;

@Data
public class InibinHeader
{
    private int version;
    private int tableLength;
    private int bitmask;
}
