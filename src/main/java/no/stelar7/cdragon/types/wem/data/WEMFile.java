package no.stelar7.cdragon.types.wem.data;

import lombok.Data;

@Data
public class WEMFile
{
    private int     dataOffset;
    private int     dataLength;
    private String  filename;
    private WEMData data;
}
