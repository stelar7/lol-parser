package no.stelar7.cdragon.types.wpk.data;

import lombok.Data;

@Data
public class WPKHeader
{
    private String magic;
    private int    version;
    private int    fileCount;
}
