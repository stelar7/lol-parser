package no.stelar7.cdragon.types.anm.data;

import lombok.Data;

@Data
public class ANMHeader
{
    private String magic;
    private int    version;
}
