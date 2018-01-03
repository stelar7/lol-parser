package no.stelar7.cdragon.types.releasemanifest.data;

import lombok.Data;

@Data
public class ReleasemanifestContentFile
{
    private String name;
    private int    size;
    private String hash;
}
