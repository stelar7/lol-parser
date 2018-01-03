package no.stelar7.cdragon.types.releasemanifest.data;

import lombok.Data;

@Data
public class ReleasemanifestHeader
{
    private String magic;
    private int    type;
    private int    entries;
    private int    version;
    private int    directoriesCount;
}
